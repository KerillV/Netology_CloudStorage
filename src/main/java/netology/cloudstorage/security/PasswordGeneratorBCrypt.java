package netology.cloudstorage.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGeneratorBCrypt {

    public static String encodePassword(String rawPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(rawPassword);
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoderAdmin = new BCryptPasswordEncoder();
        System.out.println("Зашифрованный пароль: " + encoderAdmin.encode("admin2"));
    }

}
