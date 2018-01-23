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

set -e

export LOGFEEDER_PATH=/root/ambari/ambari-logsearch/ambari-logsearch-logfeeder/target/package

export LOGFEEDER_CONF_DIR=/root/config/logfeeder

#Logfile e.g. /var/log/logfeeder.log
export LOG_PATH=/var/log/ambari-logsearch-logfeeder
export LOG_FILE=logfeeder.out

#pid file e.g. /var/run/logfeeder.pid
export LOGFEEDER_PID_FILE=/var/run/ambari-logsearch-logfeeder/logfeeder.pid

export JAVA_HOME=/usr/java/default

LOGFEEDER_JAVA_MEM=${LOGFEEDER_JAVA_MEM:-"-Xmx512m"}

export LOGFEEDER_DEBUG=true

export LOGFEEDER_DEBUG_PORT=5006

export LOGFEEDER_SSL="true"
export LOGFEEDER_KEYSTORE_LOCATION=/root/config/ssl/logsearch.keyStore.jks
export LOGFEEDER_KEYSTORE_PASSWORD=bigdata
export LOGFEEDER_KEYSTORE_TYPE=jks
export LOGFEEDER_TRUSTSTORE_LOCATION=/root/config/ssl/logsearch.trustStore.jks
export LOGFEEDER_TRUSTSTORE_PASSWORD=bigdata
export LOGFEEDER_TRUSTSTORE_TYPE=jks

