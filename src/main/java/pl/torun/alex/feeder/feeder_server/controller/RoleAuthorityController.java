package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.RoleAuthorityDto;
import pl.torun.alex.feeder.feeder_server.service.RoleAuthorityService;

import java.util.List;

@RestController
@RequestMapping("/api/role-authorities")
@RequiredArgsConstructor
public class RoleAuthorityController {

    private final RoleAuthorityService service;

    @GetMapping
    public List<RoleAuthorityDto> list() {
        return service.findAll();
    }

    @GetMapping("/by-role/{roleId}")
    public List<RoleAuthorityDto> listByRole(@PathVariable Long roleId) {
        return service.findByRoleId(roleId);
    }

    @GetMapping("/by-authority/{authorityId}")
    public List<RoleAuthorityDto> listByAuthority(@PathVariable Long authorityId) {
        return service.findByAuthorityId(authorityId);
    }

    @PostMapping
    public ResponseEntity<RoleAuthorityDto> create(@RequestBody RoleAuthorityDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @DeleteMapping("/{roleId}/{authorityId}")
    public ResponseEntity<Void> delete(@PathVariable Long roleId, @PathVariable Long authorityId) {
        service.delete(roleId, authorityId);
        return ResponseEntity.noContent().build();
    }
}

