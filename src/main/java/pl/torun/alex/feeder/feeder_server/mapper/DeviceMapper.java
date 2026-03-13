package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.torun.alex.feeder.feeder_server.dto.DeviceDto;
import pl.torun.alex.feeder.feeder_server.entity.Device;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(source = "user.id", target = "userId")
    DeviceDto toDto(Device entity);

    @Mapping(source = "userId", target = "user.id")
    Device toEntity(DeviceDto dto);
}

