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

# Dockerfile for installing the necessary dependencies for building Ambari

ARG BUILD_OS
FROM ambari-build-base:${BUILD_OS}

ARG MAVEN_VERSION=3.9.11
ENV MAVEN_HOME=/opt/maven
RUN set -eux; \
  archive="apache-maven-${MAVEN_VERSION}-bin.tar.gz"; \
  base_url="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries"; \
  mkdir -p "${MAVEN_HOME}"; \
  curl --fail --location --silent --show-error "${base_url}/${archive}" --output "/tmp/${archive}"; \
  curl --fail --location --silent --show-error "${base_url}/${archive}.sha512" --output "/tmp/${archive}.sha512"; \
  echo "$(awk '{print $1}' "/tmp/${archive}.sha512")  /tmp/${archive}" | sha512sum --check -; \
  tar -xzf "/tmp/${archive}" --strip-components 1 -C "${MAVEN_HOME}"; \
  rm -f "/tmp/${archive}" "/tmp/${archive}.sha512"
ENV PATH="${PATH}:${MAVEN_HOME}/bin"

COPY .bashrc /root/
