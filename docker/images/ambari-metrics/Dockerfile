# Multi-stage Dockerfile for Apache Ambari Metrics Collector
# Based on best practices from Apache Pinot and other Apache projects

# Build stage
FROM ubuntu:22.04 AS builder

# Build arguments
ARG MAVEN_VERSION=3.9.6
ARG JAVA_VERSION=11

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64
ENV MAVEN_HOME=/opt/maven
ENV PATH=$PATH:$MAVEN_HOME/bin

# Install build dependencies
RUN apt-get update && apt-get install -y \
    openjdk-${JAVA_VERSION}-jdk \
    maven \
    curl \
    wget \
    git \
    build-essential \
    python3 \
    python3-pip \
    python3-dev \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /workspace

# Copy source code
COPY . .

# Build Ambari Metrics
RUN mvn clean install -pl ambari-metrics -DskipTests -Drat.skip=true \
    && mvn clean package -pl ambari-metrics/ambari-metrics-assembly -DskipTests

# Runtime stage
FROM ubuntu:22.04 AS runtime

# Runtime arguments
ARG JAVA_VERSION=11

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64
ENV AMBARI_METRICS_HOME=/opt/ambari-metrics
ENV PATH=$PATH:$AMBARI_METRICS_HOME/bin

# Create ambari user
RUN groupadd -r ambari && useradd -r -g ambari -d /home/ambari -s /bin/bash ambari \
    && mkdir -p /home/ambari \
    && chown -R ambari:ambari /home/ambari

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    openjdk-${JAVA_VERSION}-jre-headless \
    python3 \
    python3-pip \
    curl \
    wget \
    netcat \
    procps \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

# Copy built artifacts from builder stage
COPY --from=builder /workspace/ambari-metrics/ambari-metrics-assembly/target/ambari-metrics-assembly-*.tar.gz /tmp/

# Install Ambari Metrics
RUN cd /tmp && \
    tar -xzf ambari-metrics-assembly-*.tar.gz && \
    mv ambari-metrics-assembly-* /opt/ambari-metrics && \
    rm -f /tmp/*.tar.gz

# Create necessary directories
RUN mkdir -p /var/lib/ambari-metrics-collector \
             /var/log/ambari-metrics-collector \
             /etc/ambari-metrics-collector/conf \
    && chown -R ambari:ambari /var/lib/ambari-metrics-collector \
                              /var/log/ambari-metrics-collector \
                              /etc/ambari-metrics-collector \
                              /opt/ambari-metrics

# Copy configuration files and scripts
COPY docker/config/metrics/ /etc/ambari-metrics-collector/conf/
COPY docker/scripts/ /usr/local/bin/

# Make scripts executable
RUN chmod +x /usr/local/bin/*.sh

# Create startup script
RUN cat > /usr/local/bin/start-ambari-metrics.sh << 'EOF'
#!/bin/bash
set -e

# Configure metrics collector
echo "Configuring Ambari Metrics Collector..."

# Set heap size
export COLLECTOR_HEAPSIZE=${METRICS_COLLECTOR_HEAPSIZE:-1024m}

# Create HBase configuration
mkdir -p /var/lib/ambari-metrics-collector/hbase/conf

# Configure HBase for embedded mode
cat > /var/lib/ambari-metrics-collector/hbase/conf/hbase-site.xml << EOL
<?xml version="1.0"?>
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file:///var/lib/ambari-metrics-collector/hbase</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/var/lib/ambari-metrics-collector/hbase/zookeeper</value>
  </property>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>false</value>
  </property>
  <property>
    <name>hbase.tmp.dir</name>
    <value>/var/lib/ambari-metrics-collector/hbase/tmp</value>
  </property>
</configuration>
EOL

# Start Ambari Metrics Collector
echo "Starting Ambari Metrics Collector..."
cd /opt/ambari-metrics
exec ./bin/ambari-metrics-collector start
EOF

RUN chmod +x /usr/local/bin/start-ambari-metrics.sh

# Switch to ambari user
USER ambari

# Expose metrics ports
EXPOSE 6188 61888

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD /usr/local/bin/health-check.sh metrics

# Set working directory
WORKDIR /home/ambari

# Default command
CMD ["/usr/local/bin/start-ambari-metrics.sh"]

# Development stage
FROM runtime AS development

# Switch back to root for development tools
USER root

# Install development dependencies
RUN apt-get update && apt-get install -y \
    maven \
    git \
    vim \
    less \
    htop \
    && rm -rf /var/lib/apt/lists/*

# Create Maven cache directory
RUN mkdir -p /home/ambari/.m2 && \
    chown -R ambari:ambari /home/ambari/.m2

# Switch back to ambari user
USER ambari

# Mount point for source code
VOLUME ["/workspace"]

# Default command for development
CMD ["bash"]
