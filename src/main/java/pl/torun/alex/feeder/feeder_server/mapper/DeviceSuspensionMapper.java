package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pl.torun.alex.feeder.feeder_server.dto.DeviceSuspensionDto;
import pl.torun.alex.feeder.feeder_server.entity.DeviceSuspension;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface DeviceSuspensionMapper {

    /**
     * Entity → DTO.
     * Converts each Instant field to a Unix epoch-second long so the JSON
     * payload is timezone-neutral for all clients.
     */
    @Mapping(source = "device.id",        target = "deviceId")
    @Mapping(source = "startSuspension",  target = "startSuspension", qualifiedByName = "instantToEpoch")
    @Mapping(source = "endSuspension",    target = "endSuspension",   qualifiedByName = "instantToEpoch")
    DeviceSuspensionDto toDto(DeviceSuspension entity);

    /**
     * DTO → Entity.
     * Rebuilds the Device proxy from the flat deviceId and converts the epoch-second
     * longs back to Instant so JPA can persist them as TIMESTAMP WITH TIME ZONE.
     */
    @Mapping(source = "deviceId",        target = "device.id")
    @Mapping(source = "startSuspension", target = "startSuspension", qualifiedByName = "epochToInstant")
    @Mapping(source = "endSuspension",   target = "endSuspension",   qualifiedByName = "epochToInstant")
    DeviceSuspension toEntity(DeviceSuspensionDto dto);

    /** Converts an Instant to UTC epoch seconds. */
    @Named("instantToEpoch")
    default Long instantToEpoch(Instant instant) {
        return instant != null ? instant.getEpochSecond() : null;
    }

    /** Converts UTC epoch seconds back to an Instant. */
    @Named("epochToInstant")
    default Instant epochToInstant(Long epochSecond) {
        return epochSecond != null ? Instant.ofEpochSecond(epochSecond) : null;
    }
}
