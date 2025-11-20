package netology.cloudstorage.service;

import jakarta.transaction.Transactional;
import netology.cloudstorage.entity.Token;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.TokenRepository;
import netology.cloudstorage.repository.UserRepositoryCloud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenServiceAuth {
    @Autowired
    private UserRepositoryCloud userRepository;
    @Autowired
    private TokenRepository tokenRepository;

    /* Генерирует новый токен и сохраняет его в базе данных.
       username - Имя пользователя, для которого создается токен */
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

    // Метод для поиска первого активного токена пользователя
    public Optional<Token> findFirstActiveToken(UserCloudStorage user) {
        return tokenRepository.findFirstByUserAndActiveTrue(user);
    }

    /* Проверяет, действителен ли указанный токен.
       Возвращает true, если токен активен, иначе false */
    public boolean isValidToken(String tokenValue) {
        Optional<Token> tokenOptional = tokenRepository.findByValue(tokenValue);
        return tokenOptional.map(Token::isActive).orElse(false);
    }

    /* Деактивирует указанный токен (при выходе пользователя из системы).
       После деактивации токен считается недействительным и его нельзя повторно использовать. */
    public void invalidateToken(String tokenValue) {
        Optional<Token> tokenOptional = tokenRepository.findByValue(tokenValue);
        tokenOptional.ifPresent(token -> {
            token.setActive(false);
            tokenRepository.save(token);
        });
    }

    /* Получает пользователя по значению токена.
       @return Пользователь, связанный с токеном, или null, если токен не найден */
    public UserCloudStorage getUserByToken(String tokenValue) {
        Optional<Token> tokenOptional = tokenRepository.findByValue(tokenValue);
        return tokenOptional.map(Token::getUser).orElse(null);
    }

}