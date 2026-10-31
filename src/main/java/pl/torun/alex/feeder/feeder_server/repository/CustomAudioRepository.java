package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.CustomAudio;

import java.util.List;

@Repository
public interface CustomAudioRepository extends JpaRepository<CustomAudio, Long> {
    List<CustomAudio> findAllByOrderByCreatedAtDesc();
}
