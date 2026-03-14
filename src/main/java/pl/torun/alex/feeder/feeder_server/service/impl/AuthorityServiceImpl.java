package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.AuthorityDto;
import pl.torun.alex.feeder.feeder_server.entity.Authority;
import pl.torun.alex.feeder.feeder_server.mapper.AuthorityMapper;
import pl.torun.alex.feeder.feeder_server.repository.AuthorityRepository;
import pl.torun.alex.feeder.feeder_server.service.AuthorityService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorityServiceImpl implements AuthorityService {

    private final AuthorityRepository repo;
    private final AuthorityMapper mapper;

    @Override
    public List<AuthorityDto> findAll() {
        return repo.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<AuthorityDto> findById(Long id) {
        return repo.findById(id).map(mapper::toDto);
    }

    @Override
    public AuthorityDto create(AuthorityDto dto) {
        Authority saved = repo.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Override
    public AuthorityDto update(Long id, AuthorityDto dto) {
        Authority entity = mapper.toEntity(dto);
        entity.setId(id);
        return mapper.toDto(repo.save(entity));
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}

