package netology.cloudstorage;

import netology.cloudstorage.service.TokenServiceAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
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
    private TokenServiceAuth tokenServiceAuth;

    @AfterEach
    void resetMocks() {
        Mockito.reset(tokenServiceAuth); // Сброс всех заглушек после каждого теста
    }

    @BeforeEach
        // блок для очистки ресурсов
    void setUp() {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            // Intentionally left empty
        } catch (Exception ignored) {
        }
    }

    // Unauthorized при невалидном токене
    @Test
    public void shouldFailUploadIfInvalidToken() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file",
                "test.txt", "text/plain", "Hello World!".getBytes(StandardCharsets.UTF_8));

        when(tokenServiceAuth.isValidToken(any())).thenReturn(false); // Токен недействителен

        ResultActions actions = mockMvc.perform(
                        MockMvcRequestBuilders.multipart("/file")
                                .file(multipartFile)
                                .header("auth-token", "invalid_token"))
                .andExpect(status().isUnauthorized()); // Ожидаем 401 Unauthorized

        actions.andDo(print()); // Распечатаем детали запроса и ответа
    }

}

