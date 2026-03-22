package pl.torun.alex.feeder.feeder_server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartCalibrationRequestDto {

    @NotBlank(message = "Device serial number must not be blank")
    private String serialNumber;
}

