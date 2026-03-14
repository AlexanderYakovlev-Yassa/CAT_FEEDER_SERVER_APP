package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.RoleDto;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    List<RoleDto> findAll();
    Optional<RoleDto> findById(Long id);
    RoleDto create(RoleDto dto);
    RoleDto update(Long id, RoleDto dto);
    void delete(Long id);
}

