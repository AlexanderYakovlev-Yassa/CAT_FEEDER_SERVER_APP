package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.CameraDto;
import pl.torun.alex.feeder.feeder_server.service.CameraService;

import java.net.URI;
import java.util.List;

/**
 * CRUD management of Camera entities.
 *
 * <pre>
 * GET    /cameras            – list all cameras
 * GET    /cameras/{id}       – get camera by id
 * POST   /cameras            – create camera
 * PUT    /cameras/{id}       – update camera
 * DELETE /cameras/{id}       – delete camera
 * POST   /cameras/{id}/users/{userId}  – assign user to camera
 * DELETE /cameras/{id}/users/{userId}  – remove user from camera
 * </pre>
 */
@RestController
@RequestMapping("/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    @GetMapping
    @PreAuthorize("hasAuthority('manage-schedule')")
    public List<CameraDto> listAll() {
        return cameraService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<CameraDto> getById(@PathVariable Long id) {
        return cameraService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<CameraDto> create(@RequestBody CameraDto dto) {
        CameraDto created = cameraService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/cameras/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<CameraDto> update(@PathVariable Long id, @RequestBody CameraDto dto) {
        return ResponseEntity.ok(cameraService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cameraService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Assign an existing user to this camera. */
    @PostMapping("/{id}/users/{userId}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<CameraDto> assignUser(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(cameraService.assignToUser(id, userId));
    }

    /** Remove a user from this camera. */
    @DeleteMapping("/{id}/users/{userId}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<Void> removeUser(@PathVariable Long id, @PathVariable Long userId) {
        cameraService.removeFromUser(id, userId);
        return ResponseEntity.noContent().build();
    }
}

