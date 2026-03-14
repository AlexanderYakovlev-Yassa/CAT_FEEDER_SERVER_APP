package pl.torun.alex.feeder.feeder_server.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true")
public class MqttService implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    private final MqttProperties properties;
    private MqttClient client;

    public MqttService(MqttProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        try {
            logger.info("Starting MQTT client connecting to {}", properties.getBroker());

            // Check for custom persistence directory from environment variable
            String persistenceDir = System.getenv("MQTT_PERSISTENCE_DIR");

            if (persistenceDir != null && !persistenceDir.isBlank()) {
                // Use file persistence with custom directory
                logger.info("Using file persistence at directory: {}", persistenceDir);
                MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistenceDir);
                client = new MqttClient(properties.getBroker(), properties.getClientId(), persistence);
            } else {
                // Fallback to memory persistence if directory not set
                logger.info("Using memory persistence (no MQTT_PERSISTENCE_DIR set)");
                client = new MqttClient(properties.getBroker(), properties.getClientId(), new MemoryPersistence());
            }

            MqttConnectOptions opts = new MqttConnectOptions();
            if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
                opts.setUserName(properties.getUsername());
            }
            if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
                opts.setPassword(properties.getPassword().toCharArray());
            }
            opts.setAutomaticReconnect(true);
            // cleanSession must be false when using automaticReconnect,
            // otherwise the broker destroys the session on disconnect causing EOFException
            opts.setCleanSession(false);
            opts.setKeepAliveInterval(60);       // send PINGREQ every 60s to keep connection alive
            opts.setConnectionTimeout(30);       // wait up to 30s for broker to respond
            opts.setMaxReconnectDelay(30_000);   // cap reconnect back-off at 30s
            client.setCallback(this);
            client.connect(opts);
            logger.info("MQTT client connected with clientId={}", properties.getClientId());
        } catch (MqttException e) {
            logger.error("Failed to start MQTT client: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
                logger.info("MQTT client disconnected");
            } catch (MqttException e) {
                logger.warn("Error while disconnecting MQTT client: {}", e.getMessage(), e);
            }
        }
    }

    public void publish(String topic, String payload, int qos, boolean retained) {
        if (client == null || !client.isConnected()) {
            logger.warn("MQTT client not connected - cannot publish");
            return;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);
            client.publish(topic, message);
            logger.debug("Published MQTT message to {}: {}", topic, payload);
        } catch (MqttException e) {
            logger.error("Failed to publish MQTT message: {}", e.getMessage(), e);
        }
    }

    public void subscribe(String topic, IMqttMessageListener listener) {
        if (client == null || !client.isConnected()) {
            logger.warn("MQTT client not connected - cannot subscribe");
            return;
        }
        try {
            client.subscribe(topic, listener);
            logger.info("Subscribed to MQTT topic {}", topic);
        } catch (MqttException e) {
            logger.error("Failed to subscribe to topic {}: {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("MQTT connection lost - automatic reconnect will attempt to restore it: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.info("MQTT message arrived on {}: {}", topic, new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // no-op
    }
}
