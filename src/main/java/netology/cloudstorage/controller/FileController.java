package netology.cloudstorage.controller;

import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.service.FileServiceCloud;
import netology.cloudstorage.service.TokenServiceAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cloud")
public class FileController {
    @Autowired
    private FileServiceCloud fileServiceCloud;
    @Autowired
    private TokenServiceAuth tokenServiceAuth;
    private final long maxFileSize = 10485760; // максимальный размер загружаемого файла

    // Константа для UTF-8
    private static final MediaType MEDIA_TYPE_UTF_8 = MediaType.valueOf("text/plain; charset=utf-8");

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("auth-token") String token) {

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

        // Загрузка файла
        try {
            fileServiceCloud.uploadWithMetadata(file, token);
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

    // Скачивание файла
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("filename") String filename,
            @RequestHeader("auth-token") String token) throws IOException {

        byte[] fileContent = fileServiceCloud.download(filename); // Читаем файл
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileContent));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream") // Добавляем заголовок content-type
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    // Редактирование имени файла
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @PutMapping("/file")
    public ResponseEntity<?> editFileName(
            @RequestParam("filename") String oldFilename,
            @RequestBody Map<String, Object> body,
            @RequestHeader("auth-token") String token) {

        String newFilename = (String) body.get("name"); // Новое имя файла
        try {
            fileServiceCloud.rename(oldFilename, newFilename); // Переименовываем файл и обновляем запись в базе данных
            return new ResponseEntity<>("Success", HttpStatus.OK);
        } catch (IOException ex) {
            return new ResponseEntity<>("Failed to rename the file: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Удаление файла
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestParam("filename") String filename,
            @RequestHeader("auth-token") String token) {

        // Декодируем имя файла (если используется экранирование)
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        fileServiceCloud.deleteFile(decodedFilename); // удаляем файл и запись в базе данных
        return new ResponseEntity<>("Success delete", HttpStatus.OK);
    }

    // Получение списка файлов
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @GetMapping("/list")
    public ResponseEntity<List<FileServiceCloud.FileInfo>> getFiles(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestHeader("auth-token") String token) {

        // Получаем пользователя по токену
        UserCloudStorage user = tokenServiceAuth.getUserByToken(token);
        if (user == null) {
            throw new AccessDeniedException("Unknown user by given token.");
        }

        // Возвращаем список файлов для текущего пользователя
        return ResponseEntity.ok(fileServiceCloud.getFilesForUser(user.getId(), limit));
    }
}
