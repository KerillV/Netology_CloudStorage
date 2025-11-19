package netology.cloudstorage.service;

import jakarta.annotation.PostConstruct;
import netology.cloudstorage.entity.FileCloudStorage;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.FileRepositoryCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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

@Service
public class FileServiceCloud {
    @Autowired
    private TokenServiceAuth tokenServiceAuth;
    @Autowired
    private FileRepositoryCloud fileRepositoryCloud;
    @Autowired
    private Environment env;
    private String UPLOAD_DIRECTORY;
    @PostConstruct
    public void initialize() {
        UPLOAD_DIRECTORY = env.getProperty("uploads.directory");
    }

    // Список разрешенных расширений файлов
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpeg", "pdf", "docx", "txt");

    public void uploadWithMetadata(MultipartFile file, String token) throws IOException {
        // Проверка существования файла с таким именем
        String filename = file.getOriginalFilename();
        if (Files.exists(Paths.get(UPLOAD_DIRECTORY + filename))) {
            logger.warn("Файл с именем {} уже существует.", filename);
            throw new IllegalStateException("Файл с именем " + filename + " уже существует.");
        }

        // Загрузка файла и сохранение метаданных
        uploadFileOnly(file);
        saveFileMetadata(file, token);
    }

    // Метод только для физического сохранения файла (без обращения к базе данных)
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

    // Метод сохранения метаданных файла в базу данных
    private void saveFileMetadata(MultipartFile file, String token) {
        FileCloudStorage fileMetaData = new FileCloudStorage();
        fileMetaData.setFilename(file.getOriginalFilename());
        fileMetaData.setSize(file.getSize());
        fileMetaData.setChecksum(getChecksum(file)); // Рассчитываем контрольную сумму файла

        // Определяем пользователя, которому принадлежит файл, используя токен
        UserCloudStorage currentUser = getCurrentUser(token);
        fileMetaData.setOwner(currentUser);

        // Сохраняем метаданные файла в базу данных
        fileRepositoryCloud.save(fileMetaData);
    }

    // Логика получения пользователя по токену
    private UserCloudStorage getCurrentUser(String token) {
        return tokenServiceAuth.getUserByToken(token);
    }

    // Метод расчета контрольной суммы файла
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

    // Скачивание файла
    public byte[] download(String filename) throws IOException {
        if (UPLOAD_DIRECTORY == null) {
            throw new IllegalStateException("Директория загрузки не установлена.");
        }
        Path path = Paths.get(UPLOAD_DIRECTORY, filename);
        return Files.readAllBytes(path);
    }

    // Переименование файла
    public void rename(String oldFilename, String newFilename) throws IOException {
        // Получаем файл из базы данных
        Optional<FileCloudStorage> fileOpt = fileRepositoryCloud.findByFilename(oldFilename);
        if (fileOpt.isPresent()) {
            FileCloudStorage fileToUpdate = fileOpt.get();

            if (UPLOAD_DIRECTORY == null) {
                throw new IllegalStateException("Директория загрузки не установлена.");
            }
            // Перемещаем файл в файловой системе
            Path source = Paths.get(UPLOAD_DIRECTORY, oldFilename);
            Path target = Paths.get(UPLOAD_DIRECTORY, newFilename);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            // Обновляем имя файла в базе данных
            fileToUpdate.setFilename(newFilename);
            fileRepositoryCloud.save(fileToUpdate);
        } else {
            throw new NoSuchElementException("Файл не найден в базе данных: " + oldFilename);
        }
    }

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

    // Удаление файла
    private static final Logger logger = LoggerFactory.getLogger(FileServiceCloud.class);

    public void deleteFile(String filename) {
        logger.info("Deleting file: {}", filename);
        // Ищем файл по имени
        Optional<FileCloudStorage> fileOpt = fileRepositoryCloud.findByFilename(filename);
        if (fileOpt.isPresent()) {
            FileCloudStorage fileToRemove = fileOpt.get(); // Получаем объект файла
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
