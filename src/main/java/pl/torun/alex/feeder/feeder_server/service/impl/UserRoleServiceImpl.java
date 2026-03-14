package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.UserRoleDto;
import pl.torun.alex.feeder.feeder_server.entity.UserRole;
import pl.torun.alex.feeder.feeder_server.entity.UserRoleId;
import pl.torun.alex.feeder.feeder_server.mapper.UserRoleMapper;
import pl.torun.alex.feeder.feeder_server.repository.UserRoleRepository;
import pl.torun.alex.feeder.feeder_server.service.UserRoleService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository repo;
    private final UserRoleMapper mapper;

    @Override
    public List<UserRoleDto> findAll() {
        return repo.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<UserRoleDto> findByUserId(Long userId) {
        return repo.findByIdUserId(userId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<UserRoleDto> findByRoleId(Long roleId) {
        return repo.findByIdRoleId(roleId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public UserRoleDto create(UserRoleDto dto) {
        UserRole entity = mapper.toEntity(dto);
        return mapper.toDto(repo.save(entity));
    }

    @Override
    public void delete(Long userId, Long roleId) {
        repo.deleteById(new UserRoleId(userId, roleId));
    }
}

