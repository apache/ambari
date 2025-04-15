# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM rockylinux/rockylinux:8

# Install essential dependencies, update system, and configure alternatives in a single layer
RUN dnf install -y dnf-plugins-core && \
    dnf config-manager --set-enabled powertools && \
    dnf -y update && \
    dnf -y install sudo wget openssh-clients openssh-server vim mariadb mariadb-server \
      java-17-openjdk-devel java-1.8.0-openjdk-devel net-tools chrony krb5-server krb5-libs krb5-workstation \
      git rpm-build python3 && \
    java_17_path=$(update-alternatives --list | grep 'java-17-openjdk' | head -1 | cut -f3) && \
    if [ -n "$java_17_path" ]; then \
        update-alternatives --set java "$java_17_path" || echo "darn"; \
    else \
        echo "could not find java17 in alternatives list" >&2; \
    fi && \
    alternatives --set java ${java_17_path}/bin/java && \
    alternatives --set javac ${java_17_path}/bin/javac && \
    dnf clean all

# Download and install MySQL connector, Apache Maven, and perform SSH configuration in a single layer
RUN wget https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.47/mysql-connector-java-5.1.47.jar \
      -O /usr/share/java/mysql-connector-java.jar && \
    wget https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz \
      -O /tmp/apache-maven.tar.gz --no-check-certificate && \
    mkdir -p /usr/share/maven && \
    tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 && \
    rm -f /tmp/apache-maven.tar.gz && \
    ln -s /usr/share/maven/bin/mvn /usr/bin/mvn && \
    sed -i 's,#   StrictHostKeyChecking ask,StrictHostKeyChecking no,g' /etc/ssh/ssh_config && \
    ssh-keygen -A -m PEM && \
    ssh-keygen -f "/root/.ssh/id_rsa" -N "" -m PEM

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH=$JAVA_HOME/bin:$PATH

EXPOSE 1-65535
