package pl.torun.alex.feeder.feeder_server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for a device suspension window.
 *
 * Timestamps are expressed as Unix epoch seconds (UTC) so that clients
 * running in different timezones all refer to the exact same moment in time.
 *
 * Example JSON:
 * {
 *   "deviceId": 1,
 *   "startSuspension": 1744272000,   // 2026-04-10 08:00:00 UTC
 *   "endSuspension":   1744531200    // 2026-04-13 08:00:00 UTC
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceSuspensionDto {

    /** DB identifier; null when creating a new suspension. */
    private Long id;

    /** ID of the device to suspend. */
    @NotNull
    private Long deviceId;

    /**
     * Inclusive start of the suspension window as a Unix timestamp (epoch seconds, UTC).
     * Using a primitive long guarantees there is no timezone interpretation on either end.
     */
    @NotNull
    private Long startSuspension;

    /**
     * Inclusive end of the suspension window as a Unix timestamp (epoch seconds, UTC).
     */
    @NotNull
    private Long endSuspension;
}
