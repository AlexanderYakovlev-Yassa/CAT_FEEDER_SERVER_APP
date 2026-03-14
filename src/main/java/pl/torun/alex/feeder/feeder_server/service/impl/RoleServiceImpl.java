package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.RoleDto;
import pl.torun.alex.feeder.feeder_server.entity.Role;
import pl.torun.alex.feeder.feeder_server.mapper.RoleMapper;
import pl.torun.alex.feeder.feeder_server.repository.RoleRepository;
import pl.torun.alex.feeder.feeder_server.service.RoleService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository repo;
    private final RoleMapper mapper;

    @Override
    public List<RoleDto> findAll() {
        return repo.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<RoleDto> findById(Long id) {
        return repo.findById(id).map(mapper::toDto);
    }

    @Override
    public RoleDto create(RoleDto dto) {
        Role saved = repo.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Override
    public RoleDto update(Long id, RoleDto dto) {
        Role entity = mapper.toEntity(dto);
        entity.setId(id);
        return mapper.toDto(repo.save(entity));
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}

