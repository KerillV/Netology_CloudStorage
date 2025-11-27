package netology.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.service.FileServiceCloud;
import netology.cloudstorage.service.UserServiceAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Контроллер управления файлами в облачном хранилище.
 * Предоставляет RESTful API для загрузки, скачивания, редактирования имен файлов, просмотра списков файлов и удаления файлов.
 * Все операции защищены правилами безопасности и доступны только пользователям с ролями ROLE_ADMIN или ROLE_USER.
 *
  */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class FileController {
    private final FileServiceCloud fileServiceCloud;
    private final UserServiceAuth userServiceAuth;
    private final long maxFileSize = 10485760; // максимальный размер загружаемого файла
    // Константа для UTF-8
    private static final MediaType MEDIA_TYPE_UTF_8 = MediaType.valueOf("text/plain; charset=utf-8");

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    /**
     * Загружает файл в облачное хранилище.
     * Перед загрузкой производится проверка размера файла и его расширения.
     *
     * @param file файл, передаваемый через multipart/form-data.
     * @return успешный статус OK (HTTP 200) при удачной загрузке или соответствующий статус ошибки в противном случае.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file) {

        // Получаем текущего пользователя из контекста безопасности
        UserCloudStorage user = userServiceAuth.currentUser();
        logger.info("пользователь в uploadFile: {}", user);

        // Проверка пустоты файла
        if (file.isEmpty()) {
            logger.warn("Uploaded file is empty!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MEDIA_TYPE_UTF_8)
                    .body("Файл не выбран или пустой.");
        }

        // Проверка расширения файла
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            logger.warn("Имя файла не задано.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MEDIA_TYPE_UTF_8)
                    .body("Имя файла не задано.");
        }

        String extension = FileServiceCloud.extractExtension(originalFilename);
        if (!FileServiceCloud.ALLOWED_EXTENSIONS.contains(extension)) {
            logger.warn("Unsupported file type: {}", extension);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MEDIA_TYPE_UTF_8)
                    .body("Файл имеет недопустимое расширение.");
        }

        // Проверка размера файла
        if (file.getSize() > maxFileSize) {
            logger.warn("File too large: {} bytes", file.getSize());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .contentType(MEDIA_TYPE_UTF_8)
                    .body("Превышен допустимый размер файла (" + maxFileSize + " байт).");
        }

        // Загрузка
        try {
            fileServiceCloud.uploadWithMetadata(file, user.getId());
            return new ResponseEntity<>("Success upload", HttpStatus.OK);
        } catch (IllegalStateException ise) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            return new ResponseEntity<>(ise.getMessage(), headers, HttpStatus.CONFLICT);
        } catch (Exception e) {
            logger.error("Ошибка при загрузке файла:", e);
            return new ResponseEntity<>("Ошибка при загрузке файла.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Скачивает файл из облачного хранилища.
     * Возвращает файл с правильным Content-Type и указанием на скачивание.
     *
     * @param filename название файла для скачивания.
     * @return ресурс с файлом и соответствующими HTTP-заголовками.
     * @throws IOException если возникли проблемы с чтением файла.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("filename") String filename) throws IOException {

        byte[] fileContent = fileServiceCloud.download(filename); // Читаем файл
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileContent));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream") // Добавляем заголовок content-type
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Изменяет имя файла в хранилище.
     * Пользователь с ролью ROLE_USER может изменить имя файла, предварительно выбрав старый и новый имена.
     *
     * @param oldFilename существующее имя файла.
     * @param body JSON-объект с полем "filename", содержащим новое имя файла.
     * @return успешный статус OK (HTTP 200) при изменении имени или ошибка BAD_REQUEST (HTTP 400), если новое имя отсутствует.
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @PutMapping("/file")
    public ResponseEntity<?> editFileName(
            @RequestParam("filename") String oldFilename,
            @RequestBody(required = false) Map<String, Object> body) {

        // Получаем текущего пользователя из контекста безопасности
        UserCloudStorage user = userServiceAuth.currentUser();

        // Логируем старое имя файла
        logger.info("Old Filename: {}", oldFilename);

        // Логируем тело запроса
        logger.info("Request Body: {}", body);

        // Извлекаем новое имя файла из тела запроса
        String newFilename = (String) body.get("filename");

        if (newFilename.isBlank()) {
            return new ResponseEntity<>("Имя файла не может быть пустым.", HttpStatus.BAD_REQUEST);
        }

        // Логируем новое имя файла
        logger.info("New Filename: {}", newFilename);

        try {
            fileServiceCloud.rename(oldFilename, newFilename, user.getId()); // Переименовываем файл и обновляем запись в базе данных
            return new ResponseEntity<>("Success", HttpStatus.OK);
        } catch (IOException ex) {
            return new ResponseEntity<>("Failed to rename the file: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Возвращает список файлов, принадлежащих текущему пользователю.
     * Ограничивает вывод списком первых N файлов, где N задаётся параметром limit.
     * Только пользователи с ролью ROLE_ADMIN или ROLE_USER могут получать списки файлов.
     *
     * @param limit максимальное количество возвращаемых файлов (по умолчанию 10).
     * @return список объектов FileInfo с информацией о файлах пользователя.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @GetMapping("/list")
    public ResponseEntity<List<FileServiceCloud.FileInfo>> getFiles(
            @RequestParam(defaultValue = "10") Integer limit) {

        // Получаем текущего пользователя из контекста безопасности
        UserCloudStorage user = userServiceAuth.currentUser();

        // Возвращаем список файлов для текущего пользователя
        return ResponseEntity.ok(fileServiceCloud.getFilesForUser(user.getId(), limit));
    }

    /**
     * Удаляет файл из облачного хранилища.
     * Доступно только пользователям с ролью ROLE_USER.
     *
     * @param filename имя удаляемого файла.
     * @return успешный статус OK (HTTP 200) при успешном удалении файла.
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestParam("filename") String filename) {

        // Получаем текущего пользователя из контекста безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserCloudStorage user = (UserCloudStorage) authentication.getPrincipal();

        // Удаляем файл и запись в базе данных
        fileServiceCloud.deleteFile(filename, user.getId());

        return new ResponseEntity<>("Success delete", HttpStatus.OK);
    }

}
