# Apache Ambari Docker Verification Guide

This guide provides step-by-step instructions to verify the Ambari Docker setup and ensure all components are working correctly.

## Prerequisites

- Docker 20.10+ installed and running
- Docker Compose 2.0+ installed
- At least 16GB RAM available for Docker
- At least 50GB free disk space

## Quick Verification Steps

### 1. Environment Check

```bash
# Check Docker version
docker --version
docker-compose --version

# Check available resources
docker system df
docker system info | grep -E "CPUs|Total Memory"

# Ensure you're in the Ambari project root
pwd  # Should show /path/to/ambari
ls docker/  # Should show docker-compose.yml and Dockerfiles
```

### 2. Build Images

```bash
# Build all Ambari images
./docker/build.sh all

# Or build specific images
./docker/build.sh server
./docker/build.sh agent
./docker/build.sh metrics

# Verify images are built
docker images | grep ambari
```

### 3. Start the Complete Stack

```bash
# Start all services
docker-compose -f docker/docker-compose.yml up -d

# Check service status
docker-compose -f docker/docker-compose.yml ps

# Monitor startup logs
docker-compose -f docker/docker-compose.yml logs -f
```

### 4. Verify Service Health

```bash
# Check all container health status
docker-compose -f docker/docker-compose.yml ps

# Check individual service health
docker-compose -f docker/docker-compose.yml exec ambari-server /usr/local/bin/health-check.sh server
docker-compose -f docker/docker-compose.yml exec ambari-agent /usr/local/bin/health-check.sh agent
docker-compose -f docker/docker-compose.yml exec ambari-metrics /usr/local/bin/health-check.sh metrics
```

### 5. Access Web Interfaces

```bash
# Ambari Web UI
open http://localhost:8080
# Default credentials: admin/admin

# Grafana Dashboard
open http://localhost:3000
# Default credentials: admin/admin

# Hadoop NameNode UI
open http://localhost:9870

# YARN ResourceManager UI
open http://localhost:8088
```

### 6. API Verification

```bash
# Test Ambari Server API
curl -u admin:admin http://localhost:8080/api/v1/clusters

# Test Metrics Collector API
curl http://localhost:6188/ws/v1/timeline/metrics/metadata

# Test Grafana API
curl http://localhost:3000/api/health
```

## Detailed Verification Steps

### Database Verification

```bash
# Connect to PostgreSQL database
docker-compose -f docker/docker-compose.yml exec ambari-db psql -U ambari -d ambari

# Check database tables
\dt

# Verify admin user exists
SELECT user_name, admin FROM users WHERE user_name = 'admin';

# Exit database
\q
```

### Zookeeper Verification

```bash
# Check Zookeeper status
docker-compose -f docker/docker-compose.yml exec zookeeper zkServer.sh status

# Connect to Zookeeper CLI
docker-compose -f docker/docker-compose.yml exec zookeeper zkCli.sh

# List root znodes
ls /

# Exit Zookeeper CLI
quit
```

### Hadoop Cluster Verification

```bash
# Check HDFS status
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfsadmin -report

# Create test directory in HDFS
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfs -mkdir /test

# List HDFS contents
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfs -ls /

# Check YARN nodes
docker-compose -f docker/docker-compose.yml exec resourcemanager yarn node -list
```

### Agent Registration Verification

```bash
# Check agent logs
docker-compose -f docker/docker-compose.yml logs ambari-agent

# Verify agent registration in Ambari Server
curl -u admin:admin http://localhost:8080/api/v1/hosts

# Check agent heartbeat
docker-compose -f docker/docker-compose.yml exec ambari-server tail -f /var/log/ambari-server/ambari-server.log | grep heartbeat
```

### Metrics Collection Verification

```bash
# Check metrics collector status
curl http://localhost:6188/ws/v1/timeline/metrics/metadata

# Verify HBase is running (embedded in metrics collector)
docker-compose -f docker/docker-compose.yml exec ambari-metrics ps aux | grep HMaster

# Check metrics data
curl "http://localhost:6188/ws/v1/timeline/metrics?metricNames=cpu_user&hostname=ambari-agent&appId=HOST&startTime=$(date -d '1 hour ago' +%s)000&endTime=$(date +%s)000"
```

## Scaling and Load Testing

### Scale Agents

```bash
# Scale to 3 agents
docker-compose -f docker/docker-compose.yml up -d --scale ambari-agent=3

# Verify all agents are registered
curl -u admin:admin http://localhost:8080/api/v1/hosts | jq '.items[].Hosts.host_name'
```

### Scale DataNodes

```bash
# Scale to 3 DataNodes
docker-compose -f docker/docker-compose.yml up -d --scale datanode=3

# Check DataNode status
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfsadmin -report
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Services Won't Start

```bash
# Check logs for specific service
docker-compose -f docker/docker-compose.yml logs [service-name]

# Check resource usage
docker stats

# Restart specific service
docker-compose -f docker/docker-compose.yml restart [service-name]
```

#### 2. Database Connection Issues

```bash
# Check database status
docker-compose -f docker/docker-compose.yml exec ambari-db pg_isready -U ambari

# Reset database
docker-compose -f docker/docker-compose.yml down
docker volume rm ambari_ambari-db-data
docker-compose -f docker/docker-compose.yml up -d ambari-db
```

#### 3. Port Conflicts

```bash
# Check what's using ports
lsof -i :8080
lsof -i :5432

# Modify ports in docker-compose.yml if needed
# Example: Change "8080:8080" to "8081:8080"
```

#### 4. Memory Issues

```bash
# Check Docker memory limit
docker system info | grep Memory

# Reduce JVM heap sizes in docker-compose.yml
# Example: Change JAVA_OPTS from -Xmx4G to -Xmx2G
```

### Health Check Commands

```bash
# Manual health checks
docker-compose -f docker/docker-compose.yml exec ambari-server /usr/local/bin/health-check.sh server
docker-compose -f docker/docker-compose.yml exec ambari-agent /usr/local/bin/health-check.sh agent
docker-compose -f docker/docker-compose.yml exec ambari-metrics /usr/local/bin/health-check.sh metrics

# Check all container health
docker-compose -f docker/docker-compose.yml ps
```

### Log Analysis

```bash
# View all logs
docker-compose -f docker/docker-compose.yml logs

# Follow specific service logs
docker-compose -f docker/docker-compose.yml logs -f ambari-server

# Search for errors
docker-compose -f docker/docker-compose.yml logs | grep -i error

# Check last 100 lines
docker-compose -f docker/docker-compose.yml logs --tail=100 ambari-server
```

## Performance Monitoring

### Resource Usage

```bash
# Monitor container resource usage
docker stats

# Check disk usage
docker system df

# Monitor network traffic
docker-compose -f docker/docker-compose.yml exec ambari-server netstat -tuln
```

### Metrics Dashboard

1. Access Grafana at http://localhost:3000
2. Login with admin/admin
3. Import Ambari dashboards from `/docker/config/grafana/dashboards/`
4. Monitor cluster metrics in real-time

## Development Workflow

### Live Development

```bash
# Start development environment
docker-compose -f docker/docker-compose.dev.yml up -d

# Enter development container
docker-compose -f docker/docker-compose.dev.yml exec ambari-server bash

# Build and test changes
cd /workspace
mvn clean install -DskipTests

# Restart services after changes
docker-compose -f docker/docker-compose.dev.yml restart ambari-server
```

### Debugging

```bash
# Enable debug mode
export AMBARI_DEBUG=true
docker-compose -f docker/docker-compose.yml up -d

# Connect debugger to:
# - Server: localhost:5005
# - Agent: localhost:5006

# Access container shell
docker-compose -f docker/docker-compose.yml exec ambari-server bash
```

## Cleanup

### Stop Services

```bash
# Stop all services
docker-compose -f docker/docker-compose.yml down

# Stop and remove volumes (WARNING: This deletes all data)
docker-compose -f docker/docker-compose.yml down -v
```

### Clean Up Images

```bash
# Remove Ambari images
docker images | grep ambari | awk '{print $3}' | xargs docker rmi

# Clean up system
docker system prune -a
```

## Success Criteria

Your Ambari Docker setup is successfully verified when:

1. ✅ All containers are running and healthy
2. ✅ Ambari Web UI is accessible at http://localhost:8080
3. ✅ Database connection is working
4. ✅ At least one agent is registered and heartbeating
5. ✅ Metrics collector is receiving data
6. ✅ Grafana dashboards are displaying metrics
7. ✅ Hadoop cluster (NameNode, DataNodes, ResourceManager) is operational
8. ✅ API endpoints are responding correctly

## Next Steps

After successful verification:

1. **Cluster Setup**: Use Ambari Web UI to install Hadoop services
2. **Security**: Configure Kerberos and SSL/TLS for production
3. **Monitoring**: Set up alerts and monitoring dashboards
4. **Backup**: Configure database and HDFS backups
5. **Scaling**: Add more agents and DataNodes as needed

## Getting Help

- **Documentation**: [Apache Ambari Docs](https://ambari.apache.org/)
- **Issues**: [GitHub Issues](https://github.com/apache/ambari/issues)
- **Community**: [Apache Ambari Mailing Lists](https://ambari.apache.org/mail-lists.html)
- **Docker Logs**: `docker-compose -f docker/docker-compose.yml logs`
