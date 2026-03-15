package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.DeviceDto;
import pl.torun.alex.feeder.feeder_server.service.DeviceService;

import java.net.URI;
import java.util.List;

/**
 * Nested resource: operations on devices that belong to a specific user.
 * Routes: /users/{userId}/devices
 */
@RestController
@RequestMapping("/users/{userId}/devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final DeviceService deviceService;

    /** GET /users/{userId}/devices — list all devices for the given user. */
    @GetMapping
    @PreAuthorize("hasAuthority('read-feeders')")
    public List<DeviceDto> listByUser(@PathVariable Long userId) {
        return deviceService.findByUserId(userId);
    }

    /** POST /users/{userId}/devices — create a new device and assign it to the user. */
    @PostMapping
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<DeviceDto> addToUser(
            @PathVariable Long userId,
            @RequestBody DeviceDto device) {
        DeviceDto created = deviceService.assignToUser(userId, device);
        return ResponseEntity
                .created(URI.create("/api/users/" + userId + "/devices/" + created.getId()))
                .body(created);
    }

    /** DELETE /users/{userId}/devices/{id} — remove (delete) a device from the user. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<Void> removeFromUser(
            @PathVariable Long userId,
            @PathVariable Long id) {
        deviceService.removeFromUser(userId, id);
        return ResponseEntity.noContent().build();
    }
}

