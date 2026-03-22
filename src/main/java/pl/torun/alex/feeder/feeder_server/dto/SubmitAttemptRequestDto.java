package pl.torun.alex.feeder.feeder_server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAttemptRequestDto {

    @NotNull(message = "Session ID must not be null")
    private Long sessionId;

    @NotNull(message = "Attempt ID must not be null")
    private Long attemptId;

    @NotNull(message = "Measured grams must not be null")
    @Min(value = 0, message = "Measured grams must be non-negative")
    private Float measuredGrams;
}

