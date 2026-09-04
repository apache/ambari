# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM rockylinux:8

RUN dnf -y install \
    curl \
    findutils \
    gcc \
    gcc-c++ \
    git \
    gzip \
    java-17-openjdk-devel \
    jq \
    libffi-devel \
    make \
    openssl-devel \
    python39 \
    python39-devel \
    python39-pip \
    redhat-rpm-config \
    rpm-build \
    sudo \
    tar \
    which \
  && dnf clean all \
  && rm -rf /var/cache/dnf \
  && ln -s /usr/bin/python3.9 /usr/local/bin/python3

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
