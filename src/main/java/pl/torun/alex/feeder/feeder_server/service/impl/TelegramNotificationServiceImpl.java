package pl.torun.alex.feeder.feeder_server.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.config.TelegramProperties;
import pl.torun.alex.feeder.feeder_server.service.TelegramNotificationService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class TelegramNotificationServiceImpl implements TelegramNotificationService {

    private final TelegramProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TelegramNotificationServiceImpl(TelegramProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void notifyFeederStatusChanged(String deviceId, boolean online, String detail) {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getBotToken() == null || properties.getBotToken().isBlank()) {
            log.warn("Telegram notifications enabled, but telegram.bot-token is empty");
            return;
        }
        if (properties.getChatIds() == null || properties.getChatIds().isEmpty()) {
            log.warn("Telegram notifications enabled, but telegram.chat-ids is empty");
            return;
        }

        String safeId = escapeHtml(deviceId);
        String safeDetail = escapeHtml(detail != null ? detail : "");
        String text = online
                ? String.format("🟢 <b>Кормушка %s в сети</b>%s", safeId,
                        safeDetail.isBlank() ? "" : "\n" + safeDetail)
                : String.format("🔴 <b>Кормушка %s не в сети</b>%s", safeId,
                        safeDetail.isBlank() ? "" : "\nПричина: " + safeDetail);

        String url = String.format("%s/bot%s/sendMessage", properties.getApiBaseUrl(), properties.getBotToken());

        for (String chatId : properties.getChatIds()) {
            if (chatId == null || chatId.isBlank()) {
                continue;
            }
            sendMessage(url, chatId.trim(), text, deviceId, online);
        }
    }

    private void sendMessage(String url, String chatId, String text, String deviceId, boolean online) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "HTML"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Sent Telegram feeder status notification ({} -> {}) to chat {}",
                        deviceId, online ? "ONLINE" : "OFFLINE", chatId);
            } else {
                log.warn("Telegram API returned HTTP {} for chat {}: {}",
                        response.statusCode(), chatId, response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram notification for feeder {} to chat {}: {}",
                    deviceId, chatId, e.getMessage());
        }
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
