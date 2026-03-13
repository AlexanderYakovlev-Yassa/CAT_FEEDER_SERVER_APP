package pl.torun.alex.feeder.feeder_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.AppUserDto;
import pl.torun.alex.feeder.feeder_server.service.AppUserService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserService service;

    public UserController(AppUserService service) {
        this.service = service;
    }

    @GetMapping
    public List<AppUserDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUserDto> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AppUserDto> create(@RequestBody AppUserDto user) {
        AppUserDto created = service.create(user);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppUserDto> update(@PathVariable Long id, @RequestBody AppUserDto user) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AppUserDto updated = service.update(id, user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
