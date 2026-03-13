package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.AppUserDto;
import pl.torun.alex.feeder.feeder_server.repository.AppUserRepository;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;
import pl.torun.alex.feeder.feeder_server.mapper.AppUserMapper;
import pl.torun.alex.feeder.feeder_server.service.AppUserService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository repo;
    private final AppUserMapper mapper;

    @Override
    public List<AppUserDto> findAll() {
        return repo.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<AppUserDto> findById(Long id) {
        return repo.findById(id).map(mapper::toDto);
    }

    @Override
    public AppUserDto create(AppUserDto appUserDto) {
        AppUser saved = repo.save(mapper.toEntity(appUserDto));
        return mapper.toDto(saved);
    }

    @Override
    public AppUserDto update(Long id, AppUserDto appUserDto) {
        AppUser entity = mapper.toEntity(appUserDto);
        entity.setId(id);
        AppUser saved = repo.save(entity);
        return mapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
