package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.torun.alex.feeder.feeder_server.dto.DailySchedulerDto;
import pl.torun.alex.feeder.feeder_server.dto.FeedingMetadataDto;
import pl.torun.alex.feeder.feeder_server.entity.DailyScheduler;
import pl.torun.alex.feeder.feeder_server.entity.FeedingMetadata;

@Mapper(componentModel = "spring")
public interface DailySchedulerMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "device.id", target = "deviceId")
    DailySchedulerDto toDto(DailyScheduler entity);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "deviceId", target = "device.id")
    DailyScheduler toEntity(DailySchedulerDto dto);

    FeedingMetadataDto toFeedingMetadataDto(FeedingMetadata entity);

    FeedingMetadata toFeedingMetadata(FeedingMetadataDto dto);
}

