package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CameraDto {
    private Long id;
    private String name;
    private String rtspUrl;
    private String storagePath;
    private boolean autoStart;
    private Set<Long> userIds = new HashSet<>();
}

