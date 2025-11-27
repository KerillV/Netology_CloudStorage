package netology.cloudstorage;

import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.repository.UserRepositoryCloud;
import netology.cloudstorage.security.PasswordGeneratorBCrypt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IntegrationTests {
    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepositoryCloud userRepositoryCloud;

    @BeforeEach
    public void setup() {
    }

    @AfterEach // очистка БД после каждого теста
    public void cleanup() {
        userRepositoryCloud.deleteAll();
    }

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withUsername("test_user@mail.ru")
            .withPassword("test_pass")
            .withDatabaseName("test_db");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
        // блок для очистки ресурсов
    void setUp() {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            // Intentionally left empty
        } catch (Exception ignored) {
        }
    }

    final Logger logger = LogManager.getLogger(IntegrationTests.class);

    @Test
    public void testCreateUser() {

        // JSON-представление пользователя
        String jsonPayload = """
                    {
                        "login": "new-user@mail.ru",
                        "password": "password-hash"
                    }
                """;

        // Устанавливаем правильные заголовки
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Логируем JSON-пэйлоад
        logger.info("JSON payload: {}", jsonPayload);

        // Отправляем POST-запрос на создание пользователя
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/user/",
                HttpMethod.POST,
                new HttpEntity<>(jsonPayload, headers),
                String.class
        );

        // Логируем статус ответа
        logger.info("Response status: {}", response.getStatusCode());

        // Проверяем статус ответа
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Проверяем тело ответа
        String body = response.getBody();
        if (body != null) {
            logger.info("Response body: {}", body);
            assertTrue(body.contains("\"message\":\"Учетная запись создана\""));
        } else {
            fail("Тело ответа пустое или null");
        }

        // Дополнительно можем проверить, что пользователь создалсся в базе данных
        UserCloudStorage createdUser = userRepositoryCloud.findByLogin("new-user@mail.ru");
        assertNotNull(createdUser);
    }

    @Test
    public void testReadUser() {
        final Logger logger = LogManager.getLogger(UserRepositoryCloud.class);

        // Создаем пользователя перед тестом
        UserCloudStorage user = new UserCloudStorage();
        user.setLogin("test-user@mail.ru");

        // Хешируем пароль и выводим его в лог
        String hashedPassword = PasswordGeneratorBCrypt.encodePassword("password-hash");
        user.setPasswordHash(hashedPassword);
        logger.info("Хешированный пароль перед сохранением: {}", hashedPassword);

        // Сохраняем пользователя
        userRepositoryCloud.save(user);

        // Проверяем, что пароль сохранился корректно
        UserCloudStorage savedUser = userRepositoryCloud.findByLogin("test-user@mail.ru");
        logger.info("Пароль после сохранения: {}", savedUser.getPasswordHash());

        // Проверяем, что контейнер запущен
        if (!postgres.isRunning()) {
            fail("Контейнер PostgreSQL не запущен");
        }

        // Получаем ID последнего пользователя
        Long lastUserId = userRepositoryCloud.getLastUserId();

        // Логируем результат
        logger.info("Последний ID пользователя: {}", lastUserId);

        if (lastUserId == null) {
            fail("Ни одного пользователя не найдено в базе данных");
        }

        // Проверяем существование пользователя с данным ID (без использования токена)
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/user/" + lastUserId,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Проверяем тело ответа
        String body = response.getBody();
        if (body != null) {
            assertTrue(body.contains("\"login\":\"test-user@mail.ru\"")); // Проверяем присутствие имени
        } else {
            fail("Тело ответа пустое или null");
        }
    }

    @Test
    public void testDeleteUser() {
        // Создаем пользователя перед тестом
        UserCloudStorage user = new UserCloudStorage();
        user.setLogin("test-user@mail.ru");
        user.setPasswordHash(PasswordGeneratorBCrypt.encodePassword("password-hash"));
        userRepositoryCloud.save(user);

        // Получаем ID последнего пользователя
        Long lastUserId = userRepositoryCloud.getLastUserId();

        // Проверяем, что ID не null
        if (lastUserId == null) {
            fail("Не удалось получить ID пользователя");
        }

        // DELETE-запрос на удаление пользователя
        ResponseEntity<Void> response = restTemplate.exchange(
                "http://localhost:" + port + "/user/" + lastUserId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        // Проверяем статус ответа
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Проверяем, что пользователь действительно удалился
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/user/" + lastUserId,
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

}
