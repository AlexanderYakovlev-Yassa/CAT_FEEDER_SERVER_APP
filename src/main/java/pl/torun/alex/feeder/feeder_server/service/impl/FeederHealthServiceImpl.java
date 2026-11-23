package pl.torun.alex.feeder.feeder_server.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.dto.FeederStatusDto;
import pl.torun.alex.feeder.feeder_server.entity.Device;
import pl.torun.alex.feeder.feeder_server.mqtt.MqttService;
import pl.torun.alex.feeder.feeder_server.repository.DeviceRepository;
import pl.torun.alex.feeder.feeder_server.service.FeederHealthService;
import pl.torun.alex.feeder.feeder_server.service.TelegramNotificationService;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FeederHealthServiceImpl implements FeederHealthService {

    static final String HEARTBEAT_TOPIC_PATTERN = "catfeeder/+/heartbeat";
    static final String STATUS_TOPIC_PATTERN = "catfeeder/+/status";
    static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(150);

    private final Optional<MqttService> mqttService;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;
    private final Optional<TelegramNotificationService> telegramNotificationService;
    private final Clock clock;
    private final ConcurrentHashMap<String, FeederStatusDto> statuses = new ConcurrentHashMap<>();

    @Autowired
    public FeederHealthServiceImpl(
            Optional<MqttService> mqttService,
            DeviceRepository deviceRepository,
            ObjectMapper objectMapper,
            Optional<TelegramNotificationService> telegramNotificationService) {
        this(mqttService, deviceRepository, objectMapper, telegramNotificationService, Clock.systemUTC());
    }

    FeederHealthServiceImpl(
            Optional<MqttService> mqttService,
            DeviceRepository deviceRepository,
            ObjectMapper objectMapper,
            Optional<TelegramNotificationService> telegramNotificationService,
            Clock clock) {
        this.mqttService = mqttService;
        this.deviceRepository = deviceRepository;
        this.objectMapper = objectMapper;
        this.telegramNotificationService = telegramNotificationService;
        this.clock = clock;
    }

    @PostConstruct
    public void init() {
        mqttService.ifPresent(mqtt -> {
            mqtt.subscribe(HEARTBEAT_TOPIC_PATTERN, (topic, message) ->
                    handleHeartbeat(topic, new String(message.getPayload(), StandardCharsets.UTF_8)));
            mqtt.subscribe(STATUS_TOPIC_PATTERN, (topic, message) ->
                    handleStatus(topic, new String(message.getPayload(), StandardCharsets.UTF_8)));
            log.info("FeederHealthService subscribed to {} and {}", HEARTBEAT_TOPIC_PATTERN, STATUS_TOPIC_PATTERN);
        });
    }

    void handleHeartbeat(String topic, String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String deviceId = resolveDeviceId(topic, json);
            if (deviceId == null) {
                return;
            }

            Instant now = clock.instant();
            FeederStatusDto current = statuses.get(deviceId);
            boolean wasOffline = current == null || !current.isOnline();

            FeederStatusDto updated = FeederStatusDto.builder()
                    .deviceId(deviceId)
                    .online(true)
                    .status(json.path("status").asText("idle"))
                    .motorRunning(json.path("motor_running").asBoolean(false))
                    .rssi(json.hasNonNull("rssi") ? json.get("rssi").asInt() : null)
                    .freeHeap(json.hasNonNull("free_heap") ? json.get("free_heap").asLong() : null)
                    .uptimeMs(json.hasNonNull("uptime_ms") ? json.get("uptime_ms").asLong() : null)
                    .lastSeenAt(now)
                    .lastHeartbeatAt(now)
                    .build();

            statuses.put(deviceId, updated);
            if (wasOffline) {
                log.info("Feeder {} is ONLINE (rssi={} dBm, freeHeap={} bytes)",
                        deviceId, updated.getRssi(), updated.getFreeHeap());
                String detail = updated.getRssi() != null
                        ? String.format("WiFi RSSI: %d dBm", updated.getRssi())
                        : "Heartbeat received";
                telegramNotificationService.ifPresent(tg -> tg.notifyFeederStatusChanged(deviceId, true, detail));
            }
        } catch (Exception e) {
            log.warn("Failed to parse heartbeat payload on {}: {}", topic, e.getMessage());
        }
    }

    void handleStatus(String topic, String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            String deviceId = resolveDeviceId(topic, json);
            if (deviceId == null) {
                return;
            }

            Instant now = clock.instant();
            String statusText = json.path("status").asText("unknown");
            boolean isOffline = "offline".equalsIgnoreCase(statusText);
            FeederStatusDto existing = statuses.get(deviceId);

            if (isOffline) {
                boolean wasOnline = existing != null && existing.isOnline();
                FeederStatusDto updated = FeederStatusDto.builder()
                        .deviceId(deviceId)
                        .online(false)
                        .status("offline")
                        .motorRunning(false)
                        .rssi(existing != null ? existing.getRssi() : null)
                        .freeHeap(existing != null ? existing.getFreeHeap() : null)
                        .uptimeMs(existing != null ? existing.getUptimeMs() : null)
                        .lastSeenAt(existing != null ? existing.getLastSeenAt() : now)
                        .lastHeartbeatAt(existing != null ? existing.getLastHeartbeatAt() : null)
                        .build();
                statuses.put(deviceId, updated);
                if (wasOnline || existing == null) {
                    String reason = json.path("message").asText("LWT/status");
                    log.warn("Feeder {} went OFFLINE: {}", deviceId, reason);
                    telegramNotificationService.ifPresent(tg -> tg.notifyFeederStatusChanged(deviceId, false, reason));
                }
                return;
            }

            boolean wasOffline = existing == null || !existing.isOnline();
            FeederStatusDto updated = FeederStatusDto.builder()
                    .deviceId(deviceId)
                    .online(true)
                    .status(statusText)
                    .motorRunning(json.path("motor_running").asBoolean(false))
                    .rssi(existing != null ? existing.getRssi() : null)
                    .freeHeap(existing != null ? existing.getFreeHeap() : null)
                    .uptimeMs(existing != null ? existing.getUptimeMs() : null)
                    .lastSeenAt(now)
                    .lastHeartbeatAt(existing != null ? existing.getLastHeartbeatAt() : null)
                    .build();
            statuses.put(deviceId, updated);
            if (wasOffline) {
                log.info("Feeder {} is ONLINE (status={})", deviceId, statusText);
                telegramNotificationService.ifPresent(
                        tg -> tg.notifyFeederStatusChanged(deviceId, true, "Статус: " + statusText));
            }
        } catch (Exception e) {
            log.warn("Failed to parse status payload on {}: {}", topic, e.getMessage());
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void checkStaleFeeders() {
        Instant cutoff = clock.instant().minus(HEARTBEAT_TIMEOUT);
        statuses.forEach((deviceId, dto) -> {
            if (dto.isOnline() && dto.getLastSeenAt() != null && dto.getLastSeenAt().isBefore(cutoff)) {
                dto.setOnline(false);
                dto.setStatus("offline");
                dto.setMotorRunning(false);
                log.warn("Feeder {} timed out (no heartbeat or status since {})", deviceId, dto.getLastSeenAt());
                telegramNotificationService.ifPresent(
                        tg -> tg.notifyFeederStatusChanged(deviceId, false, "Таймаут хартбита (>150 с)"));
            }
        });
    }

    @Override
    public Map<String, FeederStatusDto> getAllStatuses() {
        Map<String, FeederStatusDto> result = new LinkedHashMap<>();
        for (Device device : deviceRepository.findAll()) {
            String serial = device.getSerialNumber();
            result.put(serial, statuses.getOrDefault(serial, unknownStatus(serial)));
        }
        // Include any feeder reporting over MQTT that is not yet registered in DB
        statuses.forEach(result::putIfAbsent);
        return result;
    }

    @Override
    public Optional<FeederStatusDto> getStatus(String serialNumber) {
        if (serialNumber == null || serialNumber.isBlank()) {
            return Optional.empty();
        }
        FeederStatusDto dto = statuses.get(serialNumber);
        if (dto != null) {
            return Optional.of(dto);
        }
        return deviceRepository.findBySerialNumber(serialNumber)
                .map(d -> unknownStatus(d.getSerialNumber()));
    }

    private FeederStatusDto unknownStatus(String serialNumber) {
        return FeederStatusDto.builder()
                .deviceId(serialNumber)
                .online(false)
                .status("unknown")
                .motorRunning(false)
                .build();
    }

    private String resolveDeviceId(String topic, JsonNode json) {
        if (json.hasNonNull("device_id")) {
            return json.get("device_id").asText();
        }
        // Fallback: extract {device_id} from "catfeeder/{device_id}/..."
        if (topic != null) {
            String[] parts = topic.split("/");
            if (parts.length >= 2 && !parts[1].isBlank()) {
                return parts[1];
            }
        }
        return null;
    }
}
