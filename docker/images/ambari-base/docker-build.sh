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

# Apache Ambari Base Image Build Script

set -e

# Default configuration
DEFAULT_DOCKER_TAG="ambari-base:latest"
DEFAULT_OPENJDK_IMAGE="openjdk"
DEFAULT_JDK_VERSION="8"

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
Apache Ambari Base Image Build Script

Usage: $0 [Docker Tag] [JDK Version] [OpenJDK Image]

Parameters:
  Docker Tag      Name and tag your docker image. Default is '$DEFAULT_DOCKER_TAG'.
  JDK Version     The JDK version to use. Default is '$DEFAULT_JDK_VERSION'
  OpenJDK Image   Base image to use. Default is '$DEFAULT_OPENJDK_IMAGE'.

Examples:
  $0 ambari-base:latest
  $0 ambari-base:8 8 openjdk
  $0 ambari-base:arm64 8 arm64v8/openjdk

EOF
}

# Parse arguments
DOCKER_TAG="${1:-$DEFAULT_DOCKER_TAG}"
JDK_VERSION="${2:-$DEFAULT_JDK_VERSION}"
OPENJDK_IMAGE="${3:-$DEFAULT_OPENJDK_IMAGE}"

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    show_usage
    exit 0
fi

log_info "Building Ambari Base Image"
log_info "Docker Tag: $DOCKER_TAG"
log_info "JDK Version: $JDK_VERSION"
log_info "OpenJDK Image: $OPENJDK_IMAGE"

# Build the image from docker root directory to access scripts
DOCKER_ROOT="$SCRIPT_DIR/../../"
docker build \
    --build-arg JDK_VERSION="$JDK_VERSION" \
    --build-arg OPENJDK_IMAGE="$OPENJDK_IMAGE" \
    -t "$DOCKER_TAG" \
    -f "$SCRIPT_DIR/Dockerfile" \
    "$DOCKER_ROOT"

log_info "Successfully built: $DOCKER_TAG"
