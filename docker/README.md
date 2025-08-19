# Apache Ambari Docker Setup

This directory contains Docker configurations for Apache Ambari, providing containerized environments for development, testing, and production deployment.

## Quick Start

### Prerequisites
- Docker 20.10+ 
- Docker Compose 2.0+
- At least 8GB RAM available for Docker

### Development Environment
```bash
# Clone and build Ambari
git clone https://github.com/apache/ambari.git
cd ambari

# Start development environment
docker-compose -f docker/docker-compose.dev.yml up -d

# Access Ambari Web UI
open http://localhost:8080
# Default credentials: admin/admin
```

### Production Deployment
```bash
# Start production stack
docker-compose -f docker/docker-compose.prod.yml up -d

# Scale agents
docker-compose -f docker/docker-compose.prod.yml up -d --scale ambari-agent=3
```

## Available Images

### Base Images
- `apache/ambari-base`: Common runtime dependencies
- `apache/ambari-build`: Build environment with Maven, JDK, Node.js

### Service Images  
- `apache/ambari-server`: Ambari Server
- `apache/ambari-agent`: Ambari Agent
- `apache/ambari-metrics`: Metrics Collector with embedded HBase
- `apache/ambari-dev`: Development environment

### Supporting Images
- `apache/ambari-database`: PostgreSQL with Ambari schema
- `apache/ambari-grafana`: Grafana with Ambari Metrics datasource

## Build Instructions

### Build All Images
```bash
# Build all images
./docker/build.sh

# Build specific image
./docker/build.sh ambari-server

# Build with custom tag
./docker/build.sh ambari-server --tag myregistry/ambari-server:latest
```

### Development Build
```bash
# Build development image with current source
docker build -f docker/Dockerfile.dev -t ambari-dev .

# Run development container
docker run -it --rm \
  -v $(pwd):/workspace \
  -p 8080:8080 \
  -p 8441:8441 \
  ambari-dev
```

## Configuration

### Environment Variables

#### Ambari Server
- `AMBARI_DB_HOST`: Database hostname (default: ambari-db)
- `AMBARI_DB_PORT`: Database port (default: 5432)
- `AMBARI_DB_NAME`: Database name (default: ambari)
- `AMBARI_DB_USER`: Database user (default: ambari)
- `AMBARI_DB_PASSWORD`: Database password (default: bigdata)
- `AMBARI_SERVER_HTTPS`: Enable HTTPS (default: false)
- `JAVA_OPTS`: JVM options

#### Ambari Agent
- `AMBARI_SERVER_HOST`: Ambari Server hostname (default: ambari-server)
- `AMBARI_SERVER_PORT`: Ambari Server port (default: 8080)
- `AGENT_HOSTNAME`: Agent hostname (auto-detected if not set)

#### Ambari Metrics
- `AMS_HBASE_ROOTDIR`: HBase root directory (default: /var/lib/ambari-metrics-collector/hbase)
- `AMS_COLLECTOR_PORT`: Collector port (default: 6188)
- `AMS_GRAFANA_ENABLED`: Enable Grafana (default: true)

### Volume Mounts

#### Persistent Data
- `/var/lib/ambari-server`: Server data and configurations
- `/var/lib/ambari-agent`: Agent data and logs
- `/var/lib/ambari-metrics-collector`: Metrics data
- `/var/log/ambari-*`: Log directories

#### Development
- `/workspace`: Source code mount point
- `/root/.m2`: Maven repository cache

## Docker Compose Configurations

### Development (docker-compose.dev.yml)
- Single-node setup with all services
- Source code mounted for live development
- Debug ports exposed
- Automatic rebuilding enabled

### Production (docker-compose.prod.yml)
- Multi-container production setup
- Separate database container
- Health checks and restart policies
- Resource limits configured

### Testing (docker-compose.test.yml)
- Automated testing environment
- Includes test databases and mock services
- CI/CD friendly configuration

## Networking

### Default Network
- Network: `ambari-network` (bridge)
- Subnet: `172.20.0.0/16`

### Service Discovery
Services are accessible by their container names:
- `ambari-server`: Ambari Server
- `ambari-db`: PostgreSQL Database  
- `ambari-metrics`: Metrics Collector
- `ambari-grafana`: Grafana Dashboard

## Health Checks

All services include health checks:
- **Server**: HTTP check on `/api/v1/clusters`
- **Agent**: Process and heartbeat check
- **Metrics**: HTTP check on `/ws/v1/timeline/metrics`
- **Database**: Connection check

## Troubleshooting

### Common Issues

#### Container Won't Start
```bash
# Check logs
docker-compose logs ambari-server

# Check container status
docker-compose ps

# Restart specific service
docker-compose restart ambari-server
```

#### Database Connection Issues
```bash
# Verify database is running
docker-compose exec ambari-db pg_isready

# Check database logs
docker-compose logs ambari-db

# Reset database
docker-compose down -v
docker-compose up -d ambari-db
```

#### Memory Issues
```bash
# Check resource usage
docker stats

# Increase memory limits in docker-compose.yml
services:
  ambari-server:
    mem_limit: 4g
```

### Debug Mode

#### Enable Debug Logging
```bash
# Set environment variable
export AMBARI_DEBUG=true
docker-compose up -d

# Or in docker-compose.yml
environment:
  - AMBARI_DEBUG=true
```

#### Remote Debugging
```bash
# Server debug port: 5005
# Agent debug port: 5006

# Connect with IDE to localhost:5005
```

## Security Considerations

### Production Deployment
1. Change default passwords
2. Use external database with proper credentials
3. Enable HTTPS/TLS
4. Configure firewall rules
5. Use secrets management
6. Regular security updates

### Network Security
```yaml
# Example secure configuration
services:
  ambari-server:
    environment:
      - AMBARI_DB_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - db_password
      
secrets:
  db_password:
    external: true
```

## Performance Tuning

### Resource Allocation
```yaml
# Recommended production resources
services:
  ambari-server:
    mem_limit: 4g
    cpus: 2.0
    
  ambari-metrics:
    mem_limit: 2g
    cpus: 1.0
```

### JVM Tuning
```bash
# Server JVM options
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

# Metrics JVM options  
AMS_JAVA_OPTS="-Xmx1g -Xms512m"
```

## Contributing

### Adding New Services
1. Create Dockerfile in `docker/services/`
2. Add to appropriate docker-compose file
3. Update documentation
4. Add health checks
5. Test thoroughly

### Testing Changes
```bash
# Run test suite
./docker/test.sh

# Specific test
./docker/test.sh integration

# Clean test environment
./docker/test.sh clean
```

## Support

- **Documentation**: [Apache Ambari Docs](https://ambari.apache.org/)
- **Issues**: [GitHub Issues](https://github.com/apache/ambari/issues)
- **Community**: [Apache Ambari Mailing Lists](https://ambari.apache.org/mail-lists.html)
