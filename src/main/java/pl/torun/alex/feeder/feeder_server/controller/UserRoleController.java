package pl.torun.alex.feeder.feeder_server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.torun.alex.feeder.feeder_server.dto.UserRoleDto;
import pl.torun.alex.feeder.feeder_server.service.UserRoleService;

import java.util.List;

@RestController
@RequestMapping("/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService service;

    @GetMapping
    @PreAuthorize("hasAuthority('read-users')")
    public List<UserRoleDto> list() {
        return service.findAll();
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAuthority('read-users')")
    public List<UserRoleDto> listByUser(@PathVariable Long userId) {
        return service.findByUserId(userId);
    }

    @GetMapping("/by-role/{roleId}")
    @PreAuthorize("hasAuthority('read-users')")
    public List<UserRoleDto> listByRole(@PathVariable Long roleId) {
        return service.findByRoleId(roleId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('manage-users')")
    public ResponseEntity<UserRoleDto> create(@RequestBody UserRoleDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @DeleteMapping("/{userId}/{roleId}")
    @PreAuthorize("hasAuthority('manage-users')")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long roleId) {
        service.delete(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}

