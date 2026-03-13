# Spring Profiles Configuration

This project uses Spring profiles to manage different environment configurations.

## Available Profiles

### 1. **develop** (default)
Development environment with verbose logging and local configuration.

**Features:**
- SQL logging enabled
- Hibernate DDL auto-update
- Debug logging for application
- Local PostgreSQL (localhost:5432)
- Local MQTT broker (localhost:1883)
- Hardcoded credentials (for local development only)

### 2. **prod**
Production environment with security and performance optimizations.

**Features:**
- Minimal logging (INFO level)
- Hibernate DDL validation only (no schema changes)
- Environment variable configuration
- Connection pooling optimized
- Error details hidden from responses
- Externalized secrets

## How to Use

### Running with Default Profile (develop)
```bash
mvn spring-boot:run
```

### Running with Specific Profile

**Development:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=develop
```

**Production:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Running JAR with Profile
```bash
java -jar -Dspring.profiles.active=prod target/feeder-server-0.0.1-SNAPSHOT.jar
```

### Setting Profile via Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar target/feeder-server-0.0.1-SNAPSHOT.jar
```

## Production Environment Variables

When running with `prod` profile, set these environment variables:

```bash
# Database Configuration
export DB_HOST=your-postgres-host
export DB_PORT=5432
export DB_NAME=feeder
export DB_USERNAME=your-db-username
export DB_PASSWORD=your-db-password

# MQTT Configuration
export MQTT_USERNAME=your-mqtt-username
export MQTT_PASSWORD=your-mqtt-password
```

## Configuration Files

- `application.properties` - Common configuration for all profiles
- `application-develop.properties` - Development profile configuration
- `application-prod.properties` - Production profile configuration

## Docker/Kubernetes Deployment

For containerized deployments, pass the profile via environment:

**Docker:**
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
```

**Kubernetes:**
```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: prod
  - name: DB_HOST
    valueFrom:
      secretKeyRef:
        name: db-secret
        key: host
```

