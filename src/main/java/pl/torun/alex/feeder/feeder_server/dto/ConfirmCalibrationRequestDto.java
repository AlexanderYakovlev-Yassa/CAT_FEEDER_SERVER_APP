package pl.torun.alex.feeder.feeder_server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmCalibrationRequestDto {

    @NotNull(message = "Session ID must not be null")
    private Long sessionId;

    @NotNull(message = "Accept flag must not be null")
    private Boolean accept;
}

