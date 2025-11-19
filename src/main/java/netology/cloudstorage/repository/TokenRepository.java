package netology.cloudstorage.repository;

import netology.cloudstorage.entity.Token;
import netology.cloudstorage.entity.UserCloudStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    // Поиск токена по значению
    Optional<Token> findByValue(String tokenValue);

    // Поиск активного токена пользователя
    Optional<Token> findFirstByUserAndActiveTrue(UserCloudStorage user);

    // Удаляем все токены, чей срок истек
    void deleteByExpiredAtLessThan(LocalDateTime date);

}
