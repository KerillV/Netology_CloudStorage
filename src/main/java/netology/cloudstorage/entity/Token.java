package netology.cloudstorage.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String value; // само значение токена

    private boolean active; // активность токена (true - активный, false - неактивный)

    // Связь с пользователем
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private UserCloudStorage user;

    @Column(columnDefinition="TIMESTAMP WITH TIME ZONE") // Поддержка временной зоны (часовой пояс)
    private LocalDateTime expiredAt; // Срок годности токена

}
