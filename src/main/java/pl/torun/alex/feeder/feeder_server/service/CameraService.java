package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.CameraDto;

import java.util.List;
import java.util.Optional;

public interface CameraService {

    List<CameraDto> findAll();

    Optional<CameraDto> findById(Long id);

    List<CameraDto> findByUserId(Long userId);

    CameraDto create(CameraDto dto);

    CameraDto update(Long id, CameraDto dto);

    void delete(Long id);

    /** Assigns an existing camera to an existing user. */
    CameraDto assignToUser(Long cameraId, Long userId);

    /** Removes the association between a camera and a user. */
    void removeFromUser(Long cameraId, Long userId);

    /**
     * Returns true when the user identified by {@code username} has access
     * to the camera with the given logical name.
     * Used by {@code CameraStreamController} to enforce per-camera ownership.
     */
    boolean hasAccess(String cameraName, String username);
}

