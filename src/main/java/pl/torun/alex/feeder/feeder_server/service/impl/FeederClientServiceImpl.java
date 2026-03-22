package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.mqtt.MqttService;
import pl.torun.alex.feeder.feeder_server.service.FeederClientService;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeederClientServiceImpl implements FeederClientService {

    private final MqttService mqttService;

    @Override
    public void sendFeedingCommand(Device device, Integer amountInGrams) {
        String topic = "catfeeder/" + device.getSerialNumber() + "/command";
        // command: FEED:5000 (for 5 seconds)
        int durationMs = (int) (amountInGrams / device.getFeedConsumption() * 1000);
        String payload = String.format("FEED:%d", durationMs);

        log.info("Sending feeding command to device {} - amount: {}g", device.getSerialNumber(), amountInGrams);
        mqttService.publish(topic, payload, 1, false);
    }

    @Override
    public void sendScheduleBeenInitCommand(Device device) {
        String topic = "catfeeder/" + device.getSerialNumber() + "/command";
        String payload = "SCHEDULE_INIT";

        log.info("Sending schedule initialized notification to device {}", device.getSerialNumber());
        mqttService.publish(topic, payload, 1, false);
    }

    @Override
    public void sendCalibrationCommand(Device device, int durationMs) {
        String topic = "catfeeder/" + device.getSerialNumber() + "/command";
        String payload = String.format("FEED:%d", durationMs);

        log.info("Sending calibration command to device {} - duration: {}ms", device.getSerialNumber(), durationMs);
        mqttService.publish(topic, payload, 1, false);
    }
}
