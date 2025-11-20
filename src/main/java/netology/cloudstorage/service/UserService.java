package netology.cloudstorage.service;

import netology.cloudstorage.dto.UserCreationDto;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.exceptions.DuplicateUsernameException;
import netology.cloudstorage.repository.UserRepositoryCloud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepositoryCloud userRepositoryCloud;
    @Autowired
    private PasswordEncoder passwordEncoder;

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


    public UserCloudStorage getUserById(Long id) {
        return userRepositoryCloud.findById(id).orElse(null);
    }

    public void deleteUser(Long id) {
        userRepositoryCloud.deleteById(id);
    }

}
