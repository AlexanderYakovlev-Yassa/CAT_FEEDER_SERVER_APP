package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.AuthorityDto;
import pl.torun.alex.feeder.feeder_server.service.AuthorityService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/authorities")
@RequiredArgsConstructor
public class AuthorityController {

    private final AuthorityService service;

    @GetMapping
    public List<AuthorityDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorityDto> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AuthorityDto> create(@RequestBody AuthorityDto dto) {
        AuthorityDto created = service.create(dto);
        return ResponseEntity.created(URI.create("/api/authorities/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorityDto> update(@PathVariable Long id, @RequestBody AuthorityDto dto) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

