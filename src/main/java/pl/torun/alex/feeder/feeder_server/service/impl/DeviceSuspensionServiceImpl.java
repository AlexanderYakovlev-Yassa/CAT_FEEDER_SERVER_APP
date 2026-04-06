package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.torun.alex.feeder.feeder_server.dto.DeviceSuspensionDto;
import pl.torun.alex.feeder.feeder_server.entity.DeviceSuspension;
import pl.torun.alex.feeder.feeder_server.mapper.DeviceSuspensionMapper;
import pl.torun.alex.feeder.feeder_server.repository.DeviceSuspensionRepository;
import pl.torun.alex.feeder.feeder_server.service.DeviceSuspensionService;
import pl.torun.alex.feeder.feeder_server.service.MainScheduleService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceSuspensionServiceImpl implements DeviceSuspensionService {

    private final DeviceSuspensionRepository repository;
    private final DeviceSuspensionMapper mapper;

    /**
     * The main scheduler is notified after every mutation so that any already-queued
     * feeding tasks are re-evaluated against the new suspension data.
     */
    private final MainScheduleService mainScheduleService;

    @Override
    public List<DeviceSuspensionDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DeviceSuspensionDto> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Override
    public List<DeviceSuspensionDto> findByDeviceId(Long deviceId) {
        return repository.findByDeviceId(deviceId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DeviceSuspensionDto create(DeviceSuspensionDto dto) {
        DeviceSuspension entity = mapper.toEntity(dto);
        DeviceSuspension saved = repository.save(entity);

        // Re-schedule so that any task that falls within the new suspension
        // window is cancelled before it fires.
        mainScheduleService.reschedule();

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public DeviceSuspensionDto update(Long id, DeviceSuspensionDto dto) {
        DeviceSuspension entity = mapper.toEntity(dto);
        entity.setId(id);
        DeviceSuspension saved = repository.save(entity);
        mainScheduleService.reschedule();
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);

        // Re-schedule so that feedings that were suppressed by this suspension
        // can now be added back to the task queue.
        mainScheduleService.reschedule();
    }

    /**
     * Queries the DB to find whether the device has any suspension window
     * that covers the given UTC instant.
     */
    @Override
    public boolean isDeviceSuspended(Long deviceId, Instant at) {
        return repository.existsActiveSuspension(deviceId, at);
    }
}
