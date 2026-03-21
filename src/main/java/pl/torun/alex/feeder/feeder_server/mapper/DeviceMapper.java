package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pl.torun.alex.feeder.feeder_server.dto.DeviceDto;
import pl.torun.alex.feeder.feeder_server.entity.Device;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(source = "users", target = "userIds", qualifiedByName = "usersToIds")
    DeviceDto toDto(Device entity);

    // Ignore users when converting from DTO to entity — service manages associations
    @Mapping(target = "users", ignore = true)
    Device toEntity(DeviceDto dto);

    @Named("usersToIds")
    default Set<Long> usersToIds(Set<pl.torun.alex.feeder.feeder_server.entity.AppUser> users) {
        if (users == null) return null;
        return users.stream().map(pl.torun.alex.feeder.feeder_server.entity.AppUser::getId).collect(Collectors.toSet());
    }
}
