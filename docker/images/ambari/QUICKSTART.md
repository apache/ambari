# Apache Ambari Docker Quick Start Guide

This guide will help you quickly set up and run Apache Ambari using Docker for development and testing purposes.

## Prerequisites

- Docker 20.10+ installed and running
- Docker Compose 2.0+ installed
- At least 8GB RAM available for Docker
- At least 20GB free disk space

## Quick Setup (5 minutes)

### 1. Clone and Navigate
```bash
git clone https://github.com/apache/ambari.git
cd ambari
```

### 2. Start Development Environment
```bash
# Start all services
docker-compose -f docker/docker-compose.dev.yml up -d

# Check status
docker-compose -f docker/docker-compose.dev.yml ps
```

### 3. Access Services
- **Ambari Web UI**: http://localhost:8080 (admin/admin)
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Database**: localhost:5432 (ambari/bigdata)

### 4. Build Ambari (Optional)
```bash
# Enter development container
docker-compose -f docker/docker-compose.dev.yml exec ambari-server bash

# Build Ambari
cd /workspace
mvn clean install -DskipTests

# Or use the build script
./docker/build.sh all
```

## Step-by-Step Setup

### Step 1: Environment Check
```bash
# Check Docker version
docker --version
docker-compose --version

# Check available resources
docker system df
docker system info | grep -E "CPUs|Total Memory"
```

### Step 2: Build Base Images (Optional)
```bash
# Build all images from source
./docker/build.sh all

# Or build specific images
./docker/build.sh base
./docker/build.sh server
./docker/build.sh agent
./docker/build.sh metrics
```

### Step 3: Start Services
```bash
# Start database first
docker-compose -f docker/docker-compose.dev.yml up -d ambari-db

# Wait for database to be ready
docker-compose -f docker/docker-compose.dev.yml logs -f ambari-db

# Start all services
docker-compose -f docker/docker-compose.dev.yml up -d

# Monitor startup
docker-compose -f docker/docker-compose.dev.yml logs -f
```

### Step 4: Verify Installation
```bash
# Check service health
docker-compose -f docker/docker-compose.dev.yml ps

# Check Ambari Server logs
docker-compose -f docker/docker-compose.dev.yml logs ambari-server

# Test API endpoint
curl -u admin:admin http://localhost:8080/api/v1/clusters
```

## Development Workflow

### Building and Testing
```bash
# Enter development container
docker-compose -f docker/docker-compose.dev.yml exec ambari-server bash

# Navigate to workspace
cd /workspace

# Build specific module
mvn clean install -pl ambari-server -DskipTests

# Run tests
mvn test -pl ambari-server

# Build RPMs
mvn clean package rpm:rpm -DskipTests
```

### Live Development
```bash
# Source code is mounted at /workspace
# Changes are reflected immediately

# Restart services after code changes
docker-compose -f docker/docker-compose.dev.yml restart ambari-server

# View logs
docker-compose -f docker/docker-compose.dev.yml logs -f ambari-server
```

### Debugging
```bash
# Enable debug mode
export AMBARI_DEBUG=true
docker-compose -f docker/docker-compose.dev.yml up -d

# Connect debugger to:
# - Server: localhost:5005
# - Agent: localhost:5006

# Access container shell
docker-compose -f docker/docker-compose.dev.yml exec ambari-server bash
```

## Common Tasks

### Scale Agents
```bash
# Scale to 3 agents
docker-compose -f docker/docker-compose.dev.yml up -d --scale ambari-agent=3

# Check agent status
docker-compose -f docker/docker-compose.dev.yml ps ambari-agent
```

### Database Operations
```bash
# Connect to database
docker-compose -f docker/docker-compose.dev.yml exec ambari-db psql -U ambari -d ambari

# Backup database
docker-compose -f docker/docker-compose.dev.yml exec ambari-db pg_dump -U ambari ambari > backup.sql

# Restore database
docker-compose -f docker/docker-compose.dev.yml exec -T ambari-db psql -U ambari -d ambari < backup.sql
```

### View Metrics
```bash
# Access Grafana
open http://localhost:3000

# Check metrics API
curl http://localhost:6188/ws/v1/timeline/metrics/metadata

# View metrics logs
docker-compose -f docker/docker-compose.dev.yml logs ambari-metrics
```

### Clean Up
```bash
# Stop all services
docker-compose -f docker/docker-compose.dev.yml down

# Remove volumes (WARNING: This deletes all data)
docker-compose -f docker/docker-compose.dev.yml down -v

# Clean up images
docker system prune -a
```

## Troubleshooting

### Common Issues

#### Services Won't Start
```bash
# Check logs
docker-compose -f docker/docker-compose.dev.yml logs

# Check resource usage
docker stats

# Restart specific service
docker-compose -f docker/docker-compose.dev.yml restart ambari-server
```

#### Database Connection Issues
```bash
# Check database status
docker-compose -f docker/docker-compose.dev.yml exec ambari-db pg_isready -U ambari

# Reset database
docker-compose -f docker/docker-compose.dev.yml down
docker volume rm ambari_ambari-db-data
docker-compose -f docker/docker-compose.dev.yml up -d ambari-db
```

#### Port Conflicts
```bash
# Check what's using ports
lsof -i :8080
lsof -i :5432

# Change ports in docker-compose.dev.yml
# Example: "8081:8080" instead of "8080:8080"
```

#### Memory Issues
```bash
# Check Docker memory limit
docker system info | grep Memory

# Increase Docker memory in Docker Desktop settings
# Or reduce JVM heap sizes in docker-compose.dev.yml
```

### Health Checks
```bash
# Manual health check
docker-compose -f docker/docker-compose.dev.yml exec ambari-server /usr/local/bin/health-check.sh

# Check all container health
docker-compose -f docker/docker-compose.dev.yml ps
```

### Performance Tuning
```bash
# Enable BuildKit for faster builds
export DOCKER_BUILDKIT=1

# Use cached Maven dependencies
# (Already configured in docker-compose.dev.yml)

# Increase JVM memory if needed
# Edit JAVA_OPTS in docker-compose.dev.yml
```

## Advanced Usage

### Custom Configuration
```bash
# Create custom configuration
mkdir -p docker/config/server
echo "server.jdbc.url=jdbc:postgresql://custom-db:5432/ambari" > docker/config/server/ambari.properties

# Mount custom config
# Add to docker-compose.dev.yml volumes:
# - ./docker/config/server:/etc/ambari-server/conf
```

### Multi-Node Setup
```bash
# Scale agents
docker-compose -f docker/docker-compose.dev.yml up -d --scale ambari-agent=3

# Each agent gets a unique hostname
# Check with: docker-compose -f docker/docker-compose.dev.yml ps
```

### Production-Like Setup
```bash
# Use production compose file
docker-compose -f docker/docker-compose.prod.yml up -d

# Enable HTTPS
# Set AMBARI_SERVER_HTTPS=true in environment
```

### Integration with IDEs

#### IntelliJ IDEA
1. Open project in IntelliJ
2. Configure remote debugger:
   - Host: localhost
   - Port: 5005 (server) or 5006 (agent)
3. Set breakpoints and start debugging

#### VS Code
1. Install Docker extension
2. Use "Attach to Container" feature
3. Configure remote debugging in launch.json

## Next Steps

- Read the full [Docker README](README.md) for detailed configuration options
- Check out [Production Deployment Guide](docker-compose.prod.yml) for production setup
- Explore [Kubernetes deployment](k8s/) for container orchestration
- Join the [Ambari community](https://ambari.apache.org/mail-lists.html) for support

## Getting Help

- **Documentation**: [Apache Ambari Docs](https://ambari.apache.org/)
- **Issues**: [GitHub Issues](https://github.com/apache/ambari/issues)
- **Community**: [Mailing Lists](https://ambari.apache.org/mail-lists.html)
- **Docker Logs**: `docker-compose -f docker/docker-compose.dev.yml logs`

## Useful Commands Reference

```bash
# Start services
docker-compose -f docker/docker-compose.dev.yml up -d

# Stop services
docker-compose -f docker/docker-compose.dev.yml down

# View logs
docker-compose -f docker/docker-compose.dev.yml logs -f [service-name]

# Execute commands in container
docker-compose -f docker/docker-compose.dev.yml exec [service-name] [command]

# Scale services
docker-compose -f docker/docker-compose.dev.yml up -d --scale [service-name]=[count]

# Build images
./docker/build.sh [image-name]

# Health check
docker-compose -f docker/docker-compose.dev.yml exec [service-name] /usr/local/bin/health-check.sh
```

Happy developing with Apache Ambari on Docker! 🐳
