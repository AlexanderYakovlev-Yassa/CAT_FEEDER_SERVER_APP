package pl.torun.alex.feeder.feeder_server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private boolean enabled = false;
    private String botToken = "";
    private List<String> chatIds = new ArrayList<>();
    private String apiBaseUrl = "https://api.telegram.org";
}
