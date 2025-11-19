package netology.cloudstorage.controller;

import netology.cloudstorage.service.TokenServiceAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cloud/logout")
public class LogoutController {

    @Autowired
    private TokenServiceAuth tokenServiceAuth;

    @PostMapping
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("auth-token") String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        tokenServiceAuth.invalidateToken(token); // метод аннулирования токена

        // Формируем ответ с сообщением об удачном выходе
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logout");
        return ResponseEntity.ok(response);
    }
}
