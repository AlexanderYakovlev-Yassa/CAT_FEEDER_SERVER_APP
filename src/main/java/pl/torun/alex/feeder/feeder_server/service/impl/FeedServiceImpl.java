package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.FeedRequestDto;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;
import pl.torun.alex.feeder.feeder_server.service.FeederClientService;
import pl.torun.alex.feeder.feeder_server.service.FeedService;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {

    private final DeviceRepository deviceRepository;
    private final FeederClientService feederClientService;

    @Override
    public void feed(FeedRequestDto request) {
        Device device = deviceRepository.findBySerialNumber(request.getSerialNumber())
                .orElseThrow(() -> new NoSuchElementException(
                        "Device not found with serial number: " + request.getSerialNumber()));

        log.info("Manual feed triggered for device '{}' - amount: {}g",
                device.getSerialNumber(), request.getAmountInGrams());

        feederClientService.sendFeedingCommand(device, request.getAmountInGrams());
    }
}

