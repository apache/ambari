# docker-ambari

This is a docker image of [Apache Ambari](https://github.com/apache/ambari).

## How to build a docker image

There is a docker build script which will build a given Git repo/branch and tag the image.

Usage:
```bash
./docker-build.sh [Docker Tag] [Git Branch] [Ambari Git URL] [Kafka Version] [Java Version] [JDK Version] [OpenJDK Image]
```

This script will check out Ambari Repo `[Ambari Git URL]` on branch `[Git Branch]` and build the docker image for that.

The docker image is tagged as `[Docker Tag]`.

**Parameters:**
- `Docker Tag`: Name and tag your docker image. Default is `ambari:latest`.
- `Git Branch`: The Ambari branch to build. Default is `trunk`.
- `Ambari Git URL`: The Ambari Git Repo to build, users can set it to their own fork. Please note that, the URL is `https://` based, not `git://`. Default is the Apache Repo: `https://github.com/apache/ambari.git`.
- `Kafka Version`: The Kafka Version to build ambari with. Default is `2.0`
- `Java Version`: The Java Build and Runtime image version. Default is `8`
- `JDK Version`: The JDK parameter to build ambari, set as part of maven build option: `-Djdk.version=${JDK_VERSION}`. Default is `8`
- `OpenJDK Image`: Base image to use for Ambari build and runtime. Default is `openjdk`.

### Examples

* Example of building and tagging a snapshot on your own fork:
```bash
./docker-build.sh ambari_fork:snapshot-3.0 snapshot-3.0 https://github.com/your_own_fork/ambari.git
```

* Example of building a release version:
```bash
./docker-build.sh ambari:release-2.7.5 release-2.7.5 https://github.com/apache/ambari.git
```

### Build image with arm64 base image

For users on Mac M1 chips, they need to build the images with arm64 base image, e.g. `arm64v8/openjdk`

* Example of building an arm64 image:
```bash
./docker-build.sh ambari:latest trunk https://github.com/apache/ambari.git 2.0 8 8 arm64v8/openjdk
```

or just run the docker build script directly:
```bash
docker build -t ambari:latest --no-cache --network=host \
  --build-arg AMBARI_GIT_URL=https://github.com/apache/ambari.git \
  --build-arg AMBARI_BRANCH=trunk \
  --build-arg JDK_VERSION=8 \
  --build-arg OPENJDK_IMAGE=arm64v8/openjdk \
  -f Dockerfile .
```

Note that if you are not on arm64 machine, you can still build the image by turning on the experimental feature of docker, and add `--platform linux/arm64` into the `docker build ...` script.

## How to publish a docker image

Script `docker-push.sh` publishes a given docker image to your docker registry.

In order to push to your own repo, the image needs to be explicitly tagged with the repo name.

* Example of publishing a image to [apache/ambari](https://hub.docker.com/r/apache/ambari) dockerHub repo:
```bash
./docker-push.sh apache/ambari:latest
```

* Tag a built image, then push:
```bash
docker tag ambari:release-2.7.5 apache/ambari:release-2.7.5
docker push apache/ambari:release-2.7.5
```

Script `docker-build-and-push.sh` builds and publishes this docker image to your docker registry after build.

* Example of building and publishing a image to [apache/ambari](https://hub.docker.com/r/apache/ambari) dockerHub repo:
```bash
./docker-build-and-push.sh apache/ambari:latest trunk https://github.com/apache/ambari.git
```

## How to Run it

The entry point of docker image is `ambari-admin.sh` script.

### Bring up PostgreSQL Database

Example of bring up a local PostgreSQL database in docker:
```bash
docker pull postgres:13-alpine
docker run --name ambari-database --restart always -p 5432:5432 \
  -e POSTGRES_DB=ambari \
  -e POSTGRES_USER=ambari \
  -e POSTGRES_PASSWORD=bigdata \
  postgres:13-alpine
```

You can extract the database host from:
```bash
docker inspect ambari-database | grep IPAddress
            "SecondaryIPAddresses": null,
            "IPAddress": "172.17.0.2",
                    "IPAddress": "172.17.0.2",
```

Please use local database path `172.17.0.2:5432` as database host parameter.

### Ambari Server

Example of bring up a local Ambari Server:
```bash
docker run -p 8080:8080 \
  -e AMBARI_DB_HOST=172.17.0.2 \
  -e AMBARI_DB_PORT=5432 \
  -e AMBARI_DB_NAME=ambari \
  -e AMBARI_DB_USER=ambari \
  -e AMBARI_DB_PASSWORD=bigdata \
  apache/ambari:latest StartServer
```

### Ambari Agent

Example of bring up a local Ambari Agent:
```bash
docker run \
  -e AMBARI_SERVER_HOST=172.17.0.3 \
  -e AMBARI_SERVER_PORT=8080 \
  --privileged \
  apache/ambari:latest StartAgent
```

### Ambari Metrics Collector (Optional)

Example of bring up Ambari Metrics Collector:
```bash
docker run -p 6188:6188 \
  -e AMS_HBASE_ROOTDIR=/var/lib/ambari-metrics-collector/hbase \
  -e AMS_COLLECTOR_PORT=6188 \
  apache/ambari:latest StartMetrics
```

## QuickStart

### Use docker compose to bring up Ambari stack

Below is a script to use docker compose to bring up database/ambari-server/ambari-agent

```bash
docker-compose -f docker-compose.yml up
```

### Enable Optional Components

#### Enable Metrics Collection
```bash
export METRICS_ENABLED=1
docker-compose --profile metrics -f docker-compose.yml up
```

#### Enable Grafana Dashboard
```bash
export METRICS_ENABLED=1
export GRAFANA_ENABLED=1
docker-compose --profile metrics -f docker-compose.yml up
```

#### Enable Zookeeper and Kafka
```bash
export ZOOKEEPER_ENABLED=1
export KAFKA_ENABLED=1
docker-compose --profile zookeeper --profile kafka -f docker-compose.yml up
```

#### Enable All Components
```bash
export METRICS_ENABLED=1
export GRAFANA_ENABLED=1
export ZOOKEEPER_ENABLED=1
export KAFKA_ENABLED=1
docker-compose --profile metrics --profile zookeeper --profile kafka -f docker-compose.yml up
```

### Scale Ambari Agents

You can scale the number of Ambari agents:
```bash
docker-compose -f docker-compose.yml up -d --scale ambari-agent=3
```

### Access Services

Once your cluster is up and running:

- **Ambari Web UI**: http://localhost:8080 (admin/admin)
- **Grafana Dashboard**: http://localhost:3000 (admin/admin) - if enabled
- **Metrics API**: http://localhost:6188/ws/v1/timeline/metrics/metadata - if enabled
- **Database**: localhost:5432 (ambari/bigdata)

## Configuration

### Environment Variables

#### Ambari Server
- `AMBARI_DB_HOST`: Database hostname (default: ambari-database)
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

#### Ambari Metrics (Optional)
- `AMS_HBASE_ROOTDIR`: HBase root directory (default: /var/lib/ambari-metrics-collector/hbase)
- `AMS_COLLECTOR_PORT`: Collector port (default: 6188)

#### Optional Components Control
- `METRICS_ENABLED`: Enable metrics collection (0 or 1, default: 0)
- `GRAFANA_ENABLED`: Enable Grafana dashboard (0 or 1, default: 0)
- `ZOOKEEPER_ENABLED`: Enable Zookeeper (0 or 1, default: 0)
- `KAFKA_ENABLED`: Enable Kafka (0 or 1, default: 0)

### Docker Images
- `AMBARI_IMAGE`: Ambari image to use (default: apache/ambari:latest)
- `POSTGRES_IMAGE`: PostgreSQL image to use (default: postgres:13-alpine)
- `ZK_IMAGE`: Zookeeper image to use (default: zookeeper:3.8.1)
- `KAFKA_IMAGE`: Kafka image to use (default: bitnami/kafka:3.6)

## Available Dockerfiles

### Dockerfile
Main production Dockerfile that builds Ambari from source and creates a runtime image.

### Dockerfile.build
Build-only Dockerfile for development environments with all build tools.

### Dockerfile.package
Package-based Dockerfile that uses pre-built Ambari packages instead of building from source.

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
docker-compose exec ambari-database pg_isready -U ambari

# Check database logs
docker-compose logs ambari-database

# Reset database
docker-compose down -v
docker-compose up -d ambari-database
```

#### Memory Issues
```bash
# Check resource usage
docker stats

# Increase memory limits in docker-compose.yml
services:
  ambari-server:
    deploy:
      resources:
        limits:
          memory: 4G
```

### Health Checks

All services include health checks:
- **Server**: HTTP check on `/api/v1/clusters`
- **Agent**: Process and heartbeat check
- **Metrics**: HTTP check on `/ws/v1/timeline/metrics/metadata`
- **Database**: Connection check

### Debug Mode

Enable debug logging:
```bash
export AMBARI_DEBUG=true
docker-compose up -d
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
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: '2.0'
        reservations:
          memory: 2G
          cpus: '1.0'
```

### JVM Tuning
```bash
# Server JVM options
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

# Metrics JVM options  
JAVA_OPTS="-Xmx1g -Xms512m"
```

## Testing and Deployment Workflows

### Complete Build and Test Workflow

#### 1. Build Base Images First
```bash
# Build base image (required for all other builds)
cd docker/images/ambari-base
./docker-build.sh ambari-base:latest

# Build metrics base (optional)
cd ../ambari-metrics
./docker-build.sh ambari-metrics:latest
```

#### 2. Build Main Ambari Image
```bash
# Build from current branch
cd ../ambari
./docker-build.sh ambari:latest

# Build from specific branch/fork
./docker-build.sh ambari:my-feature feature-branch https://github.com/myuser/ambari.git

# Build with custom options
./docker-build.sh ambari:arm64 trunk https://github.com/apache/ambari.git 2.0 8 8 arm64v8/openjdk
```

#### 3. Test the Built Images
```bash
# Quick test - start basic services
./quick-test.sh

# Minimal test - server only
./minimal-test.sh

# Full test - all components
./simple-test.sh

# Manual testing with docker-compose
docker-compose up -d
docker-compose ps
docker-compose logs ambari-server
```

#### 4. Verify Functionality
```bash
# Check server health
curl http://localhost:8080/api/v1/clusters

# Check metrics (if enabled)
curl http://localhost:6188/ws/v1/timeline/metrics/metadata

# Access web UI
open http://localhost:8080  # admin/admin
```

### Publishing Images to Registry

#### 1. Tag Images Properly
```bash
# Tag for Docker Hub
docker tag ambari:latest apache/ambari:latest
docker tag ambari:latest apache/ambari:$(date +%Y%m%d)

# Tag for custom registry
docker tag ambari:latest myregistry.com/ambari:latest
```

#### 2. Push Using Scripts
```bash
# Push to Docker Hub
./docker-push.sh apache/ambari:latest

# Push with authentication
./docker-push.sh apache/ambari:latest --username myuser --password mypass

# Push to custom registry
./docker-push.sh myregistry.com/ambari:latest --registry myregistry.com

# Dry run to verify
./docker-push.sh apache/ambari:latest --dry-run
```

#### 3. Build and Push in One Step
```bash
# Build and push latest
./docker-build-and-push.sh apache/ambari:latest

# Build specific branch and push
./docker-build-and-push.sh apache/ambari:feature-v1 feature-branch https://github.com/myuser/ambari.git
```

### Automated CI/CD Pipeline Example

#### GitHub Actions Workflow
```yaml
name: Build and Push Ambari Docker Images

on:
  push:
    branches: [ main, trunk ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Build Base Image
      run: |
        cd docker/images/ambari-base
        ./docker-build.sh ambari-base:${{ github.sha }}
    
    - name: Build Main Image
      run: |
        cd docker/images/ambari
        ./docker-build.sh ambari:${{ github.sha }}
    
    - name: Test Images
      run: |
        cd docker/images/ambari
        ./quick-test.sh
    
    - name: Push to Registry
      if: github.ref == 'refs/heads/main'
      run: |
        echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
        ./docker-push.sh apache/ambari:${{ github.sha }}
        ./docker-push.sh apache/ambari:latest
```

### Development Workflow

#### 1. Local Development Setup
```bash
# Clone and setup
git clone https://github.com/apache/ambari.git
cd ambari

# Build development image
cd docker/images/ambari
./docker-build.sh ambari:dev trunk https://github.com/apache/ambari.git

# Start development environment
docker-compose -f docker-compose.dev.yml up -d
```

#### 2. Testing Changes
```bash
# Rebuild after changes
./docker-build.sh ambari:dev --no-cache

# Test specific components
docker-compose restart ambari-server
docker-compose logs -f ambari-server

# Run integration tests
./TESTING.md  # Follow testing guide
```

#### 3. Multi-Architecture Builds
```bash
# Setup buildx for multi-arch
docker buildx create --name multiarch --use
docker buildx inspect --bootstrap

# Build for multiple platforms
docker buildx build --platform linux/amd64,linux/arm64 \
  -t apache/ambari:latest \
  --push .
```

### Production Deployment

#### 1. Pre-deployment Checklist
- [ ] Images tested in staging environment
- [ ] Security scan completed
- [ ] Performance benchmarks met
- [ ] Documentation updated
- [ ] Rollback plan prepared

#### 2. Blue-Green Deployment
```bash
# Deploy to green environment
docker-compose -f docker-compose.prod.yml -p ambari-green up -d

# Verify green environment
curl http://green-ambari:8080/api/v1/clusters

# Switch traffic (update load balancer)
# Monitor and rollback if needed

# Cleanup old blue environment
docker-compose -p ambari-blue down
```

#### 3. Rolling Updates
```bash
# Update images one by one
docker-compose pull
docker-compose up -d --no-deps ambari-server
# Wait and verify
docker-compose up -d --no-deps ambari-agent
```

### Monitoring and Maintenance

#### 1. Health Monitoring
```bash
# Check all services
docker-compose ps
docker stats

# Monitor logs
docker-compose logs -f --tail=100

# Health check endpoints
curl http://localhost:8080/api/v1/clusters
curl http://localhost:6188/ws/v1/timeline/metrics/metadata
```

#### 2. Backup and Recovery
```bash
# Backup database
docker-compose exec ambari-database pg_dump -U ambari ambari > backup.sql

# Backup configurations
docker cp ambari_ambari-server_1:/etc/ambari-server/conf ./backup/

# Restore from backup
docker-compose exec -T ambari-database psql -U ambari ambari < backup.sql
```

## Contributing

### Adding New Services
1. Create Dockerfile in appropriate location
2. Add to docker-compose.yml
3. Update documentation
4. Add health checks
5. Test thoroughly

### Testing Changes
```bash
# Run basic test
docker-compose up -d
docker-compose ps

# Test specific component
docker-compose logs ambari-server

# Clean test environment
docker-compose down -v
```

## Support

- **Documentation**: [Apache Ambari Docs](https://ambari.apache.org/)
- **Issues**: [GitHub Issues](https://github.com/apache/ambari/issues)
- **Community**: [Apache Ambari Mailing Lists](https://ambari.apache.org/mail-lists.html)
