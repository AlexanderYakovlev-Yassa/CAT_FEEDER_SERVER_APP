package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.torun.alex.feeder.feeder_server.dto.RoleAuthorityDto;
import pl.torun.alex.feeder.feeder_server.entity.RoleAuthority;

@Mapper(componentModel = "spring")
public interface RoleAuthorityMapper {

    @Mapping(source = "id.roleId", target = "roleId")
    @Mapping(source = "id.authorityId", target = "authorityId")
    RoleAuthorityDto toDto(RoleAuthority entity);

    @Mapping(source = "roleId", target = "id.roleId")
    @Mapping(source = "authorityId", target = "id.authorityId")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "authority", ignore = true)
    RoleAuthority toEntity(RoleAuthorityDto dto);
}

