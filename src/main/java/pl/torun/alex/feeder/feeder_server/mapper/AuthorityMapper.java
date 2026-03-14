package pl.torun.alex.feeder.feeder_server.mapper;

import org.mapstruct.Mapper;
import pl.torun.alex.feeder.feeder_server.dto.AuthorityDto;
import pl.torun.alex.feeder.feeder_server.entity.Authority;

@Mapper(componentModel = "spring")
public interface AuthorityMapper {
    AuthorityDto toDto(Authority entity);
    Authority toEntity(AuthorityDto dto);
}

