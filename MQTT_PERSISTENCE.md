# MQTT Persistence Configuration

## Overview

The application now supports configurable MQTT persistence through the `MQTT_PERSISTENCE_DIR` environment variable. This allows the MQTT client to persist messages to disk for reliability, with automatic fallback to memory persistence if the directory is not specified.

## How It Works

### MqttService Implementation

The `MqttService` checks for the `MQTT_PERSISTENCE_DIR` environment variable:

- **If set**: Uses `MqttDefaultFilePersistence` to store MQTT data in the specified directory
- **If not set**: Falls back to `MemoryPersistence` (in-memory only, data lost on restart)

### Code Logic

```java
String persistenceDir = System.getenv("MQTT_PERSISTENCE_DIR");

if (persistenceDir != null && !persistenceDir.isBlank()) {
    // File persistence
    MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistenceDir);
    client = new MqttClient(brokerUrl, clientId, persistence);
} else {
    // Memory persistence fallback
    client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
}
```

## Docker Configuration

### Environment Variable

In `docker-compose.yml`, the persistence directory is configured:

```yaml
environment:
  MQTT_PERSISTENCE_DIR: /app/mqtt-persistence
```

### Volume Mount

A named volume is mounted to persist data across container restarts:

```yaml
volumes:
  - mqtt_persistence:/app/mqtt-persistence
```

### Volume Definition

```yaml
volumes:
  mqtt_persistence:
    driver: local
```

## Benefits

1. **Message Reliability**: Messages are persisted to disk, surviving application restarts
2. **QoS Support**: Proper support for MQTT QoS 1 and 2 message delivery guarantees
3. **Automatic Reconnection**: On restart, the client can recover in-flight messages
4. **Flexibility**: Works in Docker (with volume) or standalone (with or without persistence)

## Local Development

For local development without Docker:

```bash
# With file persistence
export MQTT_PERSISTENCE_DIR=/tmp/mqtt-persistence
mvn spring-boot:run

# Without (uses memory)
mvn spring-boot:run
```

## Docker Deployment

The persistence directory and volume are automatically configured in `docker-compose.yml`.

### View Persisted Data

```bash
# List files in persistence volume
docker-compose exec feeder-server ls -la /app/mqtt-persistence

# Inspect volume
docker volume inspect feeder-server-app_mqtt_persistence
```

### Backup Persistence Data

```bash
# Create backup
docker run --rm \
  -v feeder-server-app_mqtt_persistence:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/mqtt-persistence-backup.tar.gz -C /data .

# Restore backup
docker run --rm \
  -v feeder-server-app_mqtt_persistence:/data \
  -v $(pwd):/backup \
  alpine sh -c "cd /data && tar xzf /backup/mqtt-persistence-backup.tar.gz"
```

## Troubleshooting

### Permission Issues

If you encounter permission errors:

```bash
# Fix ownership in Docker
docker-compose exec feeder-server chown -R spring:spring /app/mqtt-persistence
```

### Clear Persistence Data

To start fresh:

```bash
# Stop containers
docker-compose down

# Remove persistence volume
docker volume rm feeder-server-app_mqtt_persistence

# Restart
docker-compose up -d
```

## Configuration Summary

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `MQTT_PERSISTENCE_DIR` | (not set) | Directory path for MQTT message persistence |

When not set, the application uses memory persistence and logs:
```
Using memory persistence (no MQTT_PERSISTENCE_DIR set)
```

When set, the application logs:
```
Using file persistence at directory: /app/mqtt-persistence
```

