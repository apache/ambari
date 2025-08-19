#!/bin/bash
#
# Simple Test Script for Apache Ambari Docker Infrastructure
# This script tests the supporting infrastructure without building Ambari from source
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

log_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to run test
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    log_test "Running: $test_name"
    
    if eval "$test_command" >/dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} PASSED: $test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "  ${RED}✗${NC} FAILED: $test_name"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# Function to check HTTP endpoint
check_http() {
    local url="$1"
    local expected_status="${2:-200}"
    
    local status=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    
    if [[ "$status" == "$expected_status" ]]; then
        return 0
    else
        return 1
    fi
}

# Main test function
main() {
    log_info "=== Apache Ambari Docker Infrastructure Test ==="
    log_info "Testing supporting infrastructure components..."
    
    # Test 1: Check Docker is running
    run_test "Docker daemon running" "docker info"
    
    # Test 2: Check Docker Compose is available
    run_test "Docker Compose available" "docker-compose --version"
    
    # Test 3: Check if we're in the right directory
    run_test "In Ambari project directory" "test -f pom.xml && test -d docker"
    
    # Test 4: Start infrastructure services
    log_info "Starting infrastructure services..."
    docker-compose -f docker/docker-compose.simple.yml up -d
    
    # Wait for services to start
    log_info "Waiting for services to start (60 seconds)..."
    sleep 60
    
    # Test 5: Check if containers are running
    run_test "Database container running" "docker-compose -f docker/docker-compose.simple.yml ps | grep -q 'ambari-database.*Up'"
    run_test "Zookeeper container running" "docker-compose -f docker/docker-compose.simple.yml ps | grep -q 'ambari-zookeeper.*Up'"
    run_test "NameNode container running" "docker-compose -f docker/docker-compose.simple.yml ps | grep -q 'hadoop-namenode.*Up'"
    run_test "DataNode container running" "docker-compose -f docker/docker-compose.simple.yml ps | grep -q 'hadoop-datanode.*Up'"
    run_test "ResourceManager container running" "docker-compose -f docker/docker-compose.simple.yml ps | grep -q 'hadoop-resourcemanager.*Up'"
    run_test "Grafana container running" "docker-compose -f docker/docker-compose.simple.yml ps | grep -q 'ambari-grafana.*Up'"
    
    # Test 6: Check HTTP endpoints
    run_test "Grafana accessible" "check_http 'http://localhost:3001'"
    run_test "NameNode UI accessible" "check_http 'http://localhost:9870'"
    run_test "DataNode UI accessible" "check_http 'http://localhost:9864'"
    run_test "ResourceManager UI accessible" "check_http 'http://localhost:8088'"
    
    # Test 7: Check API endpoints
    run_test "Grafana API responding" "curl -s http://localhost:3001/api/health | grep -q 'database'"
    
    # Test 8: Check database connectivity
    run_test "Database accessible" "docker-compose -f docker/docker-compose.simple.yml exec -T ambari-db pg_isready -U ambari"
    
    # Test 9: Check Zookeeper
    run_test "Zookeeper accessible" "echo 'ruok' | nc localhost 2181 | grep -q 'imok'"
    
    # Test 10: Check HDFS operations
    run_test "HDFS operations working" "docker-compose -f docker/docker-compose.simple.yml exec namenode hdfs dfs -mkdir -p /test && docker-compose -f docker/docker-compose.simple.yml exec namenode hdfs dfs -ls / | grep -q test"
    
    # Summary
    log_info "=== Test Results ==="
    log_info "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
    log_info "Tests Failed: ${RED}$TESTS_FAILED${NC}"
    
    if [[ $TESTS_FAILED -eq 0 ]]; then
        log_info "${GREEN}🎉 All infrastructure tests passed!${NC}"
        log_info ""
        log_info "Infrastructure is ready. You can now:"
        log_info "1. Access Grafana: http://localhost:3001 (admin/admin)"
        log_info "2. Check Hadoop NameNode: http://localhost:9870"
        log_info "3. Check DataNode: http://localhost:9864"
        log_info "4. Check YARN ResourceManager: http://localhost:8088"
        log_info "5. Connect to database: docker-compose -f docker/docker-compose.simple.yml exec ambari-db psql -U ambari -d ambari"
        log_info ""
        log_info "To stop services: docker-compose -f docker/docker-compose.simple.yml down"
        return 0
    else
        log_error "❌ Some tests failed. Please check the logs and troubleshoot."
        log_info ""
        log_info "Troubleshooting steps:"
        log_info "1. Check container logs: docker-compose -f docker/docker-compose.simple.yml logs"
        log_info "2. Check container status: docker-compose -f docker/docker-compose.simple.yml ps"
        log_info "3. Restart services: docker-compose -f docker/docker-compose.simple.yml restart"
        return 1
    fi
}

# Help function
show_help() {
    cat << EOF
Apache Ambari Docker Infrastructure Test Script

Usage: $0 [OPTIONS]

Options:
  -h, --help     Show this help message
  -v, --verbose  Enable verbose output
  --stop         Stop services after testing
  --clean        Clean up volumes after testing

Examples:
  $0                    # Run infrastructure tests
  $0 --stop             # Run tests and stop services
  $0 --clean            # Run tests and clean up everything

EOF
}

# Parse command line arguments
VERBOSE=false
STOP_SERVICES=false
CLEAN_UP=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --stop)
            STOP_SERVICES=true
            shift
            ;;
        --clean)
            CLEAN_UP=true
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Enable verbose mode if requested
if [[ "$VERBOSE" == "true" ]]; then
    set -x
fi

# Run main tests
main
exit_code=$?

# Stop services if requested
if [[ "$STOP_SERVICES" == "true" ]]; then
    log_info "Stopping Docker services..."
    docker-compose -f docker/docker-compose.simple.yml down
fi

# Clean up if requested
if [[ "$CLEAN_UP" == "true" ]]; then
    log_info "Cleaning up volumes and networks..."
    docker-compose -f docker/docker-compose.simple.yml down -v
    docker system prune -f
fi

exit $exit_code
