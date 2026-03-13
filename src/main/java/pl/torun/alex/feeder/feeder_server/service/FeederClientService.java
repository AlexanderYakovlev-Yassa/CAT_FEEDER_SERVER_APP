package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.entity.Device;

public interface FeederClientService {

    void sendFeedingCommand(Device device, Integer amountInGrams);
    void sendScheduleBeenInitCommand(Device device);
}
