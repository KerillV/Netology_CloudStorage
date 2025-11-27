package netology.cloudstorage;

import netology.cloudstorage.entity.FileCloudStorage;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.FileRepositoryCloud;
import netology.cloudstorage.service.FileServiceCloud;
import netology.cloudstorage.service.TokenServiceAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
class FileServiceCloudTest {
    private final String TEST_DIRECTORY = "C:/Java-Netology/uploadsNetologyCloudStorage/uploadsNetologyCloudStorage_Test/";
    @Mock
    private TokenServiceAuth tokenServiceAuth;
    @Mock
    private FileRepositoryCloud fileRepositoryCloud;
    @InjectMocks
    private FileServiceCloud service;

    @BeforeEach
        // блок для очистки ресурсов
    void setUp() {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            // Intentionally left empty
        } catch (Exception ignored) {
        }
    }

    // файл успешно загружается и сохраняются его метаданные
    @Test
    void shouldUploadFileSuccessfully() throws IOException {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream("Test Content".getBytes());
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getSize()).thenReturn(1024L);
        when(file.getBytes()).thenReturn("Test Content".getBytes());
        when(file.getContentType()).thenReturn("text/plain");
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenReturn(inputStream);

        String token = "valid_token";
        UserCloudStorage user = new UserCloudStorage();
        user.setId(1L);
        when(tokenServiceAuth.getUserByToken(token)).thenReturn(user);

        // Определяем userId (ID пользователя)
        Long userId = user.getId();

        // Используем Reflection для изменения приватного поля
        try {
            Field field = FileServiceCloud.class.getDeclaredField("UPLOAD_DIRECTORY");
            field.setAccessible(true);
            field.set(service, TEST_DIRECTORY);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Ошибка при изменении приватного поля: " + e.getMessage());
        }

        // Act
        service.uploadWithMetadata(file, userId);

        // Assert
        verify(fileRepositoryCloud, times(1)).save(any(FileCloudStorage.class));
    }

    // файл успешно скачивается
    @Test
    void shouldDownloadFileSuccessfully() throws IOException {
        // Arrange
        String filename = "test.txt";
        byte[] content = "Test Content".getBytes();

        try {
            // Используем Reflection для изменения приватного поля
            Field field = FileServiceCloud.class.getDeclaredField("UPLOAD_DIRECTORY");
            field.setAccessible(true);
            field.set(service, TEST_DIRECTORY);

            // Создаём файл в нужной директории
            Files.write(Paths.get(field.get(service) + filename), content);

            // Act
            byte[] downloadedContent = service.download(filename);

            // Assert
            assertArrayEquals(content, downloadedContent);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Ошибка при изменении приватного поля: " + e.getMessage());
        }
    }

    // файл успешно переименован
    @Test
    void shouldRenameFileSuccessfully() throws IOException {
        // Arrange
        String oldFilename = "old_test.txt";
        String newFilename = "new_test_rename.txt";
        FileCloudStorage existingFile = new FileCloudStorage();
        existingFile.setFilename(oldFilename);

        // Добавляем владельца файла
        UserCloudStorage owner = new UserCloudStorage();
        owner.setId(1L); // задаваем идентификатор пользователя
        existingFile.setOwner(owner); // назначаем владельца файла

        when(fileRepositoryCloud.findByFilename(oldFilename)).thenReturn(Optional.of(existingFile));

        // Используем Reflection для изменения приватного поля
        try {
            Field field = FileServiceCloud.class.getDeclaredField("UPLOAD_DIRECTORY");
            field.setAccessible(true);
            field.set(service, TEST_DIRECTORY);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Ошибка при изменении приватного поля: " + e.getMessage());
        }

        // Создаём исходный файл в файловой системе
        Files.write(Paths.get(TEST_DIRECTORY, oldFilename), "Old Content".getBytes());

        // Определяем userId (ID пользователя)
        Long userId = 1L;

        // Act
        service.rename(oldFilename, newFilename, userId);

        // Assert
        verify(fileRepositoryCloud, times(1)).save(existingFile);
        assertEquals(newFilename, existingFile.getFilename());
    }

    // список файлов корректно собирается
    @Test
    void shouldGetFilesForUserSuccessfully() {
        // Arrange
        Long userId = 1L;
        List<FileCloudStorage> userFiles = Arrays.asList(
                new FileCloudStorage(),
                new FileCloudStorage()
        );
        when(fileRepositoryCloud.findByOwnerId(userId)).thenReturn(userFiles);

        // Act
        List<FileServiceCloud.FileInfo> result = service.getFilesForUser(userId, 10);

        // Assert
        assertEquals(2, result.size());
    }

    // файл успешно удаляется
    @Test
    void shouldDeleteFileSuccessfully() throws IOException {
        // Arrange
        String filename = "test.txt";
        FileCloudStorage fileToRemove = new FileCloudStorage();
        fileToRemove.setFilename(filename);

        // Устанавливаем владельца файла
        UserCloudStorage owner = new UserCloudStorage();
        owner.setId(1L); // или другой идентификатор
        fileToRemove.setOwner(owner); // ставим владельца файла

        when(fileRepositoryCloud.findByFilename(filename)).thenReturn(Optional.of(fileToRemove));

        // Используем Reflection для изменения приватного поля
        try {
            Field field = FileServiceCloud.class.getDeclaredField("UPLOAD_DIRECTORY");
            field.setAccessible(true);
            field.set(service, TEST_DIRECTORY);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Ошибка при изменении приватного поля: " + e.getMessage());
        }

        // Создаём файл в файловой системе
        Files.write(Paths.get(TEST_DIRECTORY, filename), "Test Content".getBytes());

        // Определяем userId (ID пользователя)
        Long userId = 1L;

        // Act
        service.deleteFile(filename, userId);

        // Assert
        verify(fileRepositoryCloud, times(1)).delete(fileToRemove);
    }

}
