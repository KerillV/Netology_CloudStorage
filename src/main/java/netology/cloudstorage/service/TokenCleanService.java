package netology.cloudstorage.service;

import netology.cloudstorage.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenCleanService {
    @Autowired
    private TokenRepository tokenRepository;

    // Запускаем задачу на удаление токенов в полночь каждую неделю в воскресенье
    @Scheduled(cron = "0 0 0 ? * SUN")
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteByExpiredAtLessThan(now);
    }
}
