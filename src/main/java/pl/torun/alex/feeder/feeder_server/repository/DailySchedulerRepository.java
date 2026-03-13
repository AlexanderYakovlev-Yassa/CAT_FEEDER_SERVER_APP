package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.DailyScheduler;
import pl.torun.alex.feeder.feeder_server.entity.Device;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailySchedulerRepository extends JpaRepository<DailyScheduler, Long> {
    List<DailyScheduler> findByUserId(Long userId);
    List<DailyScheduler> findByDeviceId(Long deviceId);
    List<DailyScheduler> findByActiveTrue();

    Optional<DailyScheduler> findByDeviceAndActiveTrue(Device device);
}

