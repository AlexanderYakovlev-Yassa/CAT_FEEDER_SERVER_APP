package pl.torun.alex.feeder.feeder_server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.DeviceSuspensionDto;
import pl.torun.alex.feeder.feeder_server.service.DeviceSuspensionService;

import java.net.URI;
import java.util.List;

/**
 * REST API for managing device suspension windows.
 *
 * A suspension tells the scheduler to skip all feeding tasks for the target
 * device whose scheduled time falls inside [startSuspension, endSuspension].
 *
 * Base path: /suspensions
 */
@RestController
@RequestMapping("/suspensions")
@RequiredArgsConstructor
public class DeviceSuspensionController {

    private final DeviceSuspensionService service;

    /** Returns every suspension stored in the system. */
    @GetMapping
    @PreAuthorize("hasAuthority('read-schedule')")
    public List<DeviceSuspensionDto> listAll() {
        return service.findAll();
    }

    /** Returns all suspensions registered for a specific device. */
    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasAuthority('read-schedule')")
    public List<DeviceSuspensionDto> listByDevice(@PathVariable Long deviceId) {
        return service.findByDeviceId(deviceId);
    }

    /** Returns a single suspension by its DB id. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<DeviceSuspensionDto> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates a new suspension window for the given device.
     *
     * Request body example (timestamps are Unix epoch seconds, UTC):
     * {
     *   "deviceId": 1,
     *   "startSuspension": 1744264800,
     *   "endSuspension":   1744574400
     * }
     *
     * The scheduler is automatically rescheduled after creation so that any
     * feeding task falling inside the window is cancelled immediately.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<DeviceSuspensionDto> create(@Valid @RequestBody DeviceSuspensionDto dto) {
        DeviceSuspensionDto created = service.create(dto);
        return ResponseEntity
                .created(URI.create("/api/suspensions/" + created.getId()))
                .body(created);
    }

    /**
     * Replaces an existing suspension window.
     * Triggers a full reschedule so the changes take effect immediately.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<DeviceSuspensionDto> update(
            @PathVariable Long id,
            @Valid @RequestBody DeviceSuspensionDto dto) {

        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.update(id, dto));
    }

    /**
     * Deletes a suspension window.
     * Triggers a full reschedule so previously suppressed feedings can resume.
     *
     * NOTE: Spring Data JPA 3.x makes deleteById() idempotent — it silently
     * does nothing when the ID doesn't exist instead of throwing.
     * We therefore check existence explicitly and return 404 so the caller
     * always knows whether the deletion actually happened.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
