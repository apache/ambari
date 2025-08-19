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

# Apache Ambari Admin Script
# Similar to Pinot's pinot-admin.sh - main entry point for Ambari Docker containers

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    if [[ "${AMBARI_DEBUG:-false}" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Default environment variables
export AMBARI_HOME=${AMBARI_HOME:-/opt/ambari}
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}
export PATH=${JAVA_HOME}/bin:${AMBARI_HOME}/bin:${PATH}

# Database configuration
export AMBARI_DB_HOST=${AMBARI_DB_HOST:-ambari-database}
export AMBARI_DB_PORT=${AMBARI_DB_PORT:-5432}
export AMBARI_DB_NAME=${AMBARI_DB_NAME:-ambari}
export AMBARI_DB_USER=${AMBARI_DB_USER:-ambari}
export AMBARI_DB_PASSWORD=${AMBARI_DB_PASSWORD:-bigdata}

# Server configuration
export AMBARI_SERVER_HOST=${AMBARI_SERVER_HOST:-localhost}
export AMBARI_SERVER_PORT=${AMBARI_SERVER_PORT:-8080}

# Function to show usage
show_usage() {
    cat << EOF
Apache Ambari Admin Script

Usage: $0 [COMMAND] [OPTIONS]

Available Commands:
  StartServer         Start Ambari Server
  StartAgent          Start Ambari Agent
  StartMetrics        Start Ambari Metrics Collector
  QuickStart          Start Ambari in quick start mode
  
Server Commands:
  SetupServer         Setup Ambari Server database
  ResetServer         Reset Ambari Server
  UpgradeServer       Upgrade Ambari Server
  
Agent Commands:
  RegisterAgent       Register agent with server
  
Utility Commands:
  CheckHealth         Check component health
  ShowVersion         Show Ambari version
  ShowConfig          Show current configuration
  
Options:
  -h, --help          Show this help message
  -v, --verbose       Enable verbose output
  --debug             Enable debug mode

Examples:
  # Start Ambari Server
  $0 StartServer
  
  # Start Ambari Agent connecting to specific server
  $0 StartAgent -server ambari-server
  
  # Start Metrics Collector
  $0 StartMetrics
  
  # Quick start with all components
  $0 QuickStart
  
  # Setup server with custom database
  $0 SetupServer --database postgres --host db.example.com

Environment Variables:
  AMBARI_HOME             Ambari installation directory
  JAVA_HOME               Java installation directory
  AMBARI_DB_HOST          Database hostname
  AMBARI_DB_PORT          Database port
  AMBARI_DB_NAME          Database name
  AMBARI_DB_USER          Database username
  AMBARI_DB_PASSWORD      Database password
  AMBARI_SERVER_HOST      Server hostname (for agents)
  AMBARI_SERVER_PORT      Server port (for agents)
  AMBARI_DEBUG            Enable debug output

EOF
}

# Function to wait for service
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local timeout=${4:-60}
    
    log_info "Waiting for ${service_name} at ${host}:${port}..."
    
    local count=0
    while ! nc -z "${host}" "${port}" >/dev/null 2>&1; do
        if [ ${count} -ge ${timeout} ]; then
            log_error "Timeout waiting for ${service_name} at ${host}:${port}"
            return 1
        fi
        count=$((count + 1))
        sleep 1
    done
    
    log_info "${service_name} is available at ${host}:${port}"
    return 0
}

# Function to setup Ambari Server
setup_server() {
    log_info "Setting up Ambari Server..."
    
    # Wait for database
    wait_for_service "${AMBARI_DB_HOST}" "${AMBARI_DB_PORT}" "Database" 120
    
    # Setup database if not already done
    if [ ! -f /var/lib/ambari-server/.db-setup-done ]; then
        log_info "Setting up Ambari database..."
        ambari-server setup --database=postgres \
            --databasehost="${AMBARI_DB_HOST}" \
            --databaseport="${AMBARI_DB_PORT}" \
            --databasename="${AMBARI_DB_NAME}" \
            --databaseusername="${AMBARI_DB_USER}" \
            --databasepassword="${AMBARI_DB_PASSWORD}" \
            --silent
        
        touch /var/lib/ambari-server/.db-setup-done
        log_info "Database setup completed!"
    else
        log_info "Database already setup, skipping..."
    fi
}

# Function to start Ambari Server
start_server() {
    log_info "Starting Ambari Server..."
    
    # Setup server first
    setup_server
    
    # Start server
    log_info "Starting Ambari Server process..."
    exec ambari-server start --debug="${AMBARI_DEBUG:-false}"
}

# Function to start Ambari Agent
start_agent() {
    local server_host="${AMBARI_SERVER_HOST}"
    
    # Parse additional arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -server|--server)
                server_host="$2"
                shift 2
                ;;
            *)
                shift
                ;;
        esac
    done
    
    log_info "Starting Ambari Agent..."
    log_info "Server: ${server_host}:${AMBARI_SERVER_PORT}"
    
    # Wait for server
    wait_for_service "${server_host}" "${AMBARI_SERVER_PORT}" "Ambari Server" 300
    
    # Configure agent
    if [ ! -f /etc/ambari-agent/conf/ambari-agent.ini ]; then
        log_info "Configuring Ambari Agent..."
        mkdir -p /etc/ambari-agent/conf
        cat > /etc/ambari-agent/conf/ambari-agent.ini << EOF
[server]
hostname=${server_host}
url_port=${AMBARI_SERVER_PORT}
secured_url_port=8441

[agent]
prefix=/var/lib/ambari-agent/data
cache_dir=/var/lib/ambari-agent/cache
log_dir=/var/log/ambari-agent
run_dir=/var/run/ambari-agent
EOF
    fi
    
    # Start agent
    log_info "Starting Ambari Agent process..."
    exec ambari-agent start --debug="${AMBARI_DEBUG:-false}"
}

# Function to start Ambari Metrics
start_metrics() {
    log_info "Starting Ambari Metrics Collector..."
    
    # Configure metrics collector
    if [ ! -f /etc/ambari-metrics-collector/conf/ams-site.xml ]; then
        log_info "Configuring Ambari Metrics Collector..."
        mkdir -p /etc/ambari-metrics-collector/conf
        # Add basic configuration here
    fi
    
    # Start metrics collector
    log_info "Starting Ambari Metrics Collector process..."
    exec ambari-metrics-collector start
}

# Function for quick start
quick_start() {
    log_info "Starting Ambari Quick Start..."
    
    # Start server in background
    start_server &
    SERVER_PID=$!
    
    # Wait a bit for server to start
    sleep 30
    
    # Start agent
    start_agent &
    AGENT_PID=$!
    
    # Wait for both processes
    wait $SERVER_PID $AGENT_PID
}

# Function to check health
check_health() {
    log_info "Checking Ambari component health..."
    
    if command -v /usr/local/bin/health-check.sh >/dev/null 2>&1; then
        /usr/local/bin/health-check.sh
    else
        log_warn "Health check script not found"
        return 1
    fi
}

# Function to show version
show_version() {
    log_info "Apache Ambari Version Information"
    
    if command -v ambari-server >/dev/null 2>&1; then
        ambari-server --version 2>/dev/null || echo "Ambari Server: Unknown version"
    fi
    
    if command -v ambari-agent >/dev/null 2>&1; then
        ambari-agent --version 2>/dev/null || echo "Ambari Agent: Unknown version"
    fi
    
    echo "Java Version: $(java -version 2>&1 | head -n 1)"
    echo "Container: $(hostname)"
}

# Function to show configuration
show_config() {
    log_info "Current Ambari Configuration"
    echo "AMBARI_HOME: ${AMBARI_HOME}"
    echo "JAVA_HOME: ${JAVA_HOME}"
    echo "Database Host: ${AMBARI_DB_HOST}"
    echo "Database Port: ${AMBARI_DB_PORT}"
    echo "Database Name: ${AMBARI_DB_NAME}"
    echo "Database User: ${AMBARI_DB_USER}"
    echo "Server Host: ${AMBARI_SERVER_HOST}"
    echo "Server Port: ${AMBARI_SERVER_PORT}"
    echo "Debug Mode: ${AMBARI_DEBUG:-false}"
}

# Main function
main() {
    local command="${1:-}"
    
    # Parse global options
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -v|--verbose)
                export AMBARI_DEBUG=true
                shift
                ;;
            --debug)
                export AMBARI_DEBUG=true
                shift
                ;;
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [[ -z "$command" ]]; then
                    command="$1"
                fi
                shift
                ;;
        esac
    done
    
    # Execute command
    case "$command" in
        StartServer)
            start_server "$@"
            ;;
        StartAgent)
            start_agent "$@"
            ;;
        StartMetrics)
            start_metrics "$@"
            ;;
        QuickStart)
            quick_start "$@"
            ;;
        SetupServer)
            setup_server "$@"
            ;;
        CheckHealth)
            check_health "$@"
            ;;
        ShowVersion)
            show_version "$@"
            ;;
        ShowConfig)
            show_config "$@"
            ;;
        "")
            log_info "No command specified. Starting interactive shell..."
            exec /bin/bash
            ;;
        *)
            log_error "Unknown command: $command"
            show_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
