package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioPresetDto {
    private String id;
    private String name;
    private String description;
    private String icon;
    private Integer durationSeconds;
    private boolean custom;
}
