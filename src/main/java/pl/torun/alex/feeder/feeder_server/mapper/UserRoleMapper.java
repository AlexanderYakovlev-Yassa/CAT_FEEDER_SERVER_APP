package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.torun.alex.feeder.feeder_server.dto.UserRoleDto;
import pl.torun.alex.feeder.feeder_server.entity.UserRole;

@Mapper(componentModel = "spring")
public interface UserRoleMapper {

    @Mapping(source = "id.userId", target = "userId")
    @Mapping(source = "id.roleId", target = "roleId")
    UserRoleDto toDto(UserRole entity);

    @Mapping(source = "userId", target = "id.userId")
    @Mapping(source = "roleId", target = "id.roleId")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "role", ignore = true)
    UserRole toEntity(UserRoleDto dto);
}

