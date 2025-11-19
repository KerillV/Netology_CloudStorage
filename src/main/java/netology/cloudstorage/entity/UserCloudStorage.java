package netology.cloudstorage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users_cloudstorage")
@Getter
@Setter
@NoArgsConstructor
public class UserCloudStorage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // unique уникальность значения поля
    private String username;

    @Column(nullable = false) // поле не может быть пустым
    private String passwordhash;

    public String getPasswordHash() {
        return passwordhash;
    }

    public void setPasswordHash(String passwordhash) {
        this.passwordhash = passwordhash;
    }

}

