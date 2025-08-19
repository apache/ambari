#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Health check script for Ambari Docker containers
# This script determines the container type and performs appropriate health checks

set -e

# Configuration
AMBARI_SERVER_PORT=${AMBARI_SERVER_PORT:-8080}
AMBARI_SERVER_SSL_PORT=${AMBARI_SERVER_SSL_PORT:-8441}
AMS_COLLECTOR_PORT=${AMS_COLLECTOR_PORT:-6188}
GRAFANA_PORT=${GRAFANA_PORT:-3000}

# Timeout for HTTP requests (seconds)
HTTP_TIMEOUT=10

# Function to check HTTP endpoint
check_http() {
    local url=$1
    local expected_status=${2:-200}
    local timeout=${3:-$HTTP_TIMEOUT}
    
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$timeout" "$url" 2>/dev/null || echo "000")
    
    if [[ "$response" == "$expected_status" ]]; then
        return 0
    else
        echo "HTTP check failed for $url (got $response, expected $expected_status)"
        return 1
    fi
}

# Function to check if process is running
check_process() {
    local process_name=$1
    if pgrep -f "$process_name" > /dev/null; then
        return 0
    else
        echo "Process check failed: $process_name not running"
        return 1
    fi
}

# Function to check if port is listening
check_port() {
    local port=$1
    local host=${2:-localhost}
    
    if nc -z "$host" "$port" 2>/dev/null; then
        return 0
    else
        echo "Port check failed: $host:$port not accessible"
        return 1
    fi
}

# Function to check file exists and is readable
check_file() {
    local file_path=$1
    if [[ -r "$file_path" ]]; then
        return 0
    else
        echo "File check failed: $file_path not readable"
        return 1
    fi
}

# Function to check Ambari Server health
check_ambari_server() {
    echo "Checking Ambari Server health..."
    
    # Check if server process is running
    if ! check_process "ambari-server"; then
        return 1
    fi
    
    # Check if server port is listening
    if ! check_port "$AMBARI_SERVER_PORT"; then
        return 1
    fi
    
    # Check server API endpoint
    local server_url="http://localhost:$AMBARI_SERVER_PORT/api/v1/clusters"
    if ! check_http "$server_url" "200"; then
        # Try with authentication (default admin:admin)
        server_url="http://admin:admin@localhost:$AMBARI_SERVER_PORT/api/v1/clusters"
        if ! check_http "$server_url" "200"; then
            return 1
        fi
    fi
    
    # Check server log file exists
    if ! check_file "/var/log/ambari-server/ambari-server.log"; then
        return 1
    fi
    
    echo "Ambari Server health check passed"
    return 0
}

# Function to check Ambari Agent health
check_ambari_agent() {
    echo "Checking Ambari Agent health..."
    
    # Check if agent process is running
    if ! check_process "ambari-agent"; then
        return 1
    fi
    
    # Check agent log file exists
    if ! check_file "/var/log/ambari-agent/ambari-agent.log"; then
        return 1
    fi
    
    # Check if agent can communicate with server (if server host is set)
    if [[ -n "${AMBARI_SERVER_HOST:-}" && "${AMBARI_SERVER_HOST}" != "localhost" ]]; then
        if ! check_port "$AMBARI_SERVER_PORT" "$AMBARI_SERVER_HOST"; then
            echo "Warning: Cannot reach Ambari Server at $AMBARI_SERVER_HOST:$AMBARI_SERVER_PORT"
            # Don't fail health check for this, as server might be starting
        fi
    fi
    
    echo "Ambari Agent health check passed"
    return 0
}

# Function to check Ambari Metrics Collector health
check_ambari_metrics() {
    echo "Checking Ambari Metrics Collector health..."
    
    # Check if metrics collector process is running
    if ! check_process "ambari-metrics-collector"; then
        return 1
    fi
    
    # Check if collector port is listening
    if ! check_port "$AMS_COLLECTOR_PORT"; then
        return 1
    fi
    
    # Check metrics API endpoint
    local metrics_url="http://localhost:$AMS_COLLECTOR_PORT/ws/v1/timeline/metrics/metadata"
    if ! check_http "$metrics_url" "200"; then
        return 1
    fi
    
    # Check HBase master process (embedded in metrics collector)
    if ! check_process "HMaster"; then
        echo "Warning: HBase Master process not found"
        # Don't fail for this as it might be starting
    fi
    
    # Check collector log file exists
    if ! check_file "/var/log/ambari-metrics-collector/ambari-metrics-collector.log"; then
        return 1
    fi
    
    echo "Ambari Metrics Collector health check passed"
    return 0
}

# Function to check Grafana health
check_grafana() {
    echo "Checking Grafana health..."
    
    # Check if Grafana process is running
    if ! check_process "grafana-server"; then
        return 1
    fi
    
    # Check if Grafana port is listening
    if ! check_port "$GRAFANA_PORT"; then
        return 1
    fi
    
    # Check Grafana API endpoint
    local grafana_url="http://localhost:$GRAFANA_PORT/api/health"
    if ! check_http "$grafana_url" "200"; then
        return 1
    fi
    
    echo "Grafana health check passed"
    return 0
}

# Function to check database connectivity
check_database() {
    echo "Checking database connectivity..."
    
    local db_host=${AMBARI_DB_HOST:-localhost}
    local db_port=${AMBARI_DB_PORT:-5432}
    local db_name=${AMBARI_DB_NAME:-ambari}
    local db_user=${AMBARI_DB_USER:-ambari}
    
    # Check if database port is accessible
    if ! check_port "$db_port" "$db_host"; then
        return 1
    fi
    
    # Try to connect to database (if PostgreSQL client is available)
    if command -v psql >/dev/null 2>&1; then
        if ! PGPASSWORD="${AMBARI_DB_PASSWORD}" psql -h "$db_host" -p "$db_port" -U "$db_user" -d "$db_name" -c "SELECT 1;" >/dev/null 2>&1; then
            echo "Database connection test failed"
            return 1
        fi
    fi
    
    echo "Database connectivity check passed"
    return 0
}

# Function to perform basic system health checks
check_system() {
    echo "Checking system health..."
    
    # Check disk space (warn if less than 1GB free)
    local free_space
    free_space=$(df / | awk 'NR==2 {print $4}')
    if [[ "$free_space" -lt 1048576 ]]; then  # 1GB in KB
        echo "Warning: Low disk space ($(($free_space / 1024))MB free)"
    fi
    
    # Check memory usage (warn if less than 512MB free)
    local free_memory
    free_memory=$(free | awk 'NR==2{printf "%.0f", $7/1024}')
    if [[ "$free_memory" -lt 512 ]]; then
        echo "Warning: Low memory (${free_memory}MB free)"
    fi
    
    # Check if Java is available
    if ! command -v java >/dev/null 2>&1; then
        echo "Java not found in PATH"
        return 1
    fi
    
    echo "System health check passed"
    return 0
}

# Main health check function
main() {
    local exit_code=0
    
    echo "=== Ambari Docker Health Check ==="
    echo "Timestamp: $(date)"
    echo "Hostname: $(hostname)"
    echo "Container ID: $(hostname)"
    
    # Always check system health
    if ! check_system; then
        exit_code=1
    fi
    
    # Determine container type and run appropriate checks
    # Check based on running processes and environment variables
    
    if pgrep -f "ambari-server" > /dev/null || [[ "${AMBARI_COMPONENT:-}" == "server" ]]; then
        if ! check_ambari_server; then
            exit_code=1
        fi
        
        # If this is a server container, also check database
        if ! check_database; then
            exit_code=1
        fi
    fi
    
    if pgrep -f "ambari-agent" > /dev/null || [[ "${AMBARI_COMPONENT:-}" == "agent" ]]; then
        if ! check_ambari_agent; then
            exit_code=1
        fi
    fi
    
    if pgrep -f "ambari-metrics-collector" > /dev/null || [[ "${AMBARI_COMPONENT:-}" == "metrics" ]]; then
        if ! check_ambari_metrics; then
            exit_code=1
        fi
    fi
    
    if pgrep -f "grafana-server" > /dev/null || [[ "${AMBARI_COMPONENT:-}" == "grafana" ]]; then
        if ! check_grafana; then
            exit_code=1
        fi
    fi
    
    # If no specific component is detected, this might be a base container
    if ! pgrep -f "ambari-" > /dev/null && ! pgrep -f "grafana-server" > /dev/null; then
        echo "No Ambari services detected - assuming base container"
        # For base containers, just check that basic tools are available
        if ! command -v java >/dev/null 2>&1 || ! command -v mvn >/dev/null 2>&1; then
            echo "Basic tools (Java/Maven) not available"
            exit_code=1
        else
            echo "Base container health check passed"
        fi
    fi
    
    echo "=== Health Check Complete ==="
    
    if [[ $exit_code -eq 0 ]]; then
        echo "Status: HEALTHY"
    else
        echo "Status: UNHEALTHY"
    fi
    
    exit $exit_code
}

# Run main function
main "$@"
