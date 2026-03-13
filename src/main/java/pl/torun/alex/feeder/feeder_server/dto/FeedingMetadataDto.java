package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedingMetadataDto {
    private Integer amountInGrams;
    private LocalTime feedingTime;
}

