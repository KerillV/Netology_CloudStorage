package netology.cloudstorage.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import netology.cloudstorage.entity.FileCloudStorage;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.FileRepositoryCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/**
 * Сервис для работы с файлами в облачном хранилище.
 * Предоставляет методы для загрузки, скачивания, переименования и удаления файлов, а также для вычисления контрольных сумм и извлечения расширений файлов.
 * Управляет сохранением метаданных файлов в базе данных и взаимодействием с физическим хранилищем файлов.
 */
@Service
@RequiredArgsConstructor
public class FileServiceCloud {
    private final TokenServiceAuth tokenServiceAuth;
    private final FileRepositoryCloud fileRepositoryCloud;
    private final Environment env;
    private String UPLOAD_DIRECTORY;
    @PostConstruct
    public void initialize() {
        UPLOAD_DIRECTORY = env.getProperty("uploads.directory");
    }

    private static final Logger logger = LoggerFactory.getLogger(FileServiceCloud.class);

    // Список разрешенных расширений файлов
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpeg", "pdf", "docx", "txt");

    /**
     * Загружает файл с метаданными в облачное хранилище.
     * Проверяет наличие файла с таким именем, производит физическую загрузку файла и сохраняет его метаданные в базе данных.
     *
     * @param file загружаемый файл.
     * @param userId идентификатор владельца файла.
     * @throws IOException если возникла проблема с физической загрузкой файла.
     */
    public void uploadWithMetadata(MultipartFile file, Long userId) throws IOException {
        // Проверка существования файла с таким именем
        String filename = file.getOriginalFilename();
        if (Files.exists(Paths.get(UPLOAD_DIRECTORY + filename))) {
            logger.warn("Файл с именем {} уже существует.", filename);
            throw new IllegalStateException("Файл с именем " + filename + " уже существует.");
        }

        // Загрузка файла и сохранение метаданных
        uploadFileOnly(file);
        saveFileMetadata(file, userId);
    }

    /**
     * Физически сохраняет файл в хранилище без обращения к базе данных.
     *
     * @param file загружаемый файл.
     * @throws IOException если возникла проблема с сохранением файла.
     */
    private void uploadFileOnly(MultipartFile file) throws IOException {
        // Проверяем наличие директории
        File dir = new File(UPLOAD_DIRECTORY);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalStateException("Каталог для загрузки файлов ('" + UPLOAD_DIRECTORY + "') не найден или не является директорией.");
        }

        // Формируем путь к файлу
        String path = UPLOAD_DIRECTORY + file.getOriginalFilename();

        // Сохраняем файл
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(file.getBytes());
        }
    }

    /**
     * Сохраняет метаданные файла в базу данных.
     * Создает объект {@link FileCloudStorage}, заполняет его необходимыми данными и сохраняет в репозиторий.
     *
     * @param file загружаемый файл.
     * @param userId идентификатор владельца файла.
     */
    private void saveFileMetadata(MultipartFile file, Long userId) {
        FileCloudStorage fileMetaData = new FileCloudStorage();
        fileMetaData.setFilename(file.getOriginalFilename());
        fileMetaData.setSize(file.getSize());
        fileMetaData.setChecksum(getChecksum(file)); // Рассчитываем контрольную сумму файла

        // Определяем пользователя, которому принадлежит файл, используя токен
        UserCloudStorage currentUser = getCurrentUser(userId);
        fileMetaData.setOwner(currentUser);

        // Сохраняем метаданные файла в базу данных
        fileRepositoryCloud.save(fileMetaData);
    }

    // Логика получения пользователя по идентификатору Id
    private UserCloudStorage getCurrentUser(Long userId) {
        return tokenServiceAuth.getUserById(userId);
    }

    /**
     * Вычисляет контрольную сумму файла методом CRC32.
     *
     * @param file загружаемый файл.
     * @return строковое представление контрольной суммы.
     */
    private String getChecksum(MultipartFile file) {
        try (InputStream fis = file.getInputStream()) {
            CRC32 crc32 = new CRC32();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) >= 0) {
                crc32.update(buffer, 0, len);
            }
            return Long.toHexString(crc32.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Скачивает файл из хранилища.
     *
     * @param filename имя файла.
     * @return массив байтов с содержимым файла.
     * @throws IOException если возникла проблема с чтением файла.
     */
    public byte[] download(String filename) throws IOException {
        if (UPLOAD_DIRECTORY == null) {
            throw new IllegalStateException("Директория загрузки не установлена.");
        }
        Path path = Paths.get(UPLOAD_DIRECTORY, filename);
        return Files.readAllBytes(path);
    }

    /**
     * Переименовывает файл в хранилище.
     * Меняет имя файла физически и обновляет его метаданные в базе данных.
     *
     * @param oldFilename предыдущее имя файла.
     * @param newFilename новое имя файла.
     * @param userId идентификатор пользователя, выполняющего операцию.
     * @throws IOException если возникла проблема с переименованием файла.
     */
    public void rename(String oldFilename, String newFilename, Long userId) throws IOException {
        // Проверяем существование старого файла
        Optional<FileCloudStorage> fileOpt = fileRepositoryCloud.findByFilename(oldFilename);
        if (fileOpt.isPresent()) {
            FileCloudStorage fileToUpdate = fileOpt.get();

            // Проверяем принадлежность файла данному пользователю
            if (!fileToUpdate.getOwner().getId().equals(userId)) {
                throw new AccessDeniedException("You don't have permission to rename this file.");
            }

            if (UPLOAD_DIRECTORY == null) {
                throw new IllegalStateException("Директория загрузки не установлена.");
            }

            // Проверяем, что имена файлов не пусты
            if (oldFilename == null || oldFilename.isBlank() || newFilename == null || newFilename.isBlank()) {

                throw new IllegalArgumentException("Имя файла не может быть пустым.");
            }

            // Формируем старые и новые пути
            Path source = Paths.get(UPLOAD_DIRECTORY, oldFilename);
            Path target = Paths.get(UPLOAD_DIRECTORY, newFilename);

            // Проверяем, что файлы существуют
            if (!Files.exists(source)) {
                throw new NoSuchElementException("Файл не найден: " + oldFilename);
            }

            // Переименовываем файл в файловой системе
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            // Обновляем имя файла в базе данных
            fileToUpdate.setFilename(newFilename);
            fileRepositoryCloud.save(fileToUpdate);
        } else {
            throw new NoSuchElementException("Файл не найден в базе данных: " + oldFilename);
        }
    }

    /**
     * Получает список файлов для пользователя.
     * Возвращает ограниченное количество файлов для отображения пользователю.
     *
     * @param userId идентификатор пользователя.
     * @param limit лимит результатов.
     * @return список файлов.
     */
    public List<FileInfo> getFilesForUser(Long userId, int limit) {
        // Получаем список файлов пользователя
        List<FileCloudStorage> userFiles = fileRepositoryCloud.findByOwnerId(userId);

        // Применяем ограничение количества файлов
        if (limit > 0 && userFiles.size() > limit) {
            userFiles = userFiles.subList(0, limit);
        }

        // Преобразуем файлы в формат FileInfo
        return userFiles.stream()
                .map(file -> new FileInfo(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }

    /**
     * Удаляет файл из хранилища.
     * Удаляет физический файл и соответствующую запись в базе данных.
     *
     * @param filename имя файла.
     * @param userId идентификатор пользователя, выполняющего удаление.
     */
public void deleteFile(String filename, Long userId) {
    logger.info("Deleting file: {}", filename);

    // Получаем файл из базы данных
    Optional<FileCloudStorage> fileOpt = fileRepositoryCloud.findByFilename(filename);
    if (fileOpt.isPresent()) {
        FileCloudStorage fileToRemove = fileOpt.get(); // Получаем объект файла

        // Проверяем принадлежность файла данному пользователю
        if (!fileToRemove.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to delete this file.");
        }

        String fullPath = UPLOAD_DIRECTORY + filename;
        File physicalFile = new File(fullPath);
        if (physicalFile.exists()) {
            if (!physicalFile.delete()) {
                logger.error("Не удалось удалить файл: {}", filename);
                throw new RuntimeException("Не удалось удалить файл: " + filename);
            }
        } else {
            logger.error("Файл не найден: {}", filename);
            throw new NoSuchElementException("Файл не найден: " + filename);
        }
        // Удаляем запись из базы данных
        fileRepositoryCloud.delete(fileToRemove);
    } else {
        logger.error("Файл не найден в базе данных: {}", filename);
        throw new NoSuchElementException("Файл не найден в базе данных: " + filename);
    }
}

    // Внутренняя структура FileInfo для удобства представления файла
    public record FileInfo(String filename, long size) {
    }

    // Извлекает расширение файла из имени файла
    public static String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index > 0 && index < filename.length() - 1) {
            return filename.substring(index + 1); // Отсекаем точку и получаем чистое расширение
        }
        return "";
    }

}
