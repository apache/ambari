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

# Apache Ambari Docker Push Script
# Based on Apache Pinot's docker-push.sh approach

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
Apache Ambari Docker Push Script

Usage: $0 [IMAGE_NAME] [OPTIONS]

This script publishes a given docker image to your docker registry.
In order to push to your own repo, the image needs to be explicitly tagged with the repo name.

Parameters:
  IMAGE_NAME      Docker image name with tag to push (required)

Options:
  --registry REGISTRY     Docker registry URL (default: docker.io)
  --username USERNAME     Registry username (can also use DOCKER_USERNAME env var)
  --password PASSWORD     Registry password (can also use DOCKER_PASSWORD env var)
  --dry-run              Show what would be pushed without actually pushing
  --force                Force push even if image exists
  --all-tags             Push all tags for the repository
  --help                 Show this help message

Examples:
  # Push to Docker Hub (apache/ambari repo):
  $0 apache/ambari:latest

  # Push with custom registry:
  $0 myregistry.com/ambari:latest --registry myregistry.com

  # Push with authentication:
  $0 apache/ambari:latest --username myuser --password mypass

  # Dry run to see what would be pushed:
  $0 apache/ambari:latest --dry-run

  # Push all tags:
  $0 apache/ambari --all-tags

Environment Variables:
  DOCKER_USERNAME         Registry username
  DOCKER_PASSWORD         Registry password
  DOCKER_REGISTRY         Default registry URL
  DEBUG                   Enable debug output (true/false)

EOF
}

# Function to parse arguments
parse_args() {
    IMAGE_NAME=""
    REGISTRY="${DOCKER_REGISTRY:-docker.io}"
    USERNAME="${DOCKER_USERNAME:-}"
    PASSWORD="${DOCKER_PASSWORD:-}"
    DRY_RUN=false
    FORCE=false
    ALL_TAGS=false

    while [[ $# -gt 0 ]]; do
        case $1 in
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
            --all-tags)
                ALL_TAGS=true
                shift
                ;;
            --help)
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

    # Validate required arguments
    if [[ -z "$IMAGE_NAME" ]]; then
        log_error "Image name is required"
        show_usage
        exit 1
    fi
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
    
    # Check if image exists locally
    if [[ "$ALL_TAGS" == "false" ]]; then
        if ! docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
            log_error "Image not found locally: $IMAGE_NAME"
            log_info "Available images:"
            docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}\t{{.Size}}"
            exit 1
        fi
    fi
    
    log_info "Input validation passed"
}

# Function to display configuration
display_config() {
    log_info "=== Push Configuration ==="
    log_info "Image Name: $IMAGE_NAME"
    log_info "Registry: $REGISTRY"
    log_info "Username: ${USERNAME:-<not set>}"
    log_info "Password: ${PASSWORD:+<set>}${PASSWORD:-<not set>}"
    log_info "Dry Run: $DRY_RUN"
    log_info "Force: $FORCE"
    log_info "All Tags: $ALL_TAGS"
    log_info "========================="
}

# Function to login to registry
docker_login() {
    if [[ -n "$USERNAME" && -n "$PASSWORD" ]]; then
        log_info "Logging in to registry: $REGISTRY"
        
        if [[ "$DRY_RUN" == "true" ]]; then
            log_info "[DRY RUN] Would login to $REGISTRY as $USERNAME"
            return 0
        fi
        
        if echo "$PASSWORD" | docker login "$REGISTRY" --username "$USERNAME" --password-stdin; then
            log_info "Successfully logged in to $REGISTRY"
        else
            log_error "Failed to login to $REGISTRY"
            return 1
        fi
    else
        log_info "No credentials provided, assuming already logged in or using public registry"
    fi
}

# Function to push image
push_image() {
    local image_to_push="$1"
    
    log_info "Pushing image: $image_to_push"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would push: $image_to_push"
        return 0
    fi
    
    # Check if image exists on registry (unless force is used)
    if [[ "$FORCE" == "false" ]]; then
        log_info "Checking if image already exists on registry..."
        if docker manifest inspect "$image_to_push" >/dev/null 2>&1; then
            log_warn "Image already exists on registry: $image_to_push"
            log_warn "Use --force to overwrite existing image"
            return 1
        fi
    fi
    
    # Push the image
    if docker push "$image_to_push"; then
        log_info "Successfully pushed: $image_to_push"
        
        # Display image info
        log_info "Pushed image details:"
        docker images "$image_to_push" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}\t{{.Size}}"
        
        return 0
    else
        log_error "Failed to push: $image_to_push"
        return 1
    fi
}

# Function to push all tags
push_all_tags() {
    local repo_name="${IMAGE_NAME%:*}"  # Remove tag if present
    
    log_info "Finding all local tags for repository: $repo_name"
    
    # Get all local images for this repository
    local images
    images=$(docker images "$repo_name" --format "{{.Repository}}:{{.Tag}}" | grep -v "<none>")
    
    if [[ -z "$images" ]]; then
        log_error "No images found for repository: $repo_name"
        return 1
    fi
    
    log_info "Found images to push:"
    echo "$images"
    
    # Push each image
    local failed_pushes=()
    while IFS= read -r image; do
        if ! push_image "$image"; then
            failed_pushes+=("$image")
        fi
    done <<< "$images"
    
    # Report results
    if [[ ${#failed_pushes[@]} -eq 0 ]]; then
        log_info "All images pushed successfully!"
    else
        log_error "Failed to push images: ${failed_pushes[*]}"
        return 1
    fi
}

# Function to logout from registry
docker_logout() {
    if [[ -n "$USERNAME" && "$DRY_RUN" == "false" ]]; then
        log_info "Logging out from registry: $REGISTRY"
        docker logout "$REGISTRY" || true
    fi
}

# Function to cleanup
cleanup() {
    docker_logout
}

# Main function
main() {
    log_info "Apache Ambari Docker Push Script"
    log_info "================================"
    
    # Parse arguments
    parse_args "$@"
    
    # Validate inputs
    validate_inputs
    
    # Display configuration
    display_config
    
    # Set up cleanup trap
    trap cleanup EXIT
    
    # Login to registry
    if ! docker_login; then
        log_error "Registry login failed"
        exit 1
    fi
    
    # Push image(s)
    if [[ "$ALL_TAGS" == "true" ]]; then
        if push_all_tags; then
            log_info "All tags pushed successfully!"
        else
            log_error "Some pushes failed!"
            exit 1
        fi
    else
        if push_image "$IMAGE_NAME"; then
            log_info "Image pushed successfully!"
        else
            log_error "Push failed!"
            exit 1
        fi
    fi
    
    # Show next steps
    log_info ""
    log_info "Next steps:"
    log_info "  1. Verify image on registry: docker pull $IMAGE_NAME"
    log_info "  2. Use in docker-compose or kubernetes"
    log_info "  3. Update documentation with new image tag"
    
    log_info "Push completed successfully!"
}

# Run main function
main "$@"
