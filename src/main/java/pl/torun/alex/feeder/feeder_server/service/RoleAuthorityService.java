package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.RoleAuthorityDto;

import java.util.List;

public interface RoleAuthorityService {
    List<RoleAuthorityDto> findAll();
    List<RoleAuthorityDto> findByRoleId(Long roleId);
    List<RoleAuthorityDto> findByAuthorityId(Long authorityId);
    RoleAuthorityDto create(RoleAuthorityDto dto);
    void delete(Long roleId, Long authorityId);
}

