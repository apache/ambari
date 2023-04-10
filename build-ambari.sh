#!/bin/bash
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

export _JAVA_OPTIONS="-Xmx2048m -Djava.awt.headless=true"

PYTHON_VERSION=`python -V | awk -F "[ .]" '{print $2"."$3}'`
CURRENT_DIR=`pwd`
if [ $PYTHON_VERSION != 3.9 ]; then
    cp build-ambari.sh .. 
    sed -i "s|/usr/lib/python3.9|/usr/lib/python${PYTHON_VERSION}|g" `grep -nr "/usr/lib/python3.9" -rl ${CURRENT_DIR}`
    sed -i "s|/usr/bin/python3.9|/usr/bin/python${PYTHON_VERSION}|g" `grep -nr "/usr/bin/python3.9" -rl ${CURRENT_DIR}`
fi

. /etc/os-release
OS="$ID"
ARCH=`uname -m`

if [ $ARCH = "x86_64" ] ; then
  if [ "${OS}" = "centos" ]; then
    mvn clean package -Dbuild-rpm -DskipTests
  fi
else
    sed -i "s|<needarch>x86_64</needarch>|<needarch>aarch64</needarch>|g" ambari-server/pom.xml
    sed -i "s|<needarch>x86_64</needarch>|<needarch>aarch64</needarch>|g" ambari-agent/pom.xml
    sed -i "s|<needarch>x86_64</needarch>|<needarch>aarch64</needarch>|g" ambari-metrics/ambari-metrics-assembly/pom.xml
    sed -i "s|<needarch>x86_64</needarch>|<needarch>aarch64</needarch>|g" ambari-logsearch/ambari-logsearch-assembly/pom.xml
    sed -i "s|<needarch>x86_64</needarch>|<needarch>aarch64</needarch>|g" ambari-infra/ambari-infra-assembly/pom.xml

    if [ "${OS}" = "openEuler" ]; then
    pushd ambari-metrics
    mvn versions:set -DnewVersion=2.7.5.0.0
    popd
    mvn -B clean install rpm:rpm -DnewVersion=2.7.5.0.0 -DskipTests -Drat.skip

    else 
      mvn -B clean package -Dbuild-rpm -DskipTests
    fi
    
fi

\cp -rf ../build-ambari.sh .
rm -rf ../build-ambari.sh
