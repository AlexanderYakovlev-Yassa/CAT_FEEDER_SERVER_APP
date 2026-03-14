package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.RoleAuthorityDto;
import pl.torun.alex.feeder.feeder_server.service.RoleAuthorityService;

import java.util.List;

@RestController
@RequestMapping("/role-authorities")
@RequiredArgsConstructor
public class RoleAuthorityController {

    private final RoleAuthorityService service;

    @GetMapping
    @PreAuthorize("hasAuthority('read-users')")
    public List<RoleAuthorityDto> list() {
        return service.findAll();
    }

    @GetMapping("/by-role/{roleId}")
    @PreAuthorize("hasAuthority('read-users')")
    public List<RoleAuthorityDto> listByRole(@PathVariable Long roleId) {
        return service.findByRoleId(roleId);
    }

    @GetMapping("/by-authority/{authorityId}")
    @PreAuthorize("hasAuthority('read-users')")
    public List<RoleAuthorityDto> listByAuthority(@PathVariable Long authorityId) {
        return service.findByAuthorityId(authorityId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('manage-users')")
    public ResponseEntity<RoleAuthorityDto> create(@RequestBody RoleAuthorityDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @DeleteMapping("/{roleId}/{authorityId}")
    @PreAuthorize("hasAuthority('manage-users')")
    public ResponseEntity<Void> delete(@PathVariable Long roleId, @PathVariable Long authorityId) {
        service.delete(roleId, authorityId);
        return ResponseEntity.noContent().build();
    }
}

