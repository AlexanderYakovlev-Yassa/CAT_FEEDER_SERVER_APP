package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.AppUserDto;

import java.util.List;
import java.util.Optional;

public interface AppUserService {

    List<AppUserDto> findAll();
    Optional<AppUserDto> findById(Long id);
    AppUserDto create(AppUserDto appUserDto);
    AppUserDto update(Long id, AppUserDto appUserDto);
    void delete(Long id);
}
