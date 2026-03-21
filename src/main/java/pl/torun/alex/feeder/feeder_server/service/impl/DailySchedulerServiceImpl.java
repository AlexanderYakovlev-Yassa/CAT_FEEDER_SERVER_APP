package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.DailySchedulerDto;
import pl.torun.alex.feeder.feeder_server.entity.DailyScheduler;
import pl.torun.alex.feeder.feeder_server.mapper.DailySchedulerMapper;
import pl.torun.alex.feeder.feeder_server.repository.DailySchedulerRepository;
import pl.torun.alex.feeder.feeder_server.service.DailySchedulerService;
import pl.torun.alex.feeder.feeder_server.service.MainScheduleService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailySchedulerServiceImpl implements DailySchedulerService {

    private final DailySchedulerRepository repository;
    private final DailySchedulerMapper mapper;
    private final MainScheduleService mainScheduleService;

    @Override public List<DailySchedulerDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override public Optional<DailySchedulerDto> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Override public List<DailySchedulerDto> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override public List<DailySchedulerDto> findByDeviceId(Long deviceId) {
        return repository.findByDeviceId(deviceId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override public List<DailySchedulerDto> findActive() {
        return repository.findByActiveTrue().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override public DailySchedulerDto create(DailySchedulerDto dto) {
        DailyScheduler entity = mapper.toEntity(dto);
        DailyScheduler saved = repository.save(entity);
        mainScheduleService.reschedule();
        return mapper.toDto(saved);
    }

    @Override public DailySchedulerDto update(Long id, DailySchedulerDto dto) {
        DailyScheduler entity = mapper.toEntity(dto);
        entity.setId(id);
        DailyScheduler saved = repository.save(entity);
        mainScheduleService.reschedule();
        return mapper.toDto(saved);
    }

    @Override public void delete(Long id) {
        repository.deleteById(id);
        mainScheduleService.reschedule();
    }
}

