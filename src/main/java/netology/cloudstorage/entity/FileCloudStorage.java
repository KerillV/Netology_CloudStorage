package netology.cloudstorage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "files_cloudstorage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class FileCloudStorage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // значение поля генерируется автоматически
    private Long id;

    @Column
    private String filename;

    @Column
    private long size;

    /*
    Файл в таблице files_cloudstorage связан с одним владельцем UserCloudStorage через поле owner.
    Внешним ключом для связи с UserCloudStorage служит столбец user_id в таблице files_cloudstorage.
    Благодаря FetchType.LAZY, владелец UserCloudStorage не загружается автоматически при получении
    объекта FileCloudStorage, а только при прямом обращении к этому полю.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private UserCloudStorage owner;

    @Column
    private String checksum;

}
