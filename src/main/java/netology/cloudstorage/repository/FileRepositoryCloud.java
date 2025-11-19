package netology.cloudstorage.repository;

import netology.cloudstorage.entity.FileCloudStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepositoryCloud extends JpaRepository<FileCloudStorage, Long> {
    // метод для поиска файла по имени
    Optional<FileCloudStorage> findByFilename(String filename);

    // метод для поиска файла по идентификатору пользователя
    List<FileCloudStorage> findByOwnerId(Long userId);
}
