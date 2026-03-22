package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.entity.Device;

public interface FeederClientService {

    void sendFeedingCommand(Device device, Integer amountInGrams);
    void sendScheduleBeenInitCommand(Device device);
    /**
     * Sends a timed calibration burst to the feeder.
     *
     * @param device        the device to calibrate
     * @param durationMs    how long the motor should run (milliseconds)
     */
    void sendCalibrationCommand(Device device, int durationMs);
}
