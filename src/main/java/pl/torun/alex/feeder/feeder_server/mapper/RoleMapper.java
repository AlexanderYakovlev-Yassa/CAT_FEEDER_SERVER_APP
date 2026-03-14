package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import pl.torun.alex.feeder.feeder_server.dto.RoleDto;
import pl.torun.alex.feeder.feeder_server.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role entity);
    Role toEntity(RoleDto dto);
}

