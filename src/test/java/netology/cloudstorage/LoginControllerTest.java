package netology.cloudstorage;

import netology.cloudstorage.controller.LoginController;
import netology.cloudstorage.dto.LoginRequestCloud;
import netology.cloudstorage.service.UserServiceAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class LoginControllerTest {
    @Mock
    private UserServiceAuth userServiceAuth;
    @InjectMocks
    private LoginController controller;

    @BeforeEach
        // блок для очистки ресурсов
    void setUp() {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            // Intentionally left empty
        } catch (Exception ignored) {
        }
    }

    // успешная авторизация
    @Test
    void shouldAuthorizeSuccessfully() {
        // Arrange
        LoginRequestCloud request = new LoginRequestCloud();
        request.setUsername("user");
        request.setPassword("password");

        String validToken = "valid_token";
        when(userServiceAuth.authenticateAndGenerateToken(
                "user",
                "password"))
                .thenReturn(validToken);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.login(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Optional.ofNullable(response.getBody())
                .map(body -> body.get("message"))
                .filter(msg -> msg.equals("Success authorization"))
                .orElseThrow(() -> new AssertionError("Unexpected message"));

        Optional.ofNullable(response.getBody())
                .map(body -> body.get("auth-token"))
                .filter(token -> token.equals(validToken))
                .orElseThrow(() -> new AssertionError("Unexpected token"));
    }


    // неверные логин/пароль
    @Test
    void shouldHandleBadCredentials() {
        // Arrange
        LoginRequestCloud request = new LoginRequestCloud();
        request.setUsername("user");
        request.setPassword("wrong_password");

        when(userServiceAuth.authenticateAndGenerateToken(
                "user",
                "wrong_password"))
                .thenThrow(RuntimeException.class);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.login(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Optional.ofNullable(response.getBody())
                .map(body -> body.get("message"))
                .filter(msg -> msg.equals("Bad credentials"))
                .orElseThrow(() -> new AssertionError("Unexpected message"));
    }

    // отсутствуют логин/пароль
    @Test
    void shouldHandleMissingLoginOrPassword() {
        // Arrange
        LoginRequestCloud request = new LoginRequestCloud();
        request.setUsername(null); // Отсутствие логина
        request.setPassword("password");

        // Act
        ResponseEntity<Map<String, Object>> response = controller.login(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Optional.ofNullable(response.getBody())
                .map(body -> body.get("message"))
                .filter(msg -> msg.equals("Missing login or password"))
                .orElseThrow(() -> new AssertionError("Unexpected message"));
    }

}
