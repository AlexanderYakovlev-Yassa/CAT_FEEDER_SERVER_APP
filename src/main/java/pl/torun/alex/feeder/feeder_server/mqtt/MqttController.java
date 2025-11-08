package pl.torun.alex.feeder.feeder_server.mqtt;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/mqtt")
public class MqttController {

    private final Optional<MqttService> mqttService;
    private final MqttProperties properties;

    public MqttController(Optional<MqttService> mqttService, MqttProperties properties) {
        this.mqttService = mqttService;
        this.properties = properties;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        if (mqttService.isPresent()) {
            return ResponseEntity.ok("MQTT enabled (bean present). Broker=" + properties.getBroker());
        }
        return ResponseEntity.ok("MQTT disabled (set mqtt.enabled=true to enable)");
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publish(
            @RequestParam(required = false) String topic,
            @RequestParam String payload,
            @RequestParam(defaultValue = "0") int qos,
            @RequestParam(defaultValue = "false") boolean retained
    ) {
        if (mqttService.isEmpty()) {
            return ResponseEntity.status(503).body("MQTT is disabled on the server");
        }
        String t = (topic == null || topic.isBlank()) ? properties.getDefaultTopic() : topic;
        mqttService.get().publish(t, payload, qos, retained);
        return ResponseEntity.ok("Published to " + t);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestParam String topic) {
        if (mqttService.isEmpty()) {
            return ResponseEntity.status(503).body("MQTT is disabled on the server");
        }
        mqttService.get().subscribe(topic, (t, message) -> {
            // simple in-memory listener that logs message (MqttService already logs arrivals)
        });
        return ResponseEntity.ok("Subscribed to " + topic);
    }
}

