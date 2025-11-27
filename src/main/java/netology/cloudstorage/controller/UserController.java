package netology.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.dto.UserCreationDto;
import netology.cloudstorage.dto.UserDto;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления пользователями.
 * Содержит методы для создания новых пользователей, получения сведений о существующих пользователях и удаления аккаунтов.
 * Работает совместно с сервисом {@link UserService}, обеспечивая CRUD-операции над пользователями.
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Создание нового пользователя.
     * Обрабатывает запрос на создание аккаунта пользователя, принимая данные через объект {@link UserCreationDto},
     * и сохраняет новую запись в базу данных.
     *
     * @param creationDto объект с данными для создания пользователя.
     * @return успешный ответ с сообщением о создании аккаунта и статусом CREATED (201).
     */
    @PostMapping("/")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody UserCreationDto creationDto) {
        userService.createUser(creationDto);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", "Учетная запись создана");
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMap);
    }

    /**
     * Получение информации о пользователе по его идентификатору.
     * Возвращает подробную информацию о пользователе в виде объекта {@link UserDto}. Если пользователь с указанным ID не найден,
     * возвращает NOT_FOUND (404).
     *
     * @param id уникальный идентификатор пользователя.
     * @return объект с информацией о пользователе или статус NOT_FOUND, если пользователь не существует.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserCloudStorage user = userService.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(convertToDto(user));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Удаление пользователя по его идентификатору.
     * Удаляет аккаунт пользователя с указанным идентификатором и возвращает статус NO_CONTENT (204) при успешном выполнении.
     *
     * @param id уникальный идентификатор пользователя.
     * @return успешный ответ без тела.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Метод для преобразования сущности в DTO
    private UserDto convertToDto(UserCloudStorage user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setLogin(user.getLogin());
        return dto;
    }
}
