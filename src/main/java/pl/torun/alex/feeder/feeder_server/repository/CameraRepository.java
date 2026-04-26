package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.Camera;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {

    Optional<Camera> findByName(String name);

    List<Camera> findByAutoStartTrue();

    List<Camera> findByUsersId(Long userId);
}

