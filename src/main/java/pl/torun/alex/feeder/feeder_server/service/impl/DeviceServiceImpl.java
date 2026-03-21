package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.DeviceDto;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.mapper.DeviceMapper;
import pl.torun.alex.feeder.feeder_server.repository.AppUserRepository;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;
import pl.torun.alex.feeder.feeder_server.service.DeviceService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository repository;
    private final AppUserRepository userRepository;
    private final DeviceMapper mapper;

    @Override public List<DeviceDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override public Optional<DeviceDto> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Override public List<DeviceDto> findByUserId(Long userId) {
        return repository.findByUsersId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override public DeviceDto create(DeviceDto dto) {
        Device entity = mapper.toEntity(dto);
        Device saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Override public DeviceDto update(Long id, DeviceDto dto) {
        Device entity = mapper.toEntity(dto);
        entity.setId(id);
        Device saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Override public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public DeviceDto assignToUser(Long userId, DeviceDto dto) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        Device entity = mapper.toEntity(dto);
        // save device first (users mapping ignored by mapper)
        Device saved = repository.save(entity);
        // attach user and save again
        saved.getUsers().add(user);
        Device updated = repository.save(saved);
        return mapper.toDto(updated);
    }

    @Override
    public void removeFromUser(Long userId, Long deviceId) {
        Device device = repository.findById(deviceId)
                .orElseThrow(() -> new NoSuchElementException("Device not found: " + deviceId));
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (!device.getUsers().contains(user)) {
            throw new IllegalArgumentException(
                    "Device " + deviceId + " is not assigned to user " + userId);
        }

        device.getUsers().remove(user);
        if (device.getUsers().isEmpty()) {
            // if no users left, delete device
            repository.delete(device);
        } else {
            repository.save(device);
        }
    }
}
