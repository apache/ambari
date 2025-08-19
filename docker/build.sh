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

# Apache Ambari Docker Build Script
# This script builds Docker images for Apache Ambari components

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_REGISTRY="apache"
DEFAULT_TAG="latest"
DEFAULT_UBUNTU_VERSION="22.04"
DEFAULT_JAVA_VERSION="11"
DEFAULT_MAVEN_VERSION="3.9.6"
DEFAULT_NODE_VERSION="18"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Available images
AVAILABLE_IMAGES=(
    "base"
    "server"
    "agent"
    "metrics"
    "dev"
    "all"
)

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
    if [[ "${DEBUG:-false}" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Function to show usage
show_usage() {
    cat << EOF
Apache Ambari Docker Build Script

Usage: $0 [OPTIONS] [IMAGE_NAME]

Available Images:
  base      - Base image with common dependencies
  server    - Ambari Server image
  agent     - Ambari Agent image
  metrics   - Ambari Metrics Collector image
  dev       - Development environment image
  all       - Build all images

Options:
  -r, --registry REGISTRY     Docker registry prefix (default: $DEFAULT_REGISTRY)
  -t, --tag TAG              Image tag (default: $DEFAULT_TAG)
  --ubuntu-version VERSION   Ubuntu base image version (default: $DEFAULT_UBUNTU_VERSION)
  --java-version VERSION     Java version (default: $DEFAULT_JAVA_VERSION)
  --maven-version VERSION    Maven version (default: $DEFAULT_MAVEN_VERSION)
  --node-version VERSION     Node.js version (default: $DEFAULT_NODE_VERSION)
  --no-cache                 Build without using cache
  --push                     Push images to registry after building
  --parallel                 Build images in parallel (when building all)
  --target TARGET            Build specific target (runtime, build, development)
  --platform PLATFORM       Target platform (e.g., linux/amd64,linux/arm64)
  -v, --verbose              Enable verbose output
  -h, --help                 Show this help message

Examples:
  $0 base                                    # Build base image
  $0 server --tag 3.0.0                     # Build server image with custom tag
  $0 all --registry myregistry --push       # Build and push all images
  $0 dev --no-cache --verbose               # Build dev image without cache
  $0 server --platform linux/amd64,linux/arm64  # Multi-platform build

Environment Variables:
  DOCKER_REGISTRY           Default registry (overrides -r)
  DOCKER_TAG               Default tag (overrides -t)
  DOCKER_BUILDKIT          Enable BuildKit (recommended: 1)
  DEBUG                    Enable debug output (true/false)

EOF
}

# Function to parse command line arguments
parse_args() {
    REGISTRY="${DOCKER_REGISTRY:-$DEFAULT_REGISTRY}"
    TAG="${DOCKER_TAG:-$DEFAULT_TAG}"
    UBUNTU_VERSION="$DEFAULT_UBUNTU_VERSION"
    JAVA_VERSION="$DEFAULT_JAVA_VERSION"
    MAVEN_VERSION="$DEFAULT_MAVEN_VERSION"
    NODE_VERSION="$DEFAULT_NODE_VERSION"
    NO_CACHE=""
    PUSH=false
    PARALLEL=false
    TARGET=""
    PLATFORM=""
    VERBOSE=false
    IMAGE_NAME=""

    while [[ $# -gt 0 ]]; do
        case $1 in
            -r|--registry)
                REGISTRY="$2"
                shift 2
                ;;
            -t|--tag)
                TAG="$2"
                shift 2
                ;;
            --ubuntu-version)
                UBUNTU_VERSION="$2"
                shift 2
                ;;
            --java-version)
                JAVA_VERSION="$2"
                shift 2
                ;;
            --maven-version)
                MAVEN_VERSION="$2"
                shift 2
                ;;
            --node-version)
                NODE_VERSION="$2"
                shift 2
                ;;
            --no-cache)
                NO_CACHE="--no-cache"
                shift
                ;;
            --push)
                PUSH=true
                shift
                ;;
            --parallel)
                PARALLEL=true
                shift
                ;;
            --target)
                TARGET="$2"
                shift 2
                ;;
            --platform)
                PLATFORM="$2"
                shift 2
                ;;
            -v|--verbose)
                VERBOSE=true
                DEBUG=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [[ -z "$IMAGE_NAME" ]]; then
                    IMAGE_NAME="$1"
                else
                    log_error "Multiple image names specified: $IMAGE_NAME and $1"
                    exit 1
                fi
                shift
                ;;
        esac
    done

    # Validate image name
    if [[ -z "$IMAGE_NAME" ]]; then
        log_error "No image name specified"
        show_usage
        exit 1
    fi

    if [[ ! " ${AVAILABLE_IMAGES[@]} " =~ " ${IMAGE_NAME} " ]]; then
        log_error "Invalid image name: $IMAGE_NAME"
        log_error "Available images: ${AVAILABLE_IMAGES[*]}"
        exit 1
    fi
}

# Function to build Docker image
build_image() {
    local image_name=$1
    local dockerfile=$2
    local context=${3:-$PROJECT_ROOT}
    local extra_args=("${@:4}")

    local full_image_name="${REGISTRY}/ambari-${image_name}:${TAG}"
    
    log_info "Building image: $full_image_name"
    log_debug "Dockerfile: $dockerfile"
    log_debug "Context: $context"

    # Prepare build arguments
    local build_args=(
        "--build-arg" "UBUNTU_VERSION=$UBUNTU_VERSION"
        "--build-arg" "JAVA_VERSION=$JAVA_VERSION"
        "--build-arg" "MAVEN_VERSION=$MAVEN_VERSION"
        "--build-arg" "NODE_VERSION=$NODE_VERSION"
    )

    # Add target if specified
    if [[ -n "$TARGET" ]]; then
        build_args+=("--target" "$TARGET")
    fi

    # Add platform if specified
    if [[ -n "$PLATFORM" ]]; then
        build_args+=("--platform" "$PLATFORM")
    fi

    # Add no-cache if specified
    if [[ -n "$NO_CACHE" ]]; then
        build_args+=("$NO_CACHE")
    fi

    # Add extra arguments
    build_args+=("${extra_args[@]}")

    # Build command
    local build_cmd=(
        "docker" "build"
        "${build_args[@]}"
        "-f" "$dockerfile"
        "-t" "$full_image_name"
        "$context"
    )

    if [[ "$VERBOSE" == "true" ]]; then
        log_debug "Build command: ${build_cmd[*]}"
    fi

    # Execute build
    if "${build_cmd[@]}"; then
        log_info "Successfully built: $full_image_name"
        
        # Push if requested
        if [[ "$PUSH" == "true" ]]; then
            log_info "Pushing image: $full_image_name"
            if docker push "$full_image_name"; then
                log_info "Successfully pushed: $full_image_name"
            else
                log_error "Failed to push: $full_image_name"
                return 1
            fi
        fi
        
        return 0
    else
        log_error "Failed to build: $full_image_name"
        return 1
    fi
}

# Function to build base image
build_base() {
    log_info "Building Ambari base image..."
    build_image "base" "$SCRIPT_DIR/Dockerfile.base"
}

# Function to build server image
build_server() {
    log_info "Building Ambari server image..."
    
    # Create server Dockerfile if it doesn't exist
    local server_dockerfile="$SCRIPT_DIR/Dockerfile.server"
    if [[ ! -f "$server_dockerfile" ]]; then
        log_info "Creating server Dockerfile..."
        cat > "$server_dockerfile" << 'EOF'
# Apache Ambari Server Image
FROM apache/ambari-base:latest as base

# Switch to root for installation
USER root

# Install Ambari Server
COPY ambari-server/target/rpm/ambari-server/RPMS/x86_64/*.rpm /tmp/
RUN yum install -y /tmp/ambari-server-*.rpm && \
    rm -f /tmp/*.rpm

# Server configuration
COPY docker/config/server/ /etc/ambari-server/conf/

# Switch back to ambari user
USER ambari

# Expose server ports
EXPOSE 8080 8441

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD /usr/local/bin/health-check.sh

# Default command
CMD ["ambari-server", "start"]
EOF
    fi
    
    build_image "server" "$server_dockerfile"
}

# Function to build agent image
build_agent() {
    log_info "Building Ambari agent image..."
    
    # Create agent Dockerfile if it doesn't exist
    local agent_dockerfile="$SCRIPT_DIR/Dockerfile.agent"
    if [[ ! -f "$agent_dockerfile" ]]; then
        log_info "Creating agent Dockerfile..."
        cat > "$agent_dockerfile" << 'EOF'
# Apache Ambari Agent Image
FROM apache/ambari-base:latest as base

# Switch to root for installation
USER root

# Install Ambari Agent
COPY ambari-agent/target/rpm/ambari-agent/RPMS/x86_64/*.rpm /tmp/
RUN yum install -y /tmp/ambari-agent-*.rpm && \
    rm -f /tmp/*.rpm

# Agent configuration
COPY docker/config/agent/ /etc/ambari-agent/conf/

# Switch back to ambari user
USER ambari

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /usr/local/bin/health-check.sh

# Default command
CMD ["ambari-agent", "start"]
EOF
    fi
    
    build_image "agent" "$agent_dockerfile"
}

# Function to build metrics image
build_metrics() {
    log_info "Building Ambari metrics image..."
    
    # Create metrics Dockerfile if it doesn't exist
    local metrics_dockerfile="$SCRIPT_DIR/Dockerfile.metrics"
    if [[ ! -f "$metrics_dockerfile" ]]; then
        log_info "Creating metrics Dockerfile..."
        cat > "$metrics_dockerfile" << 'EOF'
# Apache Ambari Metrics Image
FROM apache/ambari-base:latest as base

# Switch to root for installation
USER root

# Install Ambari Metrics packages
COPY ambari-metrics/ambari-metrics-assembly/target/ambari-metrics-assembly-*.tar.gz /tmp/
RUN cd /tmp && \
    tar -xzf ambari-metrics-assembly-*.tar.gz && \
    mv ambari-metrics-assembly-* /opt/ambari-metrics && \
    rm -f /tmp/*.tar.gz

# Metrics configuration
COPY docker/config/metrics/ /etc/ambari-metrics-collector/conf/

# Switch back to ambari user
USER ambari

# Expose metrics ports
EXPOSE 6188 61888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD /usr/local/bin/health-check.sh

# Default command
CMD ["ambari-metrics-collector", "start"]
EOF
    fi
    
    build_image "metrics" "$metrics_dockerfile"
}

# Function to build development image
build_dev() {
    log_info "Building Ambari development image..."
    build_image "dev" "$SCRIPT_DIR/Dockerfile.base" "$PROJECT_ROOT" "--target" "build"
}

# Function to build all images
build_all() {
    log_info "Building all Ambari images..."
    
    local images=("base" "server" "agent" "metrics" "dev")
    local failed_images=()
    
    if [[ "$PARALLEL" == "true" ]]; then
        log_info "Building images in parallel..."
        local pids=()
        
        for image in "${images[@]}"; do
            (
                case $image in
                    "base") build_base ;;
                    "server") build_server ;;
                    "agent") build_agent ;;
                    "metrics") build_metrics ;;
                    "dev") build_dev ;;
                esac
            ) &
            pids+=($!)
        done
        
        # Wait for all builds to complete
        for i in "${!pids[@]}"; do
            if ! wait "${pids[$i]}"; then
                failed_images+=("${images[$i]}")
            fi
        done
    else
        log_info "Building images sequentially..."
        for image in "${images[@]}"; do
            case $image in
                "base") build_base || failed_images+=("$image") ;;
                "server") build_server || failed_images+=("$image") ;;
                "agent") build_agent || failed_images+=("$image") ;;
                "metrics") build_metrics || failed_images+=("$image") ;;
                "dev") build_dev || failed_images+=("$image") ;;
            esac
        done
    fi
    
    # Report results
    if [[ ${#failed_images[@]} -eq 0 ]]; then
        log_info "All images built successfully!"
    else
        log_error "Failed to build images: ${failed_images[*]}"
        return 1
    fi
}

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker >/dev/null 2>&1; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check Docker daemon
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker daemon is not running"
        exit 1
    fi
    
    # Check BuildKit (recommended)
    if [[ "${DOCKER_BUILDKIT:-}" != "1" ]]; then
        log_warn "DOCKER_BUILDKIT is not enabled. Consider setting DOCKER_BUILDKIT=1 for better performance"
    fi
    
    # Check project structure
    if [[ ! -f "$PROJECT_ROOT/pom.xml" ]]; then
        log_error "Not in Ambari project root directory"
        exit 1
    fi
    
    log_info "Prerequisites check passed"
}

# Main function
main() {
    log_info "Apache Ambari Docker Build Script"
    log_info "================================"
    
    # Parse arguments
    parse_args "$@"
    
    # Check prerequisites
    check_prerequisites
    
    # Show configuration
    log_info "Configuration:"
    log_info "  Registry: $REGISTRY"
    log_info "  Tag: $TAG"
    log_info "  Ubuntu Version: $UBUNTU_VERSION"
    log_info "  Java Version: $JAVA_VERSION"
    log_info "  Maven Version: $MAVEN_VERSION"
    log_info "  Node.js Version: $NODE_VERSION"
    log_info "  Image: $IMAGE_NAME"
    if [[ -n "$TARGET" ]]; then
        log_info "  Target: $TARGET"
    fi
    if [[ -n "$PLATFORM" ]]; then
        log_info "  Platform: $PLATFORM"
    fi
    log_info ""
    
    # Build image(s)
    case $IMAGE_NAME in
        "base") build_base ;;
        "server") build_server ;;
        "agent") build_agent ;;
        "metrics") build_metrics ;;
        "dev") build_dev ;;
        "all") build_all ;;
        *)
            log_error "Unknown image: $IMAGE_NAME"
            exit 1
            ;;
    esac
    
    log_info "Build completed successfully!"
}

# Run main function
main "$@"
