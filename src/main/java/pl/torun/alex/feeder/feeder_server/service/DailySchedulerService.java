package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.DailySchedulerDto;

import java.util.List;
import java.util.Optional;

public interface DailySchedulerService {

    List<DailySchedulerDto> findAll();
    Optional<DailySchedulerDto> findById(Long id);
    List<DailySchedulerDto> findByUserId(Long userId);
    List<DailySchedulerDto> findByDeviceId(Long deviceId);
    List<DailySchedulerDto> findActive();
    DailySchedulerDto create(DailySchedulerDto dto);
    DailySchedulerDto update(Long id, DailySchedulerDto dto);
    void delete(Long id);
}
