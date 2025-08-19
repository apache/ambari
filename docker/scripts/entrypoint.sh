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
export AMBARI_USER=${AMBARI_USER:-ambari}
export AMBARI_HOME=${AMBARI_HOME:-/opt/ambari}
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-11-openjdk-amd64}
export MAVEN_HOME=${MAVEN_HOME:-/opt/maven}
export PATH=${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}

# Database configuration
export AMBARI_DB_HOST=${AMBARI_DB_HOST:-ambari-db}
export AMBARI_DB_PORT=${AMBARI_DB_PORT:-5432}
export AMBARI_DB_NAME=${AMBARI_DB_NAME:-ambari}
export AMBARI_DB_USER=${AMBARI_DB_USER:-ambari}
export AMBARI_DB_PASSWORD=${AMBARI_DB_PASSWORD:-bigdata}

# Server configuration
export AMBARI_SERVER_HOST=${AMBARI_SERVER_HOST:-localhost}
export AMBARI_SERVER_PORT=${AMBARI_SERVER_PORT:-8080}
export AMBARI_SERVER_HTTPS=${AMBARI_SERVER_HTTPS:-false}

# Agent configuration
export AGENT_HOSTNAME=${AGENT_HOSTNAME:-$(hostname -f)}

# Metrics configuration
export AMS_HBASE_ROOTDIR=${AMS_HBASE_ROOTDIR:-/var/lib/ambari-metrics-collector/hbase}
export AMS_COLLECTOR_PORT=${AMS_COLLECTOR_PORT:-6188}
export AMS_GRAFANA_ENABLED=${AMS_GRAFANA_ENABLED:-true}

# JVM configuration
export JAVA_OPTS=${JAVA_OPTS:-"-Xmx2g -Xms1g -XX:+UseG1GC"}
export AMS_JAVA_OPTS=${AMS_JAVA_OPTS:-"-Xmx1g -Xms512m"}

# Debug configuration
if [[ "${AMBARI_DEBUG:-false}" == "true" ]]; then
    export JAVA_OPTS="${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    log_info "Debug mode enabled. Java debug port: 5005"
fi

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

# Function to wait for database
wait_for_database() {
    if [[ "${AMBARI_DB_HOST}" != "localhost" && "${AMBARI_DB_HOST}" != "127.0.0.1" ]]; then
        wait_for_service "${AMBARI_DB_HOST}" "${AMBARI_DB_PORT}" "Database" 120
    fi
}

# Function to setup directories and permissions
setup_directories() {
    log_info "Setting up directories and permissions..."
    
    # Ensure directories exist and have correct permissions
    local dirs=(
        "/var/log/ambari-server"
        "/var/log/ambari-agent"
        "/var/log/ambari-metrics-collector"
        "/var/log/ambari-metrics-monitor"
        "/var/run/ambari-server"
        "/var/run/ambari-agent"
        "/var/run/ambari-metrics-collector"
        "/var/run/ambari-metrics-monitor"
        "/var/lib/ambari-server"
        "/var/lib/ambari-agent"
        "/var/lib/ambari-metrics-collector"
        "/etc/ambari-server/conf"
        "/etc/ambari-agent/conf"
        "/etc/ambari-metrics-collector/conf"
        "/etc/ambari-metrics-monitor/conf"
    )
    
    for dir in "${dirs[@]}"; do
        if [[ ! -d "${dir}" ]]; then
            sudo mkdir -p "${dir}"
        fi
        sudo chown -R ${AMBARI_USER}:${AMBARI_USER} "${dir}"
    done
}

# Function to setup SSH
setup_ssh() {
    log_info "Setting up SSH configuration..."
    
    # Start SSH daemon if not running
    if ! pgrep -x "sshd" > /dev/null; then
        sudo service ssh start || sudo /usr/sbin/sshd
    fi
    
    # Ensure SSH keys are properly configured
    if [[ ! -f "/home/${AMBARI_USER}/.ssh/id_rsa" ]]; then
        ssh-keygen -t rsa -N '' -f "/home/${AMBARI_USER}/.ssh/id_rsa"
        cat "/home/${AMBARI_USER}/.ssh/id_rsa.pub" >> "/home/${AMBARI_USER}/.ssh/authorized_keys"
        chmod 600 "/home/${AMBARI_USER}/.ssh/authorized_keys"
        chmod 700 "/home/${AMBARI_USER}/.ssh"
    fi
}

# Function to setup time synchronization
setup_time_sync() {
    log_info "Setting up time synchronization..."
    
    # Start NTP service if available
    if command -v ntpd >/dev/null 2>&1; then
        sudo service ntp start || true
    fi
    
    # Sync time once
    sudo ntpdate -s time.nist.gov || true
}

# Function to display environment info
display_environment_info() {
    log_info "=== Ambari Docker Environment ==="
    log_info "Java Version: $(java -version 2>&1 | head -n 1)"
    log_info "Maven Version: $(mvn -version 2>&1 | head -n 1)"
    log_info "Node.js Version: $(node --version 2>/dev/null || echo 'Not installed')"
    log_info "Python Version: $(python3 --version)"
    log_info "User: $(whoami)"
    log_info "Working Directory: $(pwd)"
    log_info "JAVA_HOME: ${JAVA_HOME}"
    log_info "MAVEN_HOME: ${MAVEN_HOME}"
    log_info "================================="
}

# Function to handle signals
cleanup() {
    log_info "Received shutdown signal, cleaning up..."
    
    # Stop any running Ambari services
    if pgrep -f "ambari-server" > /dev/null; then
        log_info "Stopping Ambari Server..."
        sudo ambari-server stop || true
    fi
    
    if pgrep -f "ambari-agent" > /dev/null; then
        log_info "Stopping Ambari Agent..."
        sudo ambari-agent stop || true
    fi
    
    if pgrep -f "ambari-metrics-collector" > /dev/null; then
        log_info "Stopping Ambari Metrics Collector..."
        sudo ambari-metrics-collector stop || true
    fi
    
    if pgrep -f "ambari-metrics-monitor" > /dev/null; then
        log_info "Stopping Ambari Metrics Monitor..."
        sudo ambari-metrics-monitor stop || true
    fi
    
    log_info "Cleanup completed"
    exit 0
}

# Set up signal handlers
trap cleanup SIGTERM SIGINT SIGQUIT

# Main initialization
main() {
    log_info "Starting Ambari Docker container initialization..."
    
    # Display environment information
    display_environment_info
    
    # Setup basic requirements
    setup_directories
    setup_ssh
    setup_time_sync
    
    # Wait for dependencies if needed
    if [[ "${1}" == "ambari-server" || "${1}" == "server" ]]; then
        wait_for_database
    elif [[ "${1}" == "ambari-agent" || "${1}" == "agent" ]]; then
        wait_for_service "${AMBARI_SERVER_HOST}" "${AMBARI_SERVER_PORT}" "Ambari Server" 300
    fi
    
    log_info "Container initialization completed successfully"
    
    # Execute the command
    if [[ $# -eq 0 ]]; then
        log_info "No command specified, starting interactive shell"
        exec /bin/bash
    else
        log_info "Executing command: $*"
        exec "$@"
    fi
}

# Run main function
main "$@"
