package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.Device;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUserId(Long userId);
    Optional<Device> findBySerialNumber(String serialNumber);
}