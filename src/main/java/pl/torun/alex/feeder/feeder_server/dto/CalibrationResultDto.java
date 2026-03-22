package pl.torun.alex.feeder.feeder_server.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationResultDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    private String deviceSerialNumber;
    /** Calculated average feed consumption in grams per second. */
    private Float calculatedFeedConsumption;
    /** Standard deviation across the 5 measurements (g/s). */
    private Float standardDeviation;
    /** Individual measurement values (grams per second) for each attempt. */
    private List<Float> measurements;
    /** Current feed consumption saved on the device (before accepting). */
    private Float currentFeedConsumption;
}
