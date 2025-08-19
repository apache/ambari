# Apache Ambari Docker Testing Guide

This guide provides complete step-by-step testing instructions for the improved Ambari Docker setup.

## Prerequisites

Before testing, ensure you have:
- Docker 20.10+ installed
- Docker Compose 2.0+ installed
- At least 16GB RAM available
- At least 50GB free disk space
- Git (to clone the repository)

## Step 1: Environment Setup

```bash
# Check Docker installation
docker --version
docker-compose --version

# Check system resources
docker system info | grep -E "CPUs|Total Memory"
df -h  # Check disk space

# Navigate to Ambari project directory
cd /path/to/ambari  # Replace with your actual path
pwd  # Should show the ambari directory
```

## Step 2: Build Docker Images

```bash
# Make build script executable
chmod +x docker/build.sh

# Build all images (this will take 15-30 minutes)
./docker/build.sh all

# Verify images are created
docker images | grep ambari

# Expected output should show:
# apache/ambari-server    latest
# apache/ambari-agent     latest  
# apache/ambari-metrics   latest
# apache/ambari-base      latest
```

## Step 3: Start the Complete Stack

```bash
# Start all services in detached mode
docker-compose -f docker/docker-compose.yml up -d

# Wait for services to start (2-5 minutes)
# Monitor the startup process
docker-compose -f docker/docker-compose.yml logs -f

# Check all containers are running
docker-compose -f docker/docker-compose.yml ps

# Expected output: All services should show "Up" status
```

## Step 4: Basic Health Verification

```bash
# Wait for all services to be healthy (may take 5-10 minutes)
watch docker-compose -f docker/docker-compose.yml ps

# Check individual service health
docker-compose -f docker/docker-compose.yml exec ambari-server /usr/local/bin/health-check.sh server
docker-compose -f docker/docker-compose.yml exec ambari-agent /usr/local/bin/health-check.sh agent
docker-compose -f docker/docker-compose.yml exec ambari-metrics /usr/local/bin/health-check.sh metrics

# All health checks should return "Status: HEALTHY"
```

## Step 5: Web Interface Testing

### Test Ambari Web UI
```bash
# Open Ambari Web UI
open http://localhost:8080
# Or use: curl -I http://localhost:8080

# Login credentials:
# Username: admin
# Password: admin

# You should see the Ambari welcome screen
```

### Test Grafana Dashboard
```bash
# Open Grafana
open http://localhost:3000
# Or use: curl -I http://localhost:3000

# Login credentials:
# Username: admin  
# Password: admin

# You should see the Grafana dashboard
```

### Test Hadoop Web UIs
```bash
# Test NameNode UI
curl -I http://localhost:9870
open http://localhost:9870

# Test ResourceManager UI  
curl -I http://localhost:8088
open http://localhost:8088

# Both should return HTTP 200 OK
```

## Step 6: API Testing

```bash
# Test Ambari Server API
curl -u admin:admin http://localhost:8080/api/v1/clusters
# Expected: JSON response with cluster information

# Test Ambari version info
curl -u admin:admin http://localhost:8080/api/v1/version
# Expected: JSON with version details

# Test Metrics Collector API
curl http://localhost:6188/ws/v1/timeline/metrics/metadata
# Expected: JSON response with metrics metadata

# Test Grafana API
curl http://localhost:3000/api/health
# Expected: {"commit":"","database":"ok","version":""}
```

## Step 7: Database Testing

```bash
# Connect to PostgreSQL database
docker-compose -f docker/docker-compose.yml exec ambari-db psql -U ambari -d ambari

# Run these SQL commands:
\dt                                    # List tables
SELECT COUNT(*) FROM users;           # Should return 1 (admin user)
SELECT user_name FROM users;          # Should show 'admin'
\q                                    # Exit

# Test database connectivity from server
docker-compose -f docker/docker-compose.yml exec ambari-server pg_isready -h ambari-db -p 5432 -U ambari
# Expected: ambari-db:5432 - accepting connections
```

## Step 8: Zookeeper Testing

```bash
# Check Zookeeper status
docker-compose -f docker/docker-compose.yml exec zookeeper zkServer.sh status
# Expected: Mode: standalone

# Connect to Zookeeper CLI
docker-compose -f docker/docker-compose.yml exec zookeeper zkCli.sh -server localhost:2181

# Run these commands in ZK CLI:
ls /                    # List root znodes
create /test "data"     # Create test node
get /test              # Get test node data
delete /test           # Delete test node
quit                   # Exit ZK CLI
```

## Step 9: Hadoop Cluster Testing

```bash
# Check HDFS status
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfsadmin -report
# Expected: Shows DataNode information

# Test HDFS operations
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfs -mkdir /test
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfs -ls /
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfs -put /etc/hosts /test/
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfs -cat /test/hosts
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfs -rm -r /test

# Check YARN nodes
docker-compose -f docker/docker-compose.yml exec resourcemanager yarn node -list
# Expected: Shows active NodeManagers
```

## Step 10: Agent Registration Testing

```bash
# Check agent logs for registration
docker-compose -f docker/docker-compose.yml logs ambari-agent | grep -i "registration"

# Verify agent is registered in Ambari
curl -u admin:admin http://localhost:8080/api/v1/hosts
# Expected: JSON showing registered hosts

# Check agent heartbeat
docker-compose -f docker/docker-compose.yml logs ambari-server | grep -i "heartbeat" | tail -5
# Expected: Recent heartbeat messages
```

## Step 11: Metrics Collection Testing

```bash
# Check metrics collector status
curl http://localhost:6188/ws/v1/timeline/metrics/metadata | jq .
# Expected: JSON with metrics metadata

# Verify HBase is running (embedded in metrics collector)
docker-compose -f docker/docker-compose.yml exec ambari-metrics ps aux | grep HMaster
# Expected: HMaster process running

# Test metrics query (wait 5 minutes after startup)
curl "http://localhost:6188/ws/v1/timeline/metrics?metricNames=cpu_user&hostname=ambari-agent&appId=HOST&startTime=$(date -d '1 hour ago' +%s)000&endTime=$(date +%s)000" | jq .
# Expected: JSON with CPU metrics data
```

## Step 12: Scaling Testing

```bash
# Scale agents to 3 instances
docker-compose -f docker/docker-compose.yml up -d --scale ambari-agent=3

# Wait for new agents to register (2-3 minutes)
sleep 180

# Verify all agents are registered
curl -u admin:admin http://localhost:8080/api/v1/hosts | jq '.items[].Hosts.host_name'
# Expected: Should show 3 agent hostnames

# Scale DataNodes to 3 instances
docker-compose -f docker/docker-compose.yml up -d --scale datanode=3

# Check DataNode status
docker-compose -f docker/docker-compose.yml exec namenode hdfs dfsadmin -report
# Expected: Should show 3 DataNodes
```

## Step 13: Performance Testing

```bash
# Monitor resource usage
docker stats --no-stream

# Check container health over time
for i in {1..5}; do
  echo "=== Health Check $i ==="
  docker-compose -f docker/docker-compose.yml ps
  sleep 30
done

# Test API response times
time curl -u admin:admin http://localhost:8080/api/v1/clusters
time curl http://localhost:6188/ws/v1/timeline/metrics/metadata
# Response times should be under 2 seconds
```

## Step 14: Failure Recovery Testing

```bash
# Test container restart
docker-compose -f docker/docker-compose.yml restart ambari-server

# Wait for restart and check health
sleep 60
docker-compose -f docker/docker-compose.yml exec ambari-server /usr/local/bin/health-check.sh server

# Test database recovery
docker-compose -f docker/docker-compose.yml restart ambari-db
sleep 30
docker-compose -f docker/docker-compose.yml exec ambari-db pg_isready -U ambari

# Verify services recover
curl -u admin:admin http://localhost:8080/api/v1/clusters
```

## Step 15: Development Environment Testing

```bash
# Start development environment
docker-compose -f docker/docker-compose.dev.yml up -d

# Enter development container
docker-compose -f docker/docker-compose.dev.yml exec ambari-server bash

# Inside container, test build environment
cd /workspace
mvn --version
java -version
node --version

# Test Maven build (inside container)
mvn clean compile -DskipTests -pl ambari-server

# Exit container
exit

# Stop development environment
docker-compose -f docker/docker-compose.dev.yml down
```

## Expected Test Results

### ✅ Success Criteria

All tests should pass with these results:

1. **Images Built**: 4 Ambari images created successfully
2. **Services Running**: All containers show "Up (healthy)" status
3. **Web UIs Accessible**: 
   - Ambari UI at http://localhost:8080
   - Grafana at http://localhost:3000
   - NameNode UI at http://localhost:9870
   - ResourceManager UI at http://localhost:8088
4. **APIs Responding**: All curl commands return valid JSON
5. **Database Connected**: PostgreSQL accessible with admin user
6. **Zookeeper Working**: Can create/read/delete znodes
7. **HDFS Operational**: Can create directories and files
8. **Agents Registered**: At least 1 agent heartbeating
9. **Metrics Collecting**: Metrics API returns data
10. **Scaling Works**: Can scale agents and DataNodes

### ❌ Troubleshooting Common Issues

#### Issue: Containers not starting
```bash
# Check logs
docker-compose -f docker/docker-compose.yml logs [service-name]

# Check resources
docker system df
docker system info | grep Memory

# Solution: Increase Docker memory or reduce JVM heap sizes
```

#### Issue: Port conflicts
```bash
# Check port usage
lsof -i :8080
lsof -i :5432

# Solution: Change ports in docker-compose.yml
```

#### Issue: Health checks failing
```bash
# Manual health check
docker-compose -f docker/docker-compose.yml exec [service] /usr/local/bin/health-check.sh [component]

# Check service logs
docker-compose -f docker/docker-compose.yml logs [service]

# Solution: Wait longer for startup or check configuration
```

## Cleanup After Testing

```bash
# Stop all services
docker-compose -f docker/docker-compose.yml down

# Remove volumes (WARNING: Deletes all data)
docker-compose -f docker/docker-compose.yml down -v

# Clean up images (optional)
docker images | grep ambari | awk '{print $3}' | xargs docker rmi

# Clean up system
docker system prune -f
```

## Automated Testing Script

Create a test script for automated testing:

```bash
# Create test script
cat > test-ambari-docker.sh << 'EOF'
#!/bin/bash
set -e

echo "=== Starting Ambari Docker Tests ==="

# Build images
echo "Building images..."
./docker/build.sh all

# Start services
echo "Starting services..."
docker-compose -f docker/docker-compose.yml up -d

# Wait for services
echo "Waiting for services to start..."
sleep 300

# Run tests
echo "Testing APIs..."
curl -f -u admin:admin http://localhost:8080/api/v1/clusters
curl -f http://localhost:6188/ws/v1/timeline/metrics/metadata
curl -f http://localhost:3000/api/health

echo "=== All tests passed! ==="
EOF

chmod +x test-ambari-docker.sh
./test-ambari-docker.sh
```

This comprehensive testing guide ensures that all components of the improved Ambari Docker setup are working correctly and provides troubleshooting steps for common issues.
