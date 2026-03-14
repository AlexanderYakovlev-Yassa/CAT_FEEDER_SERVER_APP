package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.AuthorityDto;

import java.util.List;
import java.util.Optional;

public interface AuthorityService {
    List<AuthorityDto> findAll();
    Optional<AuthorityDto> findById(Long id);
    AuthorityDto create(AuthorityDto dto);
    AuthorityDto update(Long id, AuthorityDto dto);
    void delete(Long id);
}

