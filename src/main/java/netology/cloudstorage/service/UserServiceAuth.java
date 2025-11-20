package netology.cloudstorage.service;

import netology.cloudstorage.entity.Token;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.UserRepositoryCloud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.Optional;

/* Пользователь пытается войти, отправив своё имя и пароль.
Сервер ищет пользователя по имени в таблице users_cloudstorage.
Хеширует полученный пароль и сравнивает его с хранимым хешем.
Если всё верно, выдаёт токен доступа и сохраняет его. */

@Service
public class UserServiceAuth {
    @Autowired
    private UserRepositoryCloud userRepo;
    @Autowired
    private TokenServiceAuth tokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public String authenticateAndGenerateToken(String login, String rawPassword) {
        try {
            UserCloudStorage user = userRepo.findByLogin(login);
            if (user == null) {
                throw new AuthenticationException("User not found");
            }

            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                throw new AuthenticationException("Incorrect password");
            }

            // Проверяем, есть ли у пользователя действующий токен
            Optional<Token> existingToken = tokenService.findFirstActiveToken(user);
            if (existingToken.isPresent()) {
                // Действующий токен уже есть, возвращаем его значение
                return existingToken.get().getValue();
            } else {
                // Если токена нет, генерируем новый
                return tokenService.generateToken(login);
            }

        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }
    }
}
