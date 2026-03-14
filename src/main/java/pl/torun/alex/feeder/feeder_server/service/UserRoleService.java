package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.UserRoleDto;

import java.util.List;

public interface UserRoleService {
    List<UserRoleDto> findAll();
    List<UserRoleDto> findByUserId(Long userId);
    List<UserRoleDto> findByRoleId(Long roleId);
    UserRoleDto create(UserRoleDto dto);
    void delete(Long userId, Long roleId);
}

