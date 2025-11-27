package netology.cloudstorage.config;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Настройка цепочки фильтров безопасности.
     * Этот метод конфигурирует цепочку фильтров безопасности Spring Security.
     * Включает настройку защиты от CSRF, обработку CORS, управление правами доступа
     * и регистрацию кастомного фильтра JWT {@link JwtAuthFilter}, обеспечивающего проверку токенов.
     *
     * @param http Объект настройки безопасности HTTP ({@code HttpSecurity}).
     * @return Конфигурационная цепочка фильтров безопасности.
     * @throws Exception Возможные исключения при создании конфигурации.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(CsrfConfigurer::disable) // отключаем CSRF для взаимодействия с фронтендом
                .cors(CorsConfigurer::disable) // включаем обработку CORS
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // разрешаем запросы OPTION от фронтенда
                        .requestMatchers("/user/**").permitAll() // общий доступ к управлению пользователями
                        .requestMatchers("/login").permitAll() // открытый доступ к пути /login
                        .anyRequest().authenticated()) // остальные запросы требуют авторизации
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Убираем стандартный LogoutFilter, чтобы позволить запрос достигать LogoutController
        http.logout(LogoutConfigurer::disable);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
