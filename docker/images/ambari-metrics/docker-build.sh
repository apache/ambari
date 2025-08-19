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

# Apache Ambari Metrics Image Build Script

set -e

# Default configuration
DEFAULT_DOCKER_TAG="ambari-metrics:latest"
DEFAULT_BASE_IMAGE="ambari-base:latest"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Apache Ambari Metrics Image Build Script

Usage: $0 [Docker Tag] [Base Image]

Parameters:
  Docker Tag      Name and tag your docker image. Default is '$DEFAULT_DOCKER_TAG'.
  Base Image      Base image to use. Default is '$DEFAULT_BASE_IMAGE'.

Examples:
  $0 ambari-metrics:latest
  $0 ambari-metrics:2.7.5 ambari-base:2.7.5

EOF
}

# Parse arguments
DOCKER_TAG="${1:-$DEFAULT_DOCKER_TAG}"
BASE_IMAGE="${2:-$DEFAULT_BASE_IMAGE}"

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    show_usage
    exit 0
fi

log_info "Building Ambari Metrics Image"
log_info "Docker Tag: $DOCKER_TAG"
log_info "Base Image: $BASE_IMAGE"

# Build the image
docker build \
    --build-arg BASE_IMAGE="$BASE_IMAGE" \
    -t "$DOCKER_TAG" \
    -f "$SCRIPT_DIR/Dockerfile" \
    "$SCRIPT_DIR"

log_info "Successfully built: $DOCKER_TAG"
