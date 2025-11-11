package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySchedulerDto {
    private Long id;
    private String taskName;
    private Long userId;
    private Long deviceId;
    private List<LocalTime> scheduledTimes;
    private Boolean active;
}

