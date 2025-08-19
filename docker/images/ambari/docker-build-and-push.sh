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

# Apache Ambari Docker Build and Push Script
# Based on Apache Pinot's docker-build-and-push.sh approach

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
Apache Ambari Docker Build and Push Script

Usage: $0 [Docker Tag] [Git Branch] [Ambari Git URL] [OPTIONS]

This script builds and publishes a docker image to your docker registry.
It combines the functionality of docker-build.sh and docker-push.sh.

Parameters:
  Docker Tag      Name and tag your docker image (required)
  Git Branch      The Ambari branch to build (default: trunk)
  Ambari Git URL  The Ambari Git Repo to build (default: Apache repo)

Build Options:
  --dockerfile DOCKERFILE    Use specific Dockerfile (default: Dockerfile)
  --no-cache                 Build without using cache
  --platform PLATFORM       Target platform (e.g., linux/amd64,linux/arm64)
  --build-arg KEY=VALUE      Pass build argument

Push Options:
  --registry REGISTRY        Docker registry URL (default: docker.io)
  --username USERNAME        Registry username (can also use DOCKER_USERNAME env var)
  --password PASSWORD        Registry password (can also use DOCKER_PASSWORD env var)
  --dry-run                  Show what would be done without actually doing it
  --force                    Force push even if image exists
  --skip-push                Build only, skip push

General Options:
  --help                     Show this help message

Examples:
  # Build and push to Docker Hub (apache/ambari repo):
  $0 apache/ambari:latest

  # Build from specific branch and push:
  $0 apache/ambari:2.7.5 release-2.7.5

  # Build from fork and push with authentication:
  $0 myregistry.com/ambari:latest trunk https://github.com/myfork/ambari.git \\
    --registry myregistry.com --username myuser --password mypass

  # Build only (skip push):
  $0 ambari:dev trunk --skip-push

  # Dry run to see what would be done:
  $0 apache/ambari:latest --dry-run

Environment Variables:
  DOCKER_USERNAME         Registry username
  DOCKER_PASSWORD         Registry password
  DOCKER_REGISTRY         Default registry URL
  DOCKER_BUILDKIT         Enable BuildKit (recommended: 1)
  DEBUG                   Enable debug output (true/false)

EOF
}

# Function to parse arguments
parse_args() {
    # Required arguments
    DOCKER_TAG=""
    GIT_BRANCH="trunk"
    AMBARI_GIT_URL="https://github.com/apache/ambari.git"
    
    # Build options
    DOCKERFILE="Dockerfile"
    NO_CACHE=""
    PLATFORM=""
    BUILD_ARGS=()
    
    # Push options
    REGISTRY="${DOCKER_REGISTRY:-docker.io}"
    USERNAME="${DOCKER_USERNAME:-}"
    PASSWORD="${DOCKER_PASSWORD:-}"
    DRY_RUN=false
    FORCE=false
    SKIP_PUSH=false
    
    # Parse positional arguments first
    if [[ $# -gt 0 && ! "$1" =~ ^-- ]]; then
        DOCKER_TAG="$1"
        shift
    fi
    
    if [[ $# -gt 0 && ! "$1" =~ ^-- ]]; then
        GIT_BRANCH="$1"
        shift
    fi
    
    if [[ $# -gt 0 && ! "$1" =~ ^-- ]]; then
        AMBARI_GIT_URL="$1"
        shift
    fi
    
    # Parse optional arguments
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
            --registry)
                REGISTRY="$2"
                shift 2
                ;;
            --username)
                USERNAME="$2"
                shift 2
                ;;
            --password)
                PASSWORD="$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --force)
                FORCE=true
                shift
                ;;
            --skip-push)
                SKIP_PUSH=true
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Validate required arguments
    if [[ -z "$DOCKER_TAG" ]]; then
        log_error "Docker tag is required"
        show_usage
        exit 1
    fi
}

# Function to display configuration
display_config() {
    log_info "=== Build and Push Configuration ==="
    log_info "Docker Tag: $DOCKER_TAG"
    log_info "Git Branch: $GIT_BRANCH"
    log_info "Ambari Git URL: $AMBARI_GIT_URL"
    log_info "Dockerfile: $DOCKERFILE"
    if [[ -n "$PLATFORM" ]]; then
        log_info "Platform: $PLATFORM"
    fi
    if [[ "$SKIP_PUSH" == "false" ]]; then
        log_info "Registry: $REGISTRY"
        log_info "Username: ${USERNAME:-<not set>}"
        log_info "Password: ${PASSWORD:+<set>}${PASSWORD:-<not set>}"
    else
        log_info "Push: SKIPPED"
    fi
    log_info "Dry Run: $DRY_RUN"
    log_info "Force: $FORCE"
    log_info "================================="
}

# Function to build image
build_image() {
    log_info "=== Building Docker Image ==="
    
    # Prepare build command arguments
    local build_cmd_args=(
        "$DOCKER_TAG"
        "$GIT_BRANCH"
        "$AMBARI_GIT_URL"
    )
    
    # Add build options
    if [[ -n "$DOCKERFILE" && "$DOCKERFILE" != "Dockerfile" ]]; then
        build_cmd_args+=("--dockerfile" "$DOCKERFILE")
    fi
    
    if [[ -n "$NO_CACHE" ]]; then
        build_cmd_args+=("$NO_CACHE")
    fi
    
    if [[ -n "$PLATFORM" ]]; then
        build_cmd_args+=($PLATFORM)
    fi
    
    # Add custom build args
    build_cmd_args+=("${BUILD_ARGS[@]}")
    
    # Execute build command
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would execute: $SCRIPT_DIR/docker-build.sh ${build_cmd_args[*]}"
        return 0
    else
        log_info "Executing: $SCRIPT_DIR/docker-build.sh ${build_cmd_args[*]}"
        if "$SCRIPT_DIR/docker-build.sh" "${build_cmd_args[@]}"; then
            log_info "Build completed successfully"
            return 0
        else
            log_error "Build failed"
            return 1
        fi
    fi
}

# Function to push image
push_image() {
    log_info "=== Pushing Docker Image ==="
    
    # Prepare push command arguments
    local push_cmd_args=("$DOCKER_TAG")
    
    # Add push options
    if [[ -n "$REGISTRY" && "$REGISTRY" != "docker.io" ]]; then
        push_cmd_args+=("--registry" "$REGISTRY")
    fi
    
    if [[ -n "$USERNAME" ]]; then
        push_cmd_args+=("--username" "$USERNAME")
    fi
    
    if [[ -n "$PASSWORD" ]]; then
        push_cmd_args+=("--password" "$PASSWORD")
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        push_cmd_args+=("--dry-run")
    fi
    
    if [[ "$FORCE" == "true" ]]; then
        push_cmd_args+=("--force")
    fi
    
    # Execute push command
    log_info "Executing: $SCRIPT_DIR/docker-push.sh ${push_cmd_args[*]}"
    if "$SCRIPT_DIR/docker-push.sh" "${push_cmd_args[@]}"; then
        log_info "Push completed successfully"
        return 0
    else
        log_error "Push failed"
        return 1
    fi
}

# Function to validate prerequisites
validate_prerequisites() {
    log_info "Validating prerequisites..."
    
    # Check if build script exists
    if [[ ! -f "$SCRIPT_DIR/docker-build.sh" ]]; then
        log_error "Build script not found: $SCRIPT_DIR/docker-build.sh"
        exit 1
    fi
    
    # Check if push script exists (only if not skipping push)
    if [[ "$SKIP_PUSH" == "false" && ! -f "$SCRIPT_DIR/docker-push.sh" ]]; then
        log_error "Push script not found: $SCRIPT_DIR/docker-push.sh"
        exit 1
    fi
    
    # Make sure scripts are executable
    chmod +x "$SCRIPT_DIR/docker-build.sh"
    if [[ "$SKIP_PUSH" == "false" ]]; then
        chmod +x "$SCRIPT_DIR/docker-push.sh"
    fi
    
    log_info "Prerequisites validation passed"
}

# Main function
main() {
    log_info "Apache Ambari Docker Build and Push Script"
    log_info "=========================================="
    
    # Parse arguments
    parse_args "$@"
    
    # Validate prerequisites
    validate_prerequisites
    
    # Display configuration
    display_config
    
    # Build image
    if ! build_image; then
        log_error "Build phase failed"
        exit 1
    fi
    
    # Push image (if not skipped)
    if [[ "$SKIP_PUSH" == "false" ]]; then
        if ! push_image; then
            log_error "Push phase failed"
            exit 1
        fi
    else
        log_info "Push phase skipped as requested"
    fi
    
    # Success message
    log_info ""
    log_info "=== Build and Push Completed Successfully ==="
    log_info "Image: $DOCKER_TAG"
    if [[ "$SKIP_PUSH" == "false" && "$DRY_RUN" == "false" ]]; then
        log_info "Registry: $REGISTRY"
        log_info ""
        log_info "Next steps:"
        log_info "  1. Verify image: docker pull $DOCKER_TAG"
        log_info "  2. Test image: docker run -it --rm $DOCKER_TAG"
        log_info "  3. Use in production deployments"
    elif [[ "$DRY_RUN" == "true" ]]; then
        log_info "This was a dry run - no actual changes were made"
    else
        log_info "Image built locally - use docker-push.sh to publish"
    fi
    
    log_info "============================================="
}

# Run main function
main "$@"
