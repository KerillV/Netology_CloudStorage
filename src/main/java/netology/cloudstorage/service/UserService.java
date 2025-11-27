package netology.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import netology.cloudstorage.dto.UserCreationDto;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.exceptions.DuplicateUsernameException;
import netology.cloudstorage.repository.UserRepositoryCloud;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Сервис для работы с пользователями.
 * Предоставляет методы для создания, получения и удаления пользователей.
 * Обеспечивает шифрование паролей перед сохранением в базе данных.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepositoryCloud userRepositoryCloud;
    private final PasswordEncoder passwordEncoder;

    /**
     * Создает нового пользователя.
     * Сначала проверяет, не занят ли логин другого пользователя, затем зашифровывает пароль и сохраняет пользователя в базу данных.
     *
     * @param creationDto объект с данными для создания пользователя.
     * @throws DuplicateUsernameException если логин уже занят другим пользователем.
     */
    public void createUser(UserCreationDto creationDto) {
        // Проверяем, существует ли пользователь с таким именем
        if (userRepositoryCloud.existsByLogin(creationDto.getLogin())) {
            throw new DuplicateUsernameException("Имя пользователя уже занято");
        }

        UserCloudStorage user = new UserCloudStorage();
        user.setLogin(creationDto.getLogin());
        user.setPasswordHash(passwordEncoder.encode(creationDto.getPassword()));
        userRepositoryCloud.save(user);
    }

    /**
     * Получает пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя.
     * @return объект пользователя или null, если пользователь не найден.
     */
    public UserCloudStorage getUserById(Long id) {
        return userRepositoryCloud.findById(id).orElse(null);
    }

    /**
     * Удаляет пользователя по его идентификатору.
     *
     * @param id идентификатор пользователя.
     */
    public void deleteUser(Long id) {
        userRepositoryCloud.deleteById(id);
    }

}
