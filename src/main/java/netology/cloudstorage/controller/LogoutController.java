package netology.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.service.TokenServiceAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер выхода из системы.
 * Предоставляет API для деавторизации пользователя путём аннулирования действующего JWT-токена.
 * Используется совместно с сервисом {@link TokenServiceAuth}, который управляет состоянием токенов.
 */
@RestController
@RequestMapping("/logout")
@RequiredArgsConstructor
public class LogoutController {

    private final TokenServiceAuth tokenServiceAuth;

    private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);

    /**
     * Деавторизует пользователя путем аннулирования активного токена.
     * Этот метод получает действующий токен в заголовке запроса и вызывает службу
     * для его аннулирования. Затем формирует ответ с уведомлением об успехе.
     *
     * @param token токен аутентификации, полученный из заголовка "auth-token".
     * @return успешный ответ с сообщением "Logout successful" или статус ошибки в случае проблем.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("auth-token") String token) {
        logger.info("Attempting logout with token: {}", token);

        // Очищаем токен от приставки "Bearer "
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // Извлекаем чистый токен
        }

        if (token == null || token.isEmpty()) {
            logger.warn("Token is missing or empty");
            return ResponseEntity.badRequest().build();
        }

        // Аннулируем токен
        tokenServiceAuth.invalidateToken(token);
        logger.info("Token invalidated: {}", token);

        // Формируем ответ с сообщением об удачном выходе
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }
}
