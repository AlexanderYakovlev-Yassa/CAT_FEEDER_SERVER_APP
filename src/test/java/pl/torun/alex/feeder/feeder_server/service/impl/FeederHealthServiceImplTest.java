package pl.torun.alex.feeder.feeder_server.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.torun.alex.feeder.feeder_server.dto.FeederStatusDto;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;
import pl.torun.alex.feeder.feeder_server.service.TelegramNotificationService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeederHealthServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    private AtomicReference<Instant> currentTime;
    private FeederHealthServiceImpl service;

    @BeforeEach
    void setUp() {
        currentTime = new AtomicReference<>(Instant.parse("2026-11-23T19:00:00Z"));
        Clock mutableClock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentTime.get();
            }
        };
        service = new FeederHealthServiceImpl(
                Optional.empty(), deviceRepository, new ObjectMapper(),
                Optional.of(telegramNotificationService), mutableClock);
    }

    @Test
    void heartbeatMarksDeviceOnlineWithTelemetry() {
        service.handleHeartbeat(
                "catfeeder/FEED001/heartbeat",
                "{\"device_id\":\"FEED001\",\"status\":\"idle\",\"motor_running\":false,\"rssi\":-50,\"free_heap\":249232,\"uptime_ms\":63683}");
        service.handleHeartbeat(
                "catfeeder/FEED001/heartbeat",
                "{\"device_id\":\"FEED001\",\"status\":\"idle\",\"motor_running\":false,\"rssi\":-52,\"free_heap\":249200,\"uptime_ms\":123683}");

        Optional<FeederStatusDto> status = service.getStatus("FEED001");
        assertTrue(status.isPresent());
        FeederStatusDto dto = status.get();
        assertTrue(dto.isOnline());
        assertEquals("idle", dto.getStatus());
        assertFalse(dto.isMotorRunning());
        assertEquals(-52, dto.getRssi());
        assertEquals(249200L, dto.getFreeHeap());
        assertEquals(123683L, dto.getUptimeMs());
        assertEquals(currentTime.get(), dto.getLastHeartbeatAt());

        verify(telegramNotificationService, times(1))
                .notifyFeederStatusChanged(eq("FEED001"), eq(true), anyString());
    }

    @Test
    void lwtOfflineMarksDeviceOffline() {
        service.handleHeartbeat(
                "catfeeder/FEED002/heartbeat",
                "{\"device_id\":\"FEED002\",\"status\":\"idle\",\"motor_running\":false,\"rssi\":-48,\"free_heap\":187924,\"uptime_ms\":41502}");

        service.handleStatus(
                "catfeeder/FEED002/status",
                "{\"device_id\":\"FEED002\",\"status\":\"offline\",\"message\":\"Unexpected disconnect (LWT)\",\"motor_running\":false}");

        FeederStatusDto dto = service.getStatus("FEED002").orElseThrow();
        assertFalse(dto.isOnline());
        assertEquals("offline", dto.getStatus());
        assertEquals(-48, dto.getRssi());
        verify(telegramNotificationService, times(1))
                .notifyFeederStatusChanged(eq("FEED002"), eq(false), eq("Unexpected disconnect (LWT)"));
    }

    @Test
    void staleHeartbeatTimesOutAfter150Seconds() {
        service.handleHeartbeat(
                "catfeeder/FEED001/heartbeat",
                "{\"device_id\":\"FEED001\",\"status\":\"idle\",\"motor_running\":false,\"rssi\":-50,\"free_heap\":249232,\"uptime_ms\":63683}");

        currentTime.set(currentTime.get().plusSeconds(151));
        service.checkStaleFeeders();

        FeederStatusDto dto = service.getStatus("FEED001").orElseThrow();
        assertFalse(dto.isOnline());
        assertEquals("offline", dto.getStatus());
        verify(telegramNotificationService, times(1))
                .notifyFeederStatusChanged(eq("FEED001"), eq(false), anyString());
    }

    @Test
    void getAllStatusesIncludesUnknownDevicesFromDatabase() {
        Device feed1 = Device.builder().id(1L).name("Main").serialNumber("FEED001").build();
        Device feed2 = Device.builder().id(2L).name("Kitchen").serialNumber("FEED002").build();
        when(deviceRepository.findAll()).thenReturn(List.of(feed1, feed2));

        service.handleHeartbeat(
                "catfeeder/FEED001/heartbeat",
                "{\"device_id\":\"FEED001\",\"status\":\"idle\",\"motor_running\":false,\"rssi\":-50,\"free_heap\":249232,\"uptime_ms\":63683}");

        Map<String, FeederStatusDto> all = service.getAllStatuses();
        assertEquals(2, all.size());
        assertTrue(all.get("FEED001").isOnline());
        assertFalse(all.get("FEED002").isOnline());
        assertEquals("unknown", all.get("FEED002").getStatus());
    }
}
