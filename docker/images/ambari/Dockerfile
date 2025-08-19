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

# Multi-stage Dockerfile for Apache Ambari
# Based on Apache Pinot's Docker approach

ARG OPENJDK_IMAGE=eclipse-temurin
ARG JDK_VERSION=8
ARG UBUNTU_VERSION=20.04

# Build stage
FROM ${OPENJDK_IMAGE}:${JDK_VERSION}-jdk AS builder

# Build arguments
ARG AMBARI_GIT_URL=https://github.com/apache/ambari.git
ARG AMBARI_BRANCH=trunk
ARG MAVEN_VERSION=3.6.3
ARG NODE_VERSION=14

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/usr/local/openjdk-${JDK_VERSION}
ENV MAVEN_HOME=/opt/maven
ENV NODE_HOME=/opt/node
ENV PATH=$PATH:$MAVEN_HOME/bin:$NODE_HOME/bin

# Install build dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    build-essential \
    python3 \
    python3-pip \
    python3-dev \
    rpm \
    alien \
    && rm -rf /var/lib/apt/lists/*

# Install Maven
RUN wget -q https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt \
    && mv /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
    && rm apache-maven-${MAVEN_VERSION}-bin.tar.gz

# Install Node.js
RUN wget -q https://nodejs.org/dist/v${NODE_VERSION}.20.1/node-v${NODE_VERSION}.20.1-linux-x64.tar.xz \
    && tar -xJf node-v${NODE_VERSION}.20.1-linux-x64.tar.xz -C /opt \
    && mv /opt/node-v${NODE_VERSION}.20.1-linux-x64 /opt/node \
    && rm node-v${NODE_VERSION}.20.1-linux-x64.tar.xz

# Set working directory
WORKDIR /tmp/ambari-build

# Clone and build Ambari
RUN git clone ${AMBARI_GIT_URL} . \
    && git checkout ${AMBARI_BRANCH} \
    && mvn clean install -DskipTests -Drat.skip=true -Dfindbugs.skip=true \
    && mvn clean package rpm:rpm -DskipTests -Drat.skip=true

# Runtime stage
FROM ${OPENJDK_IMAGE}:${JDK_VERSION}-jre AS runtime

# Runtime arguments
ARG OPENJDK_IMAGE=eclipse-temurin
ARG JDK_VERSION=8

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/opt/java/openjdk
ENV AMBARI_HOME=/opt/ambari
ENV PATH=$PATH:$AMBARI_HOME/bin

# Create ambari user
RUN groupadd -r ambari && useradd -r -g ambari -d /home/ambari -s /bin/bash ambari \
    && mkdir -p /home/ambari \
    && chown -R ambari:ambari /home/ambari

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    postgresql-client \
    curl \
    wget \
    netcat-openbsd \
    procps \
    net-tools \
    sudo \
    openssh-server \
    rsync \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
RUN pip3 install psycopg2-binary requests pyyaml

# Copy built artifacts from builder stage
COPY --from=builder /tmp/ambari-build/ambari-server/target/rpm/ambari-server/RPMS/noarch/*.rpm /tmp/
COPY --from=builder /tmp/ambari-build/ambari-agent/target/rpm/ambari-agent/RPMS/noarch/*.rpm /tmp/

# Convert and install RPM packages
RUN cd /tmp && \
    for rpm in *.rpm; do \
        alien -d --scripts "$rpm"; \
    done && \
    dpkg -i *.deb && \
    rm -f /tmp/*.rpm /tmp/*.deb

# Create necessary directories
RUN mkdir -p /var/lib/ambari-server \
             /var/log/ambari-server \
             /var/lib/ambari-agent \
             /var/log/ambari-agent \
             /etc/ambari-server/conf \
             /etc/ambari-agent/conf \
             /opt/ambari \
    && chown -R ambari:ambari /var/lib/ambari-* \
                              /var/log/ambari-* \
                              /etc/ambari-* \
                              /opt/ambari

# Copy configuration files and scripts
COPY images/ambari/bin/ /opt/ambari/bin/
COPY images/ambari/etc/ /etc/ambari/
COPY scripts/ /usr/local/bin/

# Make scripts executable
RUN chmod +x /opt/ambari/bin/*.sh /usr/local/bin/*.sh

# Configure SSH
RUN apt-get update && apt-get install -y openssh-server && \
    mkdir -p /var/run/sshd && \
    sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config

# Configure sudo for ambari user
RUN echo "ambari ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

# Switch to ambari user
USER ambari

# Expose ports
EXPOSE 8080 8441 8440 6188 61888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD /usr/local/bin/health-check.sh

# Set working directory
WORKDIR /home/ambari

# Entry point
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

# Default command (Pinot-style - uses pinot-admin.sh equivalent)
CMD ["ambari-admin.sh"]
