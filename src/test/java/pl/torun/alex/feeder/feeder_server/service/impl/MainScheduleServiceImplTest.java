package pl.torun.alex.feeder.feeder_server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.entity.FeedingMetadata;
import pl.torun.alex.feeder.feeder_server.repository.DailySchedulerRepository;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;
import pl.torun.alex.feeder.feeder_server.repository.DeviceSuspensionRepository;
import pl.torun.alex.feeder.feeder_server.service.FeederClientService;

import java.time.Instant;
import java.time.LocalTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainScheduleServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DailySchedulerRepository dailySchedulerRepository;

    @Mock
    private FeederClientService feederClientService;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private DeviceSuspensionRepository deviceSuspensionRepository;

    @InjectMocks
    private MainScheduleServiceImpl mainScheduleService;

    private Device device;
    private FeedingMetadata feedingMetadata;
    private Instant scheduledTime;

    @BeforeEach
    void setUp() {
        device = Device.builder()
                .id(1L)
                .name("Test Feeder")
                .serialNumber("FEED001")
                .feedConsumption(5.0f)
                .build();

        feedingMetadata = FeedingMetadata.builder()
                .feedingTime(LocalTime.of(12, 0))
                .amountInGrams(50)
                .build();

        scheduledTime = Instant.parse("2026-05-24T12:00:00Z");
    }

    @Test
    void executeFeedingTask_whenNoSuspension_shouldSendFeedingCommand() {
        // Arrange
        when(deviceSuspensionRepository.existsActiveSuspension(device.getId(), scheduledTime))
                .thenReturn(false);

        // Act
        mainScheduleService.executeFeedingTask(device, feedingMetadata, scheduledTime);

        // Assert
        verify(feederClientService, times(1)).sendFeedingCommand(device, 50);
        verify(deviceSuspensionRepository, times(1)).existsActiveSuspension(device.getId(), scheduledTime);
    }

    @Test
    void executeFeedingTask_whenActiveSuspensionAtScheduledTime_shouldSkipFeeding() {
        // Arrange
        when(deviceSuspensionRepository.existsActiveSuspension(device.getId(), scheduledTime))
                .thenReturn(true);

        // Act
        mainScheduleService.executeFeedingTask(device, feedingMetadata, scheduledTime);

        // Assert
        verify(feederClientService, never()).sendFeedingCommand(any(), anyInt());
        verify(deviceSuspensionRepository, times(1)).existsActiveSuspension(device.getId(), scheduledTime);
    }
}
