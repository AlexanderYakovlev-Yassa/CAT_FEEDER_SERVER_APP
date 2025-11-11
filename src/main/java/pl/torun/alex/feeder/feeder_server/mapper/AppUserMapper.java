package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import pl.torun.alex.feeder.feeder_server.dto.AppUserDto;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;

@Mapper(componentModel = "spring")
public interface AppUserMapper {

    AppUserDto toDto(AppUser entity);

    AppUser toEntity(AppUserDto dto);
}
