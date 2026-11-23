package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.FeederStatusDto;

import java.util.Map;
import java.util.Optional;

/**
 * Tracks real-time feeder connectivity and health from MQTT heartbeats and LWT status updates.
 */
public interface FeederHealthService {

    /**
     * Returns the current health status for all known feeders keyed by serial number.
     */
    Map<String, FeederStatusDto> getAllStatuses();

    /**
     * Returns the current health status for a single feeder by its serial number.
     */
    Optional<FeederStatusDto> getStatus(String serialNumber);
}
