package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.CameraDto;
import pl.torun.alex.feeder.feeder_server.service.CameraService;

import java.util.List;

/**
 * Nested resource: cameras that belong to a specific user.
 * Routes: /users/{userId}/cameras
 */
@RestController
@RequestMapping("/users/{userId}/cameras")
@RequiredArgsConstructor
public class UserCameraController {

    private final CameraService cameraService;

    /** GET /users/{userId}/cameras — list all cameras for the given user. */
    @GetMapping
    @PreAuthorize("hasAuthority('read-schedule')")
    public List<CameraDto> listByUser(@PathVariable Long userId) {
        return cameraService.findByUserId(userId);
    }

    /** POST /users/{userId}/cameras/{cameraId} — assign an existing camera to the user. */
    @PostMapping("/{cameraId}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<CameraDto> assignToUser(
            @PathVariable Long userId,
            @PathVariable Long cameraId) {
        return ResponseEntity.ok(cameraService.assignToUser(cameraId, userId));
    }

    /** DELETE /users/{userId}/cameras/{cameraId} — remove the camera-user association. */
    @DeleteMapping("/{cameraId}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<Void> removeFromUser(
            @PathVariable Long userId,
            @PathVariable Long cameraId) {
        cameraService.removeFromUser(cameraId, userId);
        return ResponseEntity.noContent().build();
    }
}

