package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.AuthorityDto;
import pl.torun.alex.feeder.feeder_server.service.AuthorityService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/authorities")
@RequiredArgsConstructor
public class AuthorityController {

    private final AuthorityService service;

    @GetMapping
    @PreAuthorize("hasAuthority('read-users')")
    public List<AuthorityDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('read-users')")
    public ResponseEntity<AuthorityDto> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('manage-users')")
    public ResponseEntity<AuthorityDto> create(@RequestBody AuthorityDto dto) {
        AuthorityDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/authorities/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-users')")
    public ResponseEntity<AuthorityDto> update(@PathVariable Long id, @RequestBody AuthorityDto dto) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage-users')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

