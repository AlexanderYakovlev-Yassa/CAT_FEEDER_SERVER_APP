package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.RoleAuthorityDto;
import pl.torun.alex.feeder.feeder_server.entity.RoleAuthority;
import pl.torun.alex.feeder.feeder_server.entity.RoleAuthorityId;
import pl.torun.alex.feeder.feeder_server.mapper.RoleAuthorityMapper;
import pl.torun.alex.feeder.feeder_server.repository.RoleAuthorityRepository;
import pl.torun.alex.feeder.feeder_server.service.RoleAuthorityService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleAuthorityServiceImpl implements RoleAuthorityService {

    private final RoleAuthorityRepository repo;
    private final RoleAuthorityMapper mapper;

    @Override
    public List<RoleAuthorityDto> findAll() {
        return repo.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<RoleAuthorityDto> findByRoleId(Long roleId) {
        return repo.findByIdRoleId(roleId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<RoleAuthorityDto> findByAuthorityId(Long authorityId) {
        return repo.findByIdAuthorityId(authorityId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public RoleAuthorityDto create(RoleAuthorityDto dto) {
        RoleAuthority entity = mapper.toEntity(dto);
        return mapper.toDto(repo.save(entity));
    }

    @Override
    public void delete(Long roleId, Long authorityId) {
        repo.deleteById(new RoleAuthorityId(roleId, authorityId));
    }
}

