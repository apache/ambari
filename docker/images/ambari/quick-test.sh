#!/bin/bash
#
# Quick Test Script for Apache Ambari Docker Setup
# This script performs basic verification of the Ambari Docker environment
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
    log_info "=== Apache Ambari Docker Quick Test ==="
    log_info "Starting basic verification tests..."
    
    # Test 1: Check Docker is running
    run_test "Docker daemon running" "docker info"
    
    # Test 2: Check Docker Compose is available
    run_test "Docker Compose available" "docker-compose --version"
    
    # Test 3: Check if we're in the right directory
    run_test "In Ambari project directory" "test -f pom.xml && test -d docker"
    
    # Test 4: Check if images exist
    run_test "Ambari server image exists" "docker images | grep -q 'apache/ambari-server'"
    run_test "Ambari agent image exists" "docker images | grep -q 'apache/ambari-agent'"
    run_test "Ambari metrics image exists" "docker images | grep -q 'apache/ambari-metrics'"
    
    # Test 5: Check if containers are running
    run_test "Ambari server container running" "docker-compose -f docker/docker-compose.yml ps | grep -q 'ambari-server.*Up'"
    run_test "Ambari database container running" "docker-compose -f docker/docker-compose.yml ps | grep -q 'ambari-database.*Up'"
    run_test "Zookeeper container running" "docker-compose -f docker/docker-compose.yml ps | grep -q 'ambari-zookeeper.*Up'"
    
    # Test 6: Check HTTP endpoints
    run_test "Ambari Web UI accessible" "check_http 'http://localhost:8080'"
    run_test "Grafana accessible" "check_http 'http://localhost:3000'"
    run_test "NameNode UI accessible" "check_http 'http://localhost:9870'"
    run_test "ResourceManager UI accessible" "check_http 'http://localhost:8088'"
    
    # Test 7: Check API endpoints
    run_test "Ambari API responding" "curl -s -u admin:admin http://localhost:8080/api/v1/version | grep -q 'version'"
    run_test "Metrics API responding" "curl -s http://localhost:6188/ws/v1/timeline/metrics/metadata | grep -q 'metadata'"
    run_test "Grafana API responding" "curl -s http://localhost:3000/api/health | grep -q 'database'"
    
    # Test 8: Check database connectivity
    run_test "Database accessible" "docker-compose -f docker/docker-compose.yml exec -T ambari-db pg_isready -U ambari"
    
    # Test 9: Check Zookeeper
    run_test "Zookeeper accessible" "echo 'ruok' | nc localhost 2181 | grep -q 'imok'"
    
    # Test 10: Check agent registration
    run_test "Agent registered" "curl -s -u admin:admin http://localhost:8080/api/v1/hosts | grep -q 'host_name'"
    
    # Summary
    log_info "=== Test Results ==="
    log_info "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
    log_info "Tests Failed: ${RED}$TESTS_FAILED${NC}"
    
    if [[ $TESTS_FAILED -eq 0 ]]; then
        log_info "${GREEN}🎉 All tests passed! Ambari Docker setup is working correctly.${NC}"
        log_info ""
        log_info "Next steps:"
        log_info "1. Access Ambari Web UI: http://localhost:8080 (admin/admin)"
        log_info "2. Access Grafana: http://localhost:3000 (admin/admin)"
        log_info "3. Check Hadoop NameNode: http://localhost:9870"
        log_info "4. Check YARN ResourceManager: http://localhost:8088"
        return 0
    else
        log_error "❌ Some tests failed. Please check the logs and troubleshoot."
        log_info ""
        log_info "Troubleshooting steps:"
        log_info "1. Check container logs: docker-compose -f docker/docker-compose.yml logs"
        log_info "2. Check container status: docker-compose -f docker/docker-compose.yml ps"
        log_info "3. Restart services: docker-compose -f docker/docker-compose.yml restart"
        log_info "4. See full testing guide: docker/TESTING.md"
        return 1
    fi
}

# Help function
show_help() {
    cat << EOF
Apache Ambari Docker Quick Test Script

Usage: $0 [OPTIONS]

Options:
  -h, --help     Show this help message
  -v, --verbose  Enable verbose output
  --build        Build images before testing
  --start        Start services before testing
  --stop         Stop services after testing

Examples:
  $0                    # Run basic tests
  $0 --build --start    # Build images, start services, then test
  $0 -v                 # Run tests with verbose output

EOF
}

# Parse command line arguments
VERBOSE=false
BUILD_IMAGES=false
START_SERVICES=false
STOP_SERVICES=false

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
        --build)
            BUILD_IMAGES=true
            shift
            ;;
        --start)
            START_SERVICES=true
            shift
            ;;
        --stop)
            STOP_SERVICES=true
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

# Build images if requested
if [[ "$BUILD_IMAGES" == "true" ]]; then
    log_info "Building Docker images..."
    ./docker/build.sh all
fi

# Start services if requested
if [[ "$START_SERVICES" == "true" ]]; then
    log_info "Starting Docker services..."
    docker-compose -f docker/docker-compose.yml up -d
    
    log_info "Waiting for services to start (60 seconds)..."
    sleep 60
fi

# Run main tests
main
exit_code=$?

# Stop services if requested
if [[ "$STOP_SERVICES" == "true" ]]; then
    log_info "Stopping Docker services..."
    docker-compose -f docker/docker-compose.yml down
fi

exit $exit_code
