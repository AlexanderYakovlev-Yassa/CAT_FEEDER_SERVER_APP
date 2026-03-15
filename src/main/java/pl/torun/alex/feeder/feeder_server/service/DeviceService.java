package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.DeviceDto;

import java.util.List;
import java.util.Optional;

public interface DeviceService {
    List<DeviceDto> findAll();

    Optional<DeviceDto> findById(Long id);

    List<DeviceDto> findByUserId(Long userId);

    DeviceDto create(DeviceDto dto);

    DeviceDto update(Long id, DeviceDto dto);

    void delete(Long id);

    /** Creates a new device and assigns it to the given user. */
    DeviceDto assignToUser(Long userId, DeviceDto dto);

    /** Removes a device from a user; throws if the device doesn't belong to that user. */
    void removeFromUser(Long userId, Long deviceId);
}
