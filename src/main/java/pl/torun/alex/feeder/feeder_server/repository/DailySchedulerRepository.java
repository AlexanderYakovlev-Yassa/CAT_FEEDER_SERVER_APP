package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.DailyScheduler;

import java.util.List;

@Repository
public interface DailySchedulerRepository extends JpaRepository<DailyScheduler, Long> {
    List<DailyScheduler> findByUserId(Long userId);
    List<DailyScheduler> findByDeviceId(Long deviceId);
    List<DailyScheduler> findByActiveTrue();
}

