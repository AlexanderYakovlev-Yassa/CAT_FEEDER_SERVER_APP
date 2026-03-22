package pl.torun.alex.feeder.feeder_server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedRequestDto {

    @NotBlank(message = "Device serial number must not be blank")
    private String serialNumber;

    @NotNull(message = "Amount in grams must not be null")
    @Min(value = 1, message = "Amount in grams must be at least 1")
    private Integer amountInGrams;
}

