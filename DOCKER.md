# Docker Deployment Guide

This guide provides instructions for deploying the Cat Feeder Server application using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 512MB RAM available for the application
- PostgreSQL database (can be included in docker-compose)
- MQTT broker (can be included in docker-compose)

## Building the Docker Image

### Build Locally

```bash
# From the project root directory
docker build -t cat-feeder-server:latest .

# Build with specific version tag
docker build -t cat-feeder-server:0.0.1 .
```

### Build Arguments (Optional)

```bash
# Build with custom Maven options
docker build --build-arg MAVEN_OPTS="-Xmx512m" -t cat-feeder-server:latest .
```

## Running with Docker

### Simple Run (Development)

```bash
docker run -d \
  --name feeder-server \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=develop \
  cat-feeder-server:latest
```

### Run with Production Configuration

```bash
docker run -d \
  --name feeder-server \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=feeder \
  -e DB_USERNAME=feeder_user \
  -e DB_PASSWORD=secure_password \
  -e MQTT_USERNAME=mqtt_user \
  -e MQTT_PASSWORD=secure_mqtt_pass \
  cat-feeder-server:latest
```

## Docker Compose Configuration

### Complete Stack (Recommended)

Create a `docker-compose.yml` file in your deployment directory:

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:17-alpine
    container_name: feeder-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: feeder
      POSTGRES_USER: feeder_user
      POSTGRES_PASSWORD: ${DB_PASSWORD:-change_me_in_production}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U feeder_user -d feeder"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - feeder-network

  # MQTT Broker (Eclipse Mosquitto)
  mosquitto:
    image: eclipse-mosquitto:2.0-openssl
    container_name: feeder-mosquitto
    restart: unless-stopped
    ports:
      - "1883:1883"
      - "9001:9001"
    volumes:
      - mosquitto_config:/mosquitto/config
      - mosquitto_data:/mosquitto/data
      - mosquitto_log:/mosquitto/log
    networks:
      - feeder-network
    command: mosquitto -c /mosquitto-no-auth.conf

  # Cat Feeder Server Application
  feeder-server:
    image: cat-feeder-server:latest
    container_name: feeder-server
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
      mosquitto:
        condition: service_started
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: feeder
      DB_USERNAME: feeder_user
      DB_PASSWORD: ${DB_PASSWORD:-change_me_in_production}
      MQTT_USERNAME: ${MQTT_USERNAME:-}
      MQTT_PASSWORD: ${MQTT_PASSWORD:-}
    ports:
      - "8080:8080"
    networks:
      - feeder-network
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/feeder-service/api/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres_data:
    driver: local
  mosquitto_config:
    driver: local
  mosquitto_data:
    driver: local
  mosquitto_log:
    driver: local

networks:
  feeder-network:
    driver: bridge
```

### Minimal Configuration (Application Only)

If you already have PostgreSQL and MQTT running elsewhere:

```yaml
version: '3.8'

services:
  feeder-server:
    image: cat-feeder-server:latest
    container_name: feeder-server
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: ${DB_HOST}
      DB_PORT: ${DB_PORT:-5432}
      DB_NAME: ${DB_NAME:-feeder}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      MQTT_USERNAME: ${MQTT_USERNAME:-}
      MQTT_PASSWORD: ${MQTT_PASSWORD:-}
    ports:
      - "8080:8080"
    networks:
      - external-network

networks:
  external-network:
    external: true
```

## Environment Variables

Create a `.env` file in the same directory as `docker-compose.yml`:

```bash
# Database Configuration
DB_PASSWORD=your_secure_db_password_here

# MQTT Configuration (optional if not using authentication)
MQTT_USERNAME=your_mqtt_username
MQTT_PASSWORD=your_mqtt_password
```

**Important:** Add `.env` to your `.gitignore` file to avoid committing sensitive data!

## Running with Docker Compose

### Start All Services

```bash
# Start in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f feeder-server
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This deletes all data!)
docker-compose down -v
```

### Rebuild and Restart

```bash
# Rebuild the application image
docker-compose build feeder-server

# Restart with new image
docker-compose up -d --force-recreate feeder-server
```

## Accessing the Application

Once running, the application will be available at:

- **API Base URL:** `http://localhost:8080/feeder-service/api`
- **Health Check:** `http://localhost:8080/feeder-service/api/actuator/health`
- **PostgreSQL:** `localhost:5432`
- **MQTT:** `localhost:1883`

## Monitoring and Maintenance

### View Container Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f feeder-server

# Last 100 lines
docker-compose logs --tail=100 feeder-server
```

### Check Container Status

```bash
docker-compose ps
```

### Execute Commands in Container

```bash
# Open shell in feeder-server container
docker-compose exec feeder-server sh

# Check Java version
docker-compose exec feeder-server java -version
```

### Database Backup

```bash
# Backup PostgreSQL database
docker-compose exec postgres pg_dump -U feeder_user feeder > backup_$(date +%Y%m%d).sql

# Restore from backup
docker-compose exec -T postgres psql -U feeder_user feeder < backup_20251111.sql
```

## Production Deployment Checklist

- [ ] Change default passwords in `.env` file
- [ ] Enable MQTT authentication
- [ ] Configure proper network security (firewall rules)
- [ ] Set up SSL/TLS certificates for HTTPS
- [ ] Configure backup strategy for PostgreSQL
- [ ] Set up monitoring and alerting
- [ ] Review and adjust memory limits
- [ ] Enable Docker logging driver for centralized logs
- [ ] Test health checks and restart policies
- [ ] Document recovery procedures

## Troubleshooting

### Application Won't Start

1. Check logs: `docker-compose logs feeder-server`
2. Verify environment variables: `docker-compose config`
3. Ensure PostgreSQL is healthy: `docker-compose ps postgres`
4. Check network connectivity: `docker-compose exec feeder-server ping postgres`

### Database Connection Issues

```bash
# Test database connection from application container
docker-compose exec feeder-server sh -c 'apk add postgresql-client && psql -h postgres -U feeder_user -d feeder'
```

### MQTT Connection Issues

```bash
# Test MQTT connection
docker-compose exec feeder-server sh -c 'apk add mosquitto-clients && mosquitto_pub -h mosquitto -t test -m "hello"'
```

## Scaling and Performance

### Memory Configuration

Add resource limits to `docker-compose.yml`:

```yaml
services:
  feeder-server:
    # ...existing config...
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M
```

### Multiple Instances (Load Balancing)

```bash
# Scale to 3 instances
docker-compose up -d --scale feeder-server=3

# Requires additional load balancer configuration
```

## Security Best Practices

1. **Never commit sensitive data** to version control
2. **Use secrets management** for production (Docker Secrets, Kubernetes Secrets)
3. **Run containers as non-root** (already configured in Dockerfile)
4. **Regularly update base images** for security patches
5. **Enable TLS/SSL** for all external connections
6. **Use network segmentation** to isolate services
7. **Implement proper access controls** on MQTT topics

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Eclipse Mosquitto Docker Hub](https://hub.docker.com/_/eclipse-mosquitto)

