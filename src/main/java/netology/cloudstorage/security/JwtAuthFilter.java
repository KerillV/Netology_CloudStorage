package netology.cloudstorage.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.service.TokenServiceAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthFilter implements Filter {
    @Autowired
    private TokenServiceAuth tokenServiceAuth;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String token = httpRequest.getHeader("auth-token");

        logger.info("Processing request with token: {}", token); // Логируем получение токена

        // Пропускаем проверку токена для конкретных маршрутов
        if (httpRequest.getRequestURI().equals("/cloud/login") ||
                httpRequest.getRequestURI().startsWith("/cloud/user/")) {
            logger.debug("Skipping token check for specific routes");
            chain.doFilter(request, response); // Пропускаем запрос дальше
            return;
        }

        if (!tokenServiceAuth.isValidToken(token)) {
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("Unauthorized access.");
            return;
        }

        if (token == null || token.isEmpty()) {
            logger.warn("Missing auth-token header"); // Логируем отсутствие токена
            terminateRequest(httpResponse, "Missing auth-token header");
            return;
        }

        try {
            if (tokenServiceAuth.isValidToken(token)) {
                UserCloudStorage user = tokenServiceAuth.getUserByToken(token);
                if (user != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    AuthorityUtils.createAuthorityList("ROLE_USER"));
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
        } catch (Exception ex) {
            logger.error("An internal error occurred while processing token", ex); // Логируем внутреннюю ошибку
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    // Полностью завершает обработку запроса с ошибкой 401.
    private void terminateRequest(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
        response.flushBuffer(); // Завершаем буферизацию
        response.getOutputStream().flush();
        response.getOutputStream().close();
        logger.debug("Unauthorized request terminated successfully");
    }

}