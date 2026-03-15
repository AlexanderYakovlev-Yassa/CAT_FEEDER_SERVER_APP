package pl.torun.alex.feeder.feeder_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.DailySchedulerDto;
import pl.torun.alex.feeder.feeder_server.service.DailySchedulerService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/schedulers")
public class DailySchedulerController {

    private final DailySchedulerService service;

    public DailySchedulerController(DailySchedulerService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('read-schedule')")
    public List<DailySchedulerDto> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Boolean activeOnly) {
        if (userId != null) {
            return service.findByUserId(userId);
        }
        if (deviceId != null) {
            return service.findByDeviceId(deviceId);
        }
        if (Boolean.TRUE.equals(activeOnly)) {
            return service.findActive();
        }
        return service.findAll();
    }

    @GetMapping("/device/{deviceId}")
    @PreAuthorize("hasAuthority('read-schedule')")
    public List<DailySchedulerDto> getByDeviceId(@PathVariable Long deviceId) {
        return service.findByDeviceId(deviceId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('read-schedule')")
    public ResponseEntity<DailySchedulerDto> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<DailySchedulerDto> create(@RequestBody DailySchedulerDto scheduler) {
        DailySchedulerDto created = service.create(scheduler);
        return ResponseEntity.created(URI.create("/api/schedulers/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<DailySchedulerDto> update(@PathVariable Long id, @RequestBody DailySchedulerDto scheduler) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DailySchedulerDto updated = service.update(id, scheduler);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-schedule')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

