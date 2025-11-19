package netology.cloudstorage;

import netology.cloudstorage.controller.FileController;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.service.FileServiceCloud;
import netology.cloudstorage.service.TokenServiceAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Mock
    private FileServiceCloud fileServiceCloud;
    @Mock
    private TokenServiceAuth tokenServiceAuth;
    @InjectMocks
    private FileController controller;
    @AfterEach
    void resetMocks() {
        Mockito.reset(tokenServiceAuth); // Сброс всех заглушек после каждого теста
    }
    @BeforeEach // блок для очистки ресурсов
    void setUp() {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            // Intentionally left empty
        } catch (Exception ignored) {}
    }

    // успешная загрузка файла
    @Test
    void shouldUploadFileSuccessfully() {
        MockMultipartFile multipartFile = new MockMultipartFile("file",
                "test.txt", "text/plain", "This is a longer text for testing purposes.".getBytes(StandardCharsets.UTF_8));
        when(tokenServiceAuth.isValidToken(any())).thenReturn(true); // Токен корректен

        ResponseEntity<?> response = controller.uploadFile(multipartFile, "valid_token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success upload", response.getBody());
    }

    // Unauthorized при невалидном токене
    @Test
    public void shouldFailUploadIfInvalidToken() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file",
                "test.txt", "text/plain", "Hello World!".getBytes(StandardCharsets.UTF_8));

        when(tokenServiceAuth.isValidToken(any())).thenReturn(false); // Токен недействителен

        ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders.multipart("/cloud/file")
                                .file(multipartFile)
                                .header("auth-token", "invalid_token"))
                .andExpect(status().isUnauthorized()); // Ожидаем 401 Unauthorized

        actions.andDo(print()); // Распечатаем детали запроса и ответа
    }

    // Скачивание файла
    @Test
    void shouldDownloadFileSuccessfully() throws IOException {
        when(tokenServiceAuth.isValidToken(any())).thenReturn(true);
        when(fileServiceCloud.download(any())).thenReturn("test data".getBytes());

        ResponseEntity<?> response = controller.downloadFile("test_file.txt", "valid_token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Редактирование имени файла
    @Test
    void shouldEditFileNameSuccessfully() throws IOException {
        when(tokenServiceAuth.isValidToken(any())).thenReturn(true);
        // перехватываем исключение, если оно возникнет
        doNothing().when(fileServiceCloud).rename(any(), any());

        ResponseEntity<?> response = controller.editFileName("old_name.txt", Collections.singletonMap("name", "new_name.txt"), "valid_token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Удаление файла
    @Test
    void shouldDeleteFileSuccessfully() {
        when(tokenServiceAuth.isValidToken(any())).thenReturn(true);
        doNothing().when(fileServiceCloud).deleteFile(any()); // Для метода void используем doNothing()

        ResponseEntity<?> response = controller.deleteFile("delete_me.txt", "valid_token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Получение списка файлов
    @Test
    void shouldGetFilesListSuccessfully() {
        List<FileServiceCloud.FileInfo> files = new ArrayList<>();
        UserCloudStorage user = new UserCloudStorage();
        user.setId(1L);
        when(tokenServiceAuth.isValidToken(any())).thenReturn(true);
        when(tokenServiceAuth.getUserByToken(any())).thenReturn(user);
        when(fileServiceCloud.getFilesForUser(eq(1L), eq(10))).thenReturn(files); // Правильно указан тип возвращаемого значения

        ResponseEntity<List<FileServiceCloud.FileInfo>> response = controller.getFiles(10, "valid_token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}

