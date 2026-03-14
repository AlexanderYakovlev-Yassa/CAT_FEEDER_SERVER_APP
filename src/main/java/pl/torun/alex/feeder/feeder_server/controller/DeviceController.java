package pl.torun.alex.feeder.feeder_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.DeviceDto;
import pl.torun.alex.feeder.feeder_server.service.DeviceService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('read-feeders')")
    public List<DeviceDto> list(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return service.findByUserId(userId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('read-feeders')")
    public ResponseEntity<DeviceDto> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<DeviceDto> create(@RequestBody DeviceDto device) {
        DeviceDto created = service.create(device);
        return ResponseEntity.created(URI.create("/api/devices/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<DeviceDto> update(@PathVariable Long id, @RequestBody DeviceDto device) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DeviceDto updated = service.update(id, device);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-feeders')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

