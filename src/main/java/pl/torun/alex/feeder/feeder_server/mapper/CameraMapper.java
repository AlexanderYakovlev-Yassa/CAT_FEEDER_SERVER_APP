package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pl.torun.alex.feeder.feeder_server.dto.CameraDto;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;
import pl.torun.alex.feeder.feeder_server.entity.Camera;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CameraMapper {

    @Mapping(source = "users", target = "userIds", qualifiedByName = "usersToIds")
    CameraDto toDto(Camera entity);

    // Ignore users when converting from DTO to entity — service manages associations
    @Mapping(target = "users", ignore = true)
    Camera toEntity(CameraDto dto);

    @Named("usersToIds")
    default Set<Long> usersToIds(Set<AppUser> users) {
        if (users == null) return null;
        return users.stream().map(AppUser::getId).collect(Collectors.toSet());
    }
}

