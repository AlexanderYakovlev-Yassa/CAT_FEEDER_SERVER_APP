package pl.torun.alex.feeder.feeder_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import pl.torun.alex.feeder.feeder_server.config.TelegramProperties;
import pl.torun.alex.feeder.feeder_server.mqtt.MqttProperties;

@SpringBootApplication
@EnableConfigurationProperties({MqttProperties.class, TelegramProperties.class})
public class FeederServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeederServerApplication.class, args);
	}

}
