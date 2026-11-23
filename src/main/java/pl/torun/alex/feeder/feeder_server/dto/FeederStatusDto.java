package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Live health and connectivity snapshot for an ESP32 feeder device.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeederStatusDto {
    private String deviceId;
    private boolean online;
    private String status;
    private boolean motorRunning;
    private Integer rssi;
    private Long freeHeap;
    private Long uptimeMs;
    private Instant lastSeenAt;
    private Instant lastHeartbeatAt;
}
