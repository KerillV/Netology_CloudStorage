package netology.cloudstorage.repository;

import netology.cloudstorage.entity.UserCloudStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepositoryCloud extends JpaRepository<UserCloudStorage, Long> {

    Boolean existsByUsername(String username);

    UserCloudStorage findByUsername(String username);

    @Query("SELECT MAX(u.id) FROM UserCloudStorage u")
    Long getLastUserId();

}