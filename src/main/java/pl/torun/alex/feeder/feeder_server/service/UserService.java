package pl.torun.alex.feeder.feeder_server.service;

import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.repository.UserRepository;
import pl.torun.alex.feeder.feeder_server.entity.User;
import pl.torun.alex.feeder.feeder_server.dto.UserDto;
import pl.torun.alex.feeder.feeder_server.mapper.UserMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository repo;
    private final UserMapper mapper;

    public UserService(UserRepository repo, UserMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<UserDto> findAll() {
        return repo.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public Optional<UserDto> findById(Long id) {
        return repo.findById(id).map(mapper::toDto);
    }

    public UserDto create(UserDto userDto) {
        User saved = repo.save(mapper.toEntity(userDto));
        return mapper.toDto(saved);
    }

    public UserDto update(Long id, UserDto userDto) {
        User entity = mapper.toEntity(userDto);
        entity.setId(id);
        User saved = repo.save(entity);
        return mapper.toDto(saved);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
