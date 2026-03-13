package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedingMetadata {
    
    @Column(name = "amount_in_grams")
    private Integer amountInGrams;

    @Column(name = "feeding_time", nullable = false)
    private LocalTime feedingTime;
}
