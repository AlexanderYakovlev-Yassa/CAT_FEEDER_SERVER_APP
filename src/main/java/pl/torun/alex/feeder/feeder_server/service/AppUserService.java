package pl.torun.alex.feeder.feeder_server.service;

import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.AppUserDto;
import pl.torun.alex.feeder.feeder_server.repository.AppUserRepository;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;
import pl.torun.alex.feeder.feeder_server.mapper.AppUserMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppUserService {

    private final AppUserRepository repo;
    private final AppUserMapper mapper;

    public AppUserService(AppUserRepository repo, AppUserMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<AppUserDto> findAll() {
        return repo.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public Optional<AppUserDto> findById(Long id) {
        return repo.findById(id).map(mapper::toDto);
    }

    public AppUserDto create(AppUserDto appUserDto) {
        AppUser saved = repo.save(mapper.toEntity(appUserDto));
        return mapper.toDto(saved);
    }

    public AppUserDto update(Long id, AppUserDto appUserDto) {
        AppUser entity = mapper.toEntity(appUserDto);
        entity.setId(id);
        AppUser saved = repo.save(entity);
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
