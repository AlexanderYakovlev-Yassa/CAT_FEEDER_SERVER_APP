package pl.torun.alex.feeder.feeder_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "speaker")
public class SpeakerProperties {

    private boolean enabled = true;
    private String host = "192.168.1.1";
    private int port = 5000;
    private int timeoutMs = 5000;
    private String ffmpegPath = "ffmpeg";
}
