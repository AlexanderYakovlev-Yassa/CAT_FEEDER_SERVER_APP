package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.torun.alex.feeder.feeder_server.dto.CameraDto;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;
import pl.torun.alex.feeder.feeder_server.entity.Camera;
import pl.torun.alex.feeder.feeder_server.mapper.CameraMapper;
import pl.torun.alex.feeder.feeder_server.repository.AppUserRepository;
import pl.torun.alex.feeder.feeder_server.repository.CameraRepository;
import pl.torun.alex.feeder.feeder_server.service.CameraService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CameraServiceImpl implements CameraService {

    private final CameraRepository cameraRepository;
    private final AppUserRepository userRepository;
    private final CameraMapper mapper;

    @Override
    public List<CameraDto> findAll() {
        return cameraRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CameraDto> findById(Long id) {
        return cameraRepository.findById(id).map(mapper::toDto);
    }

    @Override
    public List<CameraDto> findByUserId(Long userId) {
        return cameraRepository.findByUsersId(userId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CameraDto create(CameraDto dto) {
        Camera entity = mapper.toEntity(dto);
        return mapper.toDto(cameraRepository.save(entity));
    }

    @Override
    public CameraDto update(Long id, CameraDto dto) {
        Camera entity = mapper.toEntity(dto);
        entity.setId(id);
        return mapper.toDto(cameraRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        cameraRepository.deleteById(id);
    }

    @Override
    @Transactional
    public CameraDto assignToUser(Long cameraId, Long userId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new NoSuchElementException("Camera not found: " + cameraId));
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        user.getCameras().add(camera);
        userRepository.save(user);
        return mapper.toDto(cameraRepository.findById(cameraId).orElseThrow());
    }

    @Override
    @Transactional
    public void removeFromUser(Long cameraId, Long userId) {
        Camera camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new NoSuchElementException("Camera not found: " + cameraId));
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (!user.getCameras().contains(camera)) {
            throw new IllegalArgumentException(
                    "Camera " + cameraId + " is not assigned to user " + userId);
        }
        user.getCameras().remove(camera);
        userRepository.save(user);
    }

    @Override
    public boolean hasAccess(String cameraName, String username) {
        return cameraRepository.findByName(cameraName)
                .map(cam -> cam.getUsers().stream()
                        .anyMatch(u -> u.getUsername().equals(username)))
                .orElse(false);
    }
}

