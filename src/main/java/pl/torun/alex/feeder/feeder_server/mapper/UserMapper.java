package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import pl.torun.alex.feeder.feeder_server.dto.UserDto;
import pl.torun.alex.feeder.feeder_server.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User entity);

    User toEntity(UserDto dto);
}

