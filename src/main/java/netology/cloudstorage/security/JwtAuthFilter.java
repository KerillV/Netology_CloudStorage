package netology.cloudstorage.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.service.TokenServiceAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final TokenServiceAuth tokenServiceAuth;


    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    /**
     * Основной метод фильтра, обрабатывающий входящие запросы.
     *
     * Производит проверку заголовков запроса на предмет валидных JWT-токенов и выполняет необходимые операции аутентификации.
     * Валидация включает проверку наличия токена, правильность его форматов и допустимых расширений файла.
     * После успешной проверки токена выполняется дальнейшая обработка запроса.
     *
     * @param request Входящий запрос ({@code ServletRequest}).
     * @param response Ответ сервера ({@code ServletResponse}).
     * @param chain Цепочка последующих фильтров ({@code FilterChain}).
     * @throws IOException Возможные проблемы ввода-вывода.
     * @throws ServletException Исключения, возникающие при обработке запроса сервлетом.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Получаем заголовок Authorization
        String authHeader = httpRequest.getHeader("Authorization");
        logger.info("Необработанный токен из заголовка Authorization: {}", authHeader);
        String tokenFromAuth = null;

        // Проверяем наличие и корректность формата заголовка
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenFromAuth = authHeader.substring(7); // Извлекаем чистый токен
            logger.info("Обработанный токен от Bearer: {}", tokenFromAuth);
        }

        // Получаем токен из заголовка auth-token
        String tokenFromAuthToken = httpRequest.getHeader("auth-token");
        logger.info("tokenFromAuthToken : {}", tokenFromAuthToken);

        // Очистка токена от auth-token
        if (tokenFromAuthToken != null && tokenFromAuthToken.startsWith("Bearer ")) {
            tokenFromAuthToken = tokenFromAuthToken.substring(7); // Извлекаем чистый токен
            logger.info("Обработанный токен от auth-token: {}", tokenFromAuthToken);
        }

        // Выбор приоритета: сначала проверяем Authorization, потом auth-token
        String token = tokenFromAuth != null ? tokenFromAuth : tokenFromAuthToken;
        logger.info("выбор приоритета : {}", token);

        // Пропускаем OPTIONS-запросы
        if (httpRequest.getMethod().equalsIgnoreCase("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }

        // Пропускаем проверку токена для конкретных маршрутов
        if (httpRequest.getRequestURI().equals("/login") ||
                httpRequest.getRequestURI().startsWith("/user/")) {
            logger.debug("Пропуск проверки токена для определенных маршрутов");
            chain.doFilter(request, response); // Пропускаем запрос дальше
            return;
        }

        // Проверяем токен
        if (token != null && tokenServiceAuth.isValidToken(token)) {
            logger.info("Валидный токен: {}", token);

            // Получаем текущего пользователя из токена
            UserCloudStorage user = tokenServiceAuth.getUserByToken(token);
            if (user != null) {
                // Создаём объект Authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                AuthorityUtils.createAuthorityList("ROLE_USER"));

                // Присваиваем объект Authentication контексту безопасности
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("Validating successful, proceeding further");
                chain.doFilter(request, response); // Пропускаем запрос дальше
            } else {
                logger.error("User not found with token: {}", token); // Логируем проблему с пользователем
                terminateRequest(httpResponse, "User not found");
            }
        } else {
            logger.error("Invalid token received: {}", token); // Логируем проблему с токеном
            terminateRequest(httpResponse, "Invalid token");
        }
    }

    /**
     * Терминатор запроса, полностью завершает обработку запроса с ошибкой 401.
     * Завершение обработки запроса с ошибкой аутентификации.
     *
     * Отправляет клиенту ошибку с кодом состояния 401 (Unauthorized),
     * записывая сообщение об ошибке в ответ и завершая поток вывода.
     *
     * @param response Объект ответа клиента ({@code HttpServletResponse}).
     * @param message Сообщение об ошибке, отправляемое клиенту.
     * @throws IOException Возможные проблемы ввода-вывода.
     */
    private void terminateRequest(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
        response.flushBuffer(); // Завершаем буферизацию
        response.getOutputStream().flush();
        response.getOutputStream().close();
        logger.debug("Unauthorized request terminated successfully");
    }

}