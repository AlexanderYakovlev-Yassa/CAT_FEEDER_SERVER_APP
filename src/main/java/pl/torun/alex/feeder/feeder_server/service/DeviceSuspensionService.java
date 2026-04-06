package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.DeviceSuspensionDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceSuspensionService {

    List<DeviceSuspensionDto> findAll();

    Optional<DeviceSuspensionDto> findById(Long id);

    /** Returns all suspension windows registered for the given device. */
    List<DeviceSuspensionDto> findByDeviceId(Long deviceId);

    /** Creates a new suspension window and triggers a scheduler reschedule. */
    DeviceSuspensionDto create(DeviceSuspensionDto dto);

    /** Updates an existing suspension window and triggers a scheduler reschedule. */
    DeviceSuspensionDto update(Long id, DeviceSuspensionDto dto);

    /** Removes a suspension window and triggers a scheduler reschedule. */
    void delete(Long id);

    /**
     * Returns {@code true} when the device has an active suspension at the given UTC instant.
     * Called by the scheduler just before it sends a feeding command.
     * Using Instant keeps the check timezone-agnostic.
     */
    boolean isDeviceSuspended(Long deviceId, Instant at);
}

