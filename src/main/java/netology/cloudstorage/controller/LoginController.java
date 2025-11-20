package netology.cloudstorage.controller;

import netology.cloudstorage.dto.LoginRequestCloud;
import netology.cloudstorage.service.UserServiceAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cloud/login")
public class LoginController {
    @Autowired
    private UserServiceAuth userServiceAuth;

    @PostMapping
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestCloud request) {
        String login = request.getLogin();
        String password = request.getPassword();

        // Проверка наличия и заполнения полей
        if (login == null || login.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Missing login or password"), HttpStatus.BAD_REQUEST);
        }

        try {
            String token = userServiceAuth.authenticateAndGenerateToken(login, password);
            Map<String, Object> result = new HashMap<>();
            result.put("auth-token", token);
            result.put("message", "Success authorization");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", "Bad credentials"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
