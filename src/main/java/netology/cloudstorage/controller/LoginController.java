package netology.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.dto.LoginRequestCloud;
import netology.cloudstorage.service.UserServiceAuth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер авторизации пользователей.
 * Осуществляет обработку входящих запросов на вход в систему и выдачу JWT-токена
 * при успешной проверке учетных данных пользователя.
 *
 * <p>Использует сервис {@link UserServiceAuth} для аутентификации и генерации токенов.
 */
@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final UserServiceAuth userServiceAuth;

    private static final Logger LOGGER = LogManager.getLogger(LoginController.class);

    /**
     * Авторизирует пользователя и выдает JWT-токен.
     * Этот метод принимает объект с именем пользователя и паролем, проверяет введённые данные
     * и, в случае успеха, возвращает токен для дальнейшей авторизации.
     *
     * @param request объект с данными для входа (получается через тело запроса).
     * @return объект с результатом авторизации и JWT-токеном.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestCloud request) {
        LOGGER.info("Received login attempt: {}", request); // логируем приходящие данные
        String login = request.getLogin();
        LOGGER.info("логин {}", login);
        String password = request.getPassword();
        LOGGER.info("пароль {}", password);

        // Проверка наличия и заполнения полей
        if (login == null || login.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Missing login or password"), HttpStatus.BAD_REQUEST);
        }

        try {
            String token = userServiceAuth.authenticateAndGenerateToken(login, password);
            LOGGER.info("Successful login for user: {}", login); // успешная авторизация
            Map<String, Object> result = new HashMap<>();
            result.put("auth-token", token);
            result.put("message", "Success authorization");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            LOGGER.error("Login failed due to exception: ", e); // ошибка авторизации
            return new ResponseEntity<>(Map.of("message", "Bad credentials"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            LOGGER.error("Internal server error during login: ", e); // внутренняя ошибка
            return new ResponseEntity<>(Map.of("message", "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

