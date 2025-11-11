package pl.torun.alex.feeder.feeder_server.service;

import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.DeviceDto;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.mapper.DeviceMapper;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final DeviceRepository repository;
    private final DeviceMapper mapper;

    public DeviceService(DeviceRepository repository, DeviceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<DeviceDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<DeviceDto> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public List<DeviceDto> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public DeviceDto create(DeviceDto dto) {
        Device entity = mapper.toEntity(dto);
        Device saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public DeviceDto update(Long id, DeviceDto dto) {
        Device entity = mapper.toEntity(dto);
        entity.setId(id);
        Device saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

