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
public class DeviceDto {
    private Long id;
    private String name;
    private String serialNumber;
    private Set<Long> userIds = new HashSet<>();
    private Float feedConsumption;
}
