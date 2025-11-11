package pl.torun.alex.feeder.feeder_server.service;

import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.DailySchedulerDto;
import pl.torun.alex.feeder.feeder_server.entity.DailyScheduler;
import pl.torun.alex.feeder.feeder_server.mapper.DailySchedulerMapper;
import pl.torun.alex.feeder.feeder_server.repository.DailySchedulerRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DailySchedulerService {

    private final DailySchedulerRepository repository;
    private final DailySchedulerMapper mapper;

    public DailySchedulerService(DailySchedulerRepository repository, DailySchedulerMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<DailySchedulerDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<DailySchedulerDto> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public List<DailySchedulerDto> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public List<DailySchedulerDto> findByDeviceId(Long deviceId) {
        return repository.findByDeviceId(deviceId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public List<DailySchedulerDto> findActive() {
        return repository.findByActiveTrue().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public DailySchedulerDto create(DailySchedulerDto dto) {
        DailyScheduler entity = mapper.toEntity(dto);
        DailyScheduler saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public DailySchedulerDto update(Long id, DailySchedulerDto dto) {
        DailyScheduler entity = mapper.toEntity(dto);
        entity.setId(id);
        DailyScheduler saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

