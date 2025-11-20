package netology.cloudstorage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreationDto {
    private String login; // почтовый ящик
    private String password;
}
