package netology.cloudstorage.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import netology.cloudstorage.entity.Token;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.TokenRepository;
import netology.cloudstorage.repository.UserRepositoryCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для работы с токенами аутентификации.
 * Обеспечивает создание, проверку и деактивацию токенов для авторизации пользователей.
 * Поддерживает связывание токенов с пользователями и отслеживание срока действия токенов.
 */
@Service
@RequiredArgsConstructor
public class TokenServiceAuth {
    private final UserRepositoryCloud userRepository;
    private final TokenRepository tokenRepository;

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceAuth.class);

    /**
     * Генерирует новый токен и сохраняет его в базе данных.
     * Создается новый токен с уникальным значением, устанавливается активный статус и срок действия на 24 часа вперёд.
     * Связывает созданный токен с пользователем, исходя из его логина.
     *
     * @param login логин пользователя, для которого создается токен.
     * @return строковое представление токена.
     */
    @Transactional
    public String generateToken(String login) {
        String tokenValue = UUID.randomUUID().toString();
        Token token = new Token();
        token.setValue(tokenValue);
        token.setActive(true);

        // Устанавливаем срок действия токена на 24 часа вперед
        token.setExpiredAt(LocalDateTime.now().plusHours(24));

        // находим пользователя по имени
        UserCloudStorage user = userRepository.findByLogin(login);
        token.setUser(user); // назначаем владельца токена

        tokenRepository.save(token);
        return tokenValue;
    }

    /**
     * Находит первый активный токен для указанного пользователя.
     *
     * @param user пользователь, для которого ищется активный токен.
     * @return необязательный объект токена (если токен найден и активен).
     */
    public Optional<Token> findFirstActiveToken(UserCloudStorage user) {
        return tokenRepository.findFirstByUserAndActiveTrue(user);
    }

    /**
     * Проверяет, действителен ли указанный токен.
     * Токен считается действительным, если он присутствует в базе данных и находится в активном состоянии.
     *
     * @param tokenValue строковое значение токена.
     * @return true, если токен активен, иначе false.
     */
    public boolean isValidToken(String tokenValue) {
        logger.info("Проверка действительности токена: {}", tokenValue);
        Optional<Token> tokenOptional = tokenRepository.findByValue(tokenValue);
        return tokenOptional.map(Token::isActive).orElse(false);
    }

    /**
     * Деактивирует указанный токен.
     * Токен переключается в неактивное состояние, что предотвращает его дальнейшее использование.
     *
     * @param tokenValue строковое значение токена.
     */
    @Transactional
    public void invalidateToken(String tokenValue) {
        logger.info("токен отправлен в invalidateToken: {}", tokenValue);
        Optional<Token> tokenOptional = tokenRepository.findByValue(tokenValue);
        if (tokenOptional.isPresent()) {
            Token token = tokenOptional.get();
            token.setActive(false);
            tokenRepository.save(token);
            logger.info("Token deactivated: {}", tokenValue);
        } else {
            logger.warn("Token not found: {}", tokenValue);
        }
    }

    /**
     * Получает пользователя по его идентификатору.
     *
     * @param userId идентификатор пользователя.
     * @return объект пользователя или null, если пользователь не найден.
     */
    public UserCloudStorage getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * Получает пользователя по значению токена.
     *
     * @param tokenValue строковое значение токена.
     * @return объект пользователя, ассоциированного с данным токеном, или null, если токен не найден.
     */
    public UserCloudStorage getUserByToken(String tokenValue) {
        Optional<Token> tokenOptional = tokenRepository.findByValue(tokenValue);
        return tokenOptional.map(Token::getUser).orElse(null);
    }

}