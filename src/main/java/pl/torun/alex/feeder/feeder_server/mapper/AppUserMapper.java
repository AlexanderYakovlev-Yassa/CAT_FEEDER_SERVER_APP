package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pl.torun.alex.feeder.feeder_server.dto.AppUserDto;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AppUserMapper {

    @Mapping(source = "devices", target = "deviceIds", qualifiedByName = "devicesToIds")
    AppUserDto toDto(AppUser entity);

    @Mapping(target = "devices", ignore = true)
    AppUser toEntity(AppUserDto dto);

    @Named("devicesToIds")
    default Set<Long> devicesToIds(Set<pl.torun.alex.feeder.feeder_server.entity.Device> devices) {
        if (devices == null) return null;
        return devices.stream().map(pl.torun.alex.feeder.feeder_server.entity.Device::getId).collect(Collectors.toSet());
    }
}
