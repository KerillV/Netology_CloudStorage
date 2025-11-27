package netology.cloudstorage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Класс конфигурации веб-приложения Cloud Storage, позволяющий взаимодействовать с фронтэндом.
 * Данный класс реализует интерфейс {@link WebMvcConfigurer}, позволяя настроить
 * дополнительную конфигурацию MVC и разрешить работу с междоменными запросами (CORS).
 */
@Configuration
@EnableWebMvc
public class WebConfigCloud implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                // разрешенный адрес в параметре allowedOrigins – это адрес фронта
                .allowedOrigins("http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Cache-Control", "Content-Type", "Location", "Access-Control-Allow-Origin");
    }
}
