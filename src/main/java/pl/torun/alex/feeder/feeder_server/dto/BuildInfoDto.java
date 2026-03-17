package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BuildInfoDto {
    private String branch;
    private String commitId;
    private String commitTime;
}

