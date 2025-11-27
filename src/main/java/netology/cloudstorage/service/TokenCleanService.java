package netology.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.repository.TokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenCleanService {
    private final TokenRepository tokenRepository;

    /**
     * Периодическая задача для очистки устаревших токенов.
     * Каждый раз, когда эта задача срабатывает, она удаляет из базы данных все токены,
     * срок действия которых истек (токены, чей timestamp меньше текущего момента).
     */
    @Scheduled(cron = "0 0 0 ? * SUN")
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteByExpiredAtLessThan(now);
    }
}
