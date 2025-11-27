package netology.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.entity.Token;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.UserRepositoryCloud;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.Optional;

/**
 * Сервис аутентификации пользователей.
 * Предоставляет методы для проверки подлинности пользователя и выдачи токена.
 * Аутентификация осуществляется путем сравнения хеша пароля и последующего формирования токена доступа.
 */
@Service
@RequiredArgsConstructor
public class UserServiceAuth {
    private final UserRepositoryCloud userRepo;
    private final TokenServiceAuth tokenService;
    private final PasswordEncoder passwordEncoder;

    private static final Logger LOGGER = LogManager.getLogger(UserServiceAuth.class);

    /**
     * Аутентифицирует пользователя и генерирует токен.
     * Процесс включает поиск пользователя по имени, сравнение полученного пароля с сохранённым хешем и выдачу токена.
     * Если подходящий токен уже существует, он возвращается. Иначе генерируется новый токен.
     *
     * @param login логин пользователя.
     * @param rawPassword незашифрованный пароль пользователя.
     * @return токен доступа.
     */
    public String authenticateAndGenerateToken(String login, String rawPassword) {
        try {
            UserCloudStorage user = userRepo.findByLogin(login);
            if (user == null) {
                LOGGER.error("User not found for login: {}", login); // пользователь не найден
                throw new AuthenticationException("User not found");
            }

            // Проверяем совпадение пароля с сохраненным хешем
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                LOGGER.error("Incorrect password provided for user: {}", login); // неверный пароль
                throw new AuthenticationException("Incorrect password");
            }

            // Проверяем, есть ли у пользователя действующий токен
            Optional<Token> existingToken = tokenService.findFirstActiveToken(user);
            if (existingToken.isPresent()) {
                LOGGER.info("token уже есть: {}", existingToken); // проверка токена
                // Действующий токен уже есть, возвращаем его значение
                return existingToken.get().getValue();
            } else {
                // Если токена нет, генерируем новый
                LOGGER.info("Generating token for user: {}", login); // генерация токена
                return tokenService.generateToken(login);
            }

        } catch (AuthenticationException e) {
            LOGGER.error("Authentication failure: ", e); // сбой аутентификации
            throw new RuntimeException(e);
        }
    }

    /**
     * Возвращает текущего аутентифицированного пользователя.
     * Извлекает пользователя из контекста безопасности Spring Security.
     *
     * @return текущий пользователь.
     */
    public UserCloudStorage currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LOGGER.info("Authentication object: {}", authentication);
        return (UserCloudStorage) authentication.getPrincipal();
    }
}
