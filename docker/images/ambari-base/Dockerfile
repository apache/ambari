# Apache Ambari Base Image
# Multi-stage build for optimized production image

ARG UBUNTU_VERSION=22.04
FROM ubuntu:${UBUNTU_VERSION} AS base

# Metadata
LABEL maintainer="Apache Ambari Community <dev@ambari.apache.org>"
LABEL org.apache.ambari.version="3.0.0-SNAPSHOT"
LABEL org.apache.ambari.component="base"
LABEL org.opencontainers.image.source="https://github.com/apache/ambari"
LABEL org.opencontainers.image.description="Apache Ambari Base Runtime Image"
LABEL org.opencontainers.image.licenses="Apache-2.0"

# Build arguments
ARG JAVA_VERSION=11
ARG MAVEN_VERSION=3.9.6
ARG NODE_VERSION=18
ARG PYTHON_VERSION=3.10

# Environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=UTC
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Create ambari user and group
RUN groupadd -r ambari --gid=1000 && \
    useradd -r -g ambari --uid=1000 --home-dir=/opt/ambari --shell=/bin/bash ambari && \
    mkdir -p /opt/ambari && \
    chown -R ambari:ambari /opt/ambari

# Install system dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        # Basic utilities
        curl \
        wget \
        unzip \
        zip \
        tar \
        gzip \
        ca-certificates \
        gnupg \
        lsb-release \
        # Network tools
        net-tools \
        netcat \
        telnet \
        openssh-client \
        # System tools
        sudo \
        vim \
        less \
        htop \
        procps \
        psmisc \
        # Build tools (for native dependencies)
        build-essential \
        gcc \
        g++ \
        make \
        # Python and pip
        python3 \
        python3-pip \
        python3-dev \
        python3-setuptools \
        python3-wheel \
        # Git for source management
        git \
        # Database clients
        postgresql-client \
        mysql-client \
        # SSL/TLS support
        openssl \
        # Time synchronization
        ntp \
        ntpdate \
        # Process management
        supervisor && \
    # Clean up
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install OpenJDK
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-${JAVA_VERSION}-jdk && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64
ENV PATH=${JAVA_HOME}/bin:${PATH}

# Install Maven using apt package manager (more reliable)
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set Maven environment (Ubuntu package installs to /usr/share/maven)
ENV MAVEN_HOME=/usr/share/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

# Install Node.js and npm using apt package manager (more reliable)
RUN apt-get update && \
    apt-get install -y --no-install-recommends nodejs npm && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Python packages
RUN pip3 install --no-cache-dir \
    setuptools \
    wheel \
    requests \
    pyyaml \
    jinja2 \
    psutil \
    six

# Create directory structure
RUN mkdir -p \
    /var/log/ambari-server \
    /var/log/ambari-agent \
    /var/log/ambari-metrics-collector \
    /var/log/ambari-metrics-monitor \
    /var/run/ambari-server \
    /var/run/ambari-agent \
    /var/run/ambari-metrics-collector \
    /var/run/ambari-metrics-monitor \
    /var/lib/ambari-server \
    /var/lib/ambari-agent \
    /var/lib/ambari-metrics-collector \
    /etc/ambari-server/conf \
    /etc/ambari-agent/conf \
    /etc/ambari-metrics-collector/conf \
    /etc/ambari-metrics-monitor/conf && \
    chown -R ambari:ambari \
        /var/log/ambari-* \
        /var/run/ambari-* \
        /var/lib/ambari-* \
        /etc/ambari-*

# Configure sudo for ambari user
RUN echo "ambari ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers.d/ambari && \
    chmod 0440 /etc/sudoers.d/ambari

# SSH configuration for multi-node setups
RUN mkdir -p /home/ambari/.ssh && \
    ssh-keygen -t rsa -N '' -f /home/ambari/.ssh/id_rsa && \
    cat /home/ambari/.ssh/id_rsa.pub >> /home/ambari/.ssh/authorized_keys && \
    chmod 600 /home/ambari/.ssh/authorized_keys && \
    chmod 700 /home/ambari/.ssh && \
    chown -R ambari:ambari /home/ambari/.ssh

# Configure SSH daemon
RUN apt-get update && apt-get install -y openssh-server && \
    mkdir -p /var/run/sshd && \
    sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config && \
    sed -i 's/UsePAM yes/UsePAM no/' /etc/ssh/sshd_config

# Set up Maven repository cache
RUN mkdir -p /home/ambari/.m2 && \
    chown -R ambari:ambari /home/ambari/.m2

# Health check script
COPY scripts/health-check.sh /usr/local/bin/health-check.sh
RUN chmod +x /usr/local/bin/health-check.sh

# Entrypoint script
COPY scripts/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# Set working directory
WORKDIR /opt/ambari

# Switch to ambari user
USER ambari

# Default command
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["bash"]

# Build stage for development
FROM base AS build

# Switch back to root for build tools
USER root

# Install additional build dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        # Additional build tools
        cmake \
        autoconf \
        automake \
        libtool \
        pkg-config \
        # Development headers
        libssl-dev \
        libffi-dev \
        libxml2-dev \
        libxslt1-dev \
        zlib1g-dev \
        # RPM building tools
        rpm \
        rpmbuild \
        # Debugging tools
        gdb \
        strace \
        tcpdump && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install additional Python development packages
RUN pip3 install --no-cache-dir \
    pytest \
    mock \
    coverage \
    flake8 \
    pylint

# Pre-download Maven dependencies for faster builds
COPY pom.xml /tmp/pom.xml
RUN cd /tmp && \
    mvn dependency:go-offline -B -q && \
    rm -rf /tmp/*

# Switch back to ambari user
USER ambari

# Runtime stage for production
FROM base AS runtime

# Copy only necessary files from build stage
# This keeps the runtime image smaller

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD /usr/local/bin/health-check.sh

# Expose common ports
EXPOSE 8080 8441 8440 6188 61888

# Default volumes
VOLUME ["/var/log/ambari-server", "/var/log/ambari-agent", "/var/log/ambari-metrics-collector"]
VOLUME ["/var/lib/ambari-server", "/var/lib/ambari-agent", "/var/lib/ambari-metrics-collector"]
VOLUME ["/etc/ambari-server/conf", "/etc/ambari-agent/conf", "/etc/ambari-metrics-collector/conf"]
