package netology.cloudstorage.controller;

import netology.cloudstorage.dto.UserCreationDto;
import netology.cloudstorage.dto.UserDto;
import netology.cloudstorage.entity.UserCloudStorage;
import netology.cloudstorage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cloud/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody UserCreationDto creationDto) {
        userService.createUser(creationDto);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", "Учетная запись создана");
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMap);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserCloudStorage user = userService.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(convertToDto(user));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Метод для преобразования сущности в DTO
    private UserDto convertToDto(UserCloudStorage user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }
}
