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

# Log Feeder extra options
export LOGFEEDER_JAVA_OPTS=${LOGFEEDER_JAVA_OPTS:-""}

# Log Feeder debug options
# export LOGFEEDER_DEBUG=true
# exoprt LOGFEEDER_DEBUG_SUSPEND=n
export LOGFEEDER_DEBUG_PORT=5006

# Log Feeder memory
# export LOGFEEDER_JAVA_MEM="-Xmx512m"

# export LOG_PATH=/var/log/ambari-logsearch-logfeeder/
# export LOG_FILE=logfeeder.log

# Pid file of the application
# export LOGFEEDER_PID_DIR=/var/run/ambari-logsearch-logfeeder
# export LOGFEEDER_PID_FILE=logfeeder.pid

# SSL settings"
# export LOGFEEDER_SSL="true"
# export LOGFEEDER_KEYSTORE_LOCATION="/my/path/keystore.jks"
# export LOGFEEDER_KEYSTORE_TYPE="jks"
# export LOGFEEDER_TRUSTSTORE_LOCATION="/my/path/trutstore.jks"
# export LOGFEEDER_TRUSTSTORE_TYPE="jks"