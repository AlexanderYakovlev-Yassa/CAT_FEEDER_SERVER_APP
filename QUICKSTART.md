# Quick Start Guide - Docker Deployment

This guide helps you quickly deploy the Cat Feeder Server application using Docker.

## Prerequisites

- Docker and Docker Compose installed
- 2GB free disk space
- Ports 8080, 5432, and 1883 available

## Quick Start (5 minutes)

### 1. Clone and Navigate

```bash
cd /path/to/feeder-server-app
```

### 2. Create Environment File

```bash
cp .env.example .env
```

Edit `.env` and set your database password:
```bash
DB_PASSWORD=your_secure_password_here
```

### 3. Build and Run

```bash
# Build and start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f feeder-server
```

### 4. Verify

The application should be running at:
- **API:** http://localhost:8080/feeder-service/api
- **Health:** http://localhost:8080/feeder-service/api/actuator/health

Expected health response:
```json
{
  "status": "UP"
}
```

## Common Commands

```bash
# Stop all services
docker-compose down

# Restart application only
docker-compose restart feeder-server

# View application logs
docker-compose logs -f feeder-server

# Rebuild after code changes
docker-compose build feeder-server
docker-compose up -d feeder-server

# Stop and remove everything (including data!)
docker-compose down -v
```

## Troubleshooting

### Application won't start
```bash
# Check logs
docker-compose logs feeder-server

# Verify PostgreSQL is running
docker-compose ps postgres
```

### Database connection failed
```bash
# Test database connection
docker-compose exec feeder-server sh
# Inside container:
wget -O- http://localhost:8080/feeder-service/api/actuator/health
```

### Port already in use
```bash
# Change ports in docker-compose.yml
ports:
  - "8081:8080"  # Use 8081 instead of 8080
```

## Next Steps

1. **Configure MQTT authentication** - Edit `docker-compose.yml` mosquitto configuration
2. **Set up SSL/TLS** - Add reverse proxy (nginx/traefik)
3. **Configure backups** - Schedule PostgreSQL backups
4. **Monitor logs** - Set up centralized logging

## Full Documentation

- [DOCKER.md](./DOCKER.md) - Complete Docker deployment guide
- [PROFILES.md](./PROFILES.md) - Spring profiles configuration
- [README.md](./README.md) - Application documentation

## Support

For issues or questions:
1. Check logs: `docker-compose logs feeder-server`
2. Review [DOCKER.md](./DOCKER.md) troubleshooting section
3. Check Docker and database status: `docker-compose ps`

