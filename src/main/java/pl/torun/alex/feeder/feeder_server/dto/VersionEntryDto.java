package pl.torun.alex.feeder.feeder_server.dto;

import lombok.Data;

import java.util.List;

@Data
public class VersionEntryDto {
    private String version;
    private String releaseDate;
    private List<String> changes;
}
