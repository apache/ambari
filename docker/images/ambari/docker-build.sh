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
# Based on Apache Pinot's docker-build.sh approach

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_DOCKER_TAG="ambari:latest"
DEFAULT_GIT_BRANCH="trunk"
DEFAULT_AMBARI_GIT_URL="https://github.com/apache/ambari.git"
DEFAULT_KAFKA_VERSION="2.0"
DEFAULT_JAVA_VERSION="8"
DEFAULT_JDK_VERSION="8"
DEFAULT_OPENJDK_IMAGE="eclipse-temurin"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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

Usage: $0 [Docker Tag] [Git Branch] [Ambari Git URL] [Kafka Version] [Java Version] [JDK Version] [OpenJDK Image]

This script will check out Ambari Repo [Ambari Git URL] on branch [Git Branch] and build the docker image for that.
The docker image is tagged as [Docker Tag].

Parameters:
  Docker Tag      Name and tag your docker image. Default is '$DEFAULT_DOCKER_TAG'.
  Git Branch      The Ambari branch to build. Default is '$DEFAULT_GIT_BRANCH'.
  Ambari Git URL  The Ambari Git Repo to build, users can set it to their own fork. 
                  Please note that, the URL is https:// based, not git://. 
                  Default is the Apache Repo: '$DEFAULT_AMBARI_GIT_URL'.
  Kafka Version   The Kafka Version to build ambari with. Default is '$DEFAULT_KAFKA_VERSION'
  Java Version    The Java Build and Runtime image version. Default is '$DEFAULT_JAVA_VERSION'
  JDK Version     The JDK parameter to build ambari, set as part of maven build option: 
                  -Djdk.version=\${JDK_VERSION}. Default is '$DEFAULT_JDK_VERSION'
  OpenJDK Image   Base image to use for Ambari build and runtime. Default is '$DEFAULT_OPENJDK_IMAGE'.

Examples:
  # Build and tag a snapshot on your own fork:
  $0 ambari_fork:snapshot-3.0 snapshot-3.0 https://github.com/your_own_fork/ambari.git

  # Build a release version:
  $0 ambari:release-2.7.5 release-2.7.5 https://github.com/apache/ambari.git

  # Build image with arm64 base image (for Mac M1 chips):
  $0 ambari:latest trunk https://github.com/apache/ambari.git 2.0 8 8 arm64v8/openjdk

Options:
  --dockerfile DOCKERFILE    Use specific Dockerfile (default: Dockerfile)
  --no-cache                 Build without using cache
  --platform PLATFORM       Target platform (e.g., linux/amd64,linux/arm64)
  --build-arg KEY=VALUE      Pass build argument
  --help                     Show this help message

Environment Variables:
  DOCKER_BUILDKIT           Enable BuildKit (recommended: 1)
  DEBUG                     Enable debug output (true/false)

EOF
}

# Function to parse arguments
parse_args() {
    # Optional arguments
    DOCKERFILE="Dockerfile"
    NO_CACHE=""
    PLATFORM=""
    BUILD_ARGS=()
    
    # Parse all arguments, separating positional from optional
    local positional_args=()
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dockerfile)
                DOCKERFILE="$2"
                shift 2
                ;;
            --no-cache)
                NO_CACHE="--no-cache"
                shift
                ;;
            --platform)
                PLATFORM="--platform $2"
                shift 2
                ;;
            --build-arg)
                BUILD_ARGS+=("--build-arg" "$2")
                shift 2
                ;;
            --help)
                show_usage
                exit 0
                ;;
            --*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                # This is a positional argument
                positional_args+=("$1")
                shift
                ;;
        esac
    done
    
    # Assign positional arguments
    DOCKER_TAG="${positional_args[0]:-$DEFAULT_DOCKER_TAG}"
    GIT_BRANCH="${positional_args[1]:-$DEFAULT_GIT_BRANCH}"
    AMBARI_GIT_URL="${positional_args[2]:-$DEFAULT_AMBARI_GIT_URL}"
    KAFKA_VERSION="${positional_args[3]:-$DEFAULT_KAFKA_VERSION}"
    JAVA_VERSION="${positional_args[4]:-$DEFAULT_JAVA_VERSION}"
    JDK_VERSION="${positional_args[5]:-$DEFAULT_JDK_VERSION}"
    OPENJDK_IMAGE="${positional_args[6]:-$DEFAULT_OPENJDK_IMAGE}"
}

# Function to validate inputs
validate_inputs() {
    log_info "Validating inputs..."
    
    # Check if Docker is available
    if ! command -v docker >/dev/null 2>&1; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check Docker daemon
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker daemon is not running"
        exit 1
    fi
    
    # Validate Git URL format
    if [[ ! "$AMBARI_GIT_URL" =~ ^https:// ]]; then
        log_error "Git URL must be https:// based: $AMBARI_GIT_URL"
        exit 1
    fi
    
    # Check if Dockerfile exists
    if [[ ! -f "$SCRIPT_DIR/$DOCKERFILE" ]]; then
        log_error "Dockerfile not found: $SCRIPT_DIR/$DOCKERFILE"
        exit 1
    fi
    
    log_info "Input validation passed"
}

# Function to display configuration
display_config() {
    log_info "=== Build Configuration ==="
    log_info "Docker Tag: $DOCKER_TAG"
    log_info "Git Branch: $GIT_BRANCH"
    log_info "Ambari Git URL: $AMBARI_GIT_URL"
    log_info "Kafka Version: $KAFKA_VERSION"
    log_info "Java Version: $JAVA_VERSION"
    log_info "JDK Version: $JDK_VERSION"
    log_info "OpenJDK Image: $OPENJDK_IMAGE"
    log_info "Dockerfile: $DOCKERFILE"
    if [[ -n "$PLATFORM" ]]; then
        log_info "Platform: $PLATFORM"
    fi
    log_info "=========================="
}

# Function to build Docker image
build_image() {
    log_info "Starting Docker build..."
    
    # Prepare build arguments
    local build_args=(
        "--build-arg" "AMBARI_GIT_URL=$AMBARI_GIT_URL"
        "--build-arg" "AMBARI_BRANCH=$GIT_BRANCH"
        "--build-arg" "KAFKA_VERSION=$KAFKA_VERSION"
        "--build-arg" "JDK_VERSION=$JDK_VERSION"
        "--build-arg" "OPENJDK_IMAGE=$OPENJDK_IMAGE"
    )
    
    # Add custom build args
    build_args+=("${BUILD_ARGS[@]}")
    
    # Build command
    local build_cmd=(
        "docker" "build"
        "${build_args[@]}"
        "-f" "$SCRIPT_DIR/$DOCKERFILE"
        "-t" "$DOCKER_TAG"
    )
    
    # Add optional flags
    if [[ -n "$NO_CACHE" ]]; then
        build_cmd+=("$NO_CACHE")
    fi
    
    if [[ -n "$PLATFORM" ]]; then
        build_cmd+=($PLATFORM)
    fi
    
    # Add context (docker directory - parent of script directory)
    build_cmd+=("$(dirname "$(dirname "$SCRIPT_DIR")")")
    
    log_debug "Build command: ${build_cmd[*]}"
    
    # Execute build
    log_info "Executing: ${build_cmd[*]}"
    if "${build_cmd[@]}"; then
        log_info "Docker image built successfully: $DOCKER_TAG"
        
        # Display image info
        log_info "Image details:"
        docker images "$DOCKER_TAG" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}\t{{.Size}}"
        
        return 0
    else
        log_error "Docker build failed"
        return 1
    fi
}

# Function to run post-build tests
run_tests() {
    log_info "Running post-build tests..."
    
    # Test if image can be run
    log_info "Testing image startup..."
    if docker run --rm "$DOCKER_TAG" java -version >/dev/null 2>&1; then
        log_info "Image startup test passed"
    else
        log_warn "Image startup test failed"
    fi
    
    # Test if required tools are available
    log_info "Testing required tools..."
    if docker run --rm "$DOCKER_TAG" which ambari-server >/dev/null 2>&1; then
        log_info "Ambari server found in image"
    else
        log_warn "Ambari server not found in image"
    fi
}

# Function to cleanup
cleanup() {
    log_info "Cleaning up temporary files..."
    # Add any cleanup logic here if needed
}

# Main function
main() {
    log_info "Apache Ambari Docker Build Script"
    log_info "================================="
    
    # Parse arguments
    parse_args "$@"
    
    # Validate inputs
    validate_inputs
    
    # Display configuration
    display_config
    
    # Set up cleanup trap
    trap cleanup EXIT
    
    # Build image
    if build_image; then
        # Run tests
        run_tests
        
        log_info "Build completed successfully!"
        log_info "Image: $DOCKER_TAG"
        
        # Show next steps
        log_info ""
        log_info "Next steps:"
        log_info "  1. Test the image: docker run -it --rm $DOCKER_TAG"
        log_info "  2. Push to registry: docker push $DOCKER_TAG"
        log_info "  3. Use in docker-compose or kubernetes"
        
        exit 0
    else
        log_error "Build failed!"
        exit 1
    fi
}

# Run main function
main "$@"
