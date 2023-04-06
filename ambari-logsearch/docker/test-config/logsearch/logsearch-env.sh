#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

export LOGSEARCH_PATH=/root/ambari/ambari-logsearch/ambari-logsearch-server/target/package

export LOGSEARCH_CONF_DIR=/root/config/logsearch

export LOG_PATH=/var/log/ambari-logsearch-portal
export LOG_FILE=logsearch-app.log

export LOGSEARCH_PID_FILE=/var/run/ambari-logsearch-portal/logsearch.pid

export JAVA_HOME=/usr/java/default

LOGSEARCH_JAVA_MEM=${LOGSEARCH_JAVA_MEM:-"-Xmx1024m"}

export LOGSEARCH_DEBUG=true

export LOGSEARCH_DEBUG_PORT=5005

export LOGSEARCH_SSL="true"
export LOGSEARCH_KEYSTORE_LOCATION=/etc/ambari-logsearch-portal/conf/keys/logsearch.jks
export LOGSEARCH_KEYSTORE_TYPE=jks
export LOGSEARCH_TRUSTSTORE_LOCATION=/etc/ambari-logsearch-portal/conf/keys/logsearch.jks
export LOGSEARCH_TRUSTSTORE_TYPE=jks
