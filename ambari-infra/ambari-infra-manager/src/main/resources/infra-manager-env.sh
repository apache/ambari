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

# Extend with java options or system properties. e.g.: INFRA_MANAGER_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=5007,server=y,suspend=n"
export INFRA_MANAGER_OPTS=""

# Infra Manager debug options
#export INFRA_MANAGER_DEBUG=true
#export INFRA_MANAGER_DEBUG_SUSPEND=n
export INFRA_MANAGER_DEBUG_PORT=5005

# Infra Manager memory
# export INFRA_MANAGER_JAVA_MEM="--Xmx1024m"

#export LOG_PATH=/var/log/ambari-infra-manager/
#export LOG_FILE=infra-manager.log

# Pid file of the application
#export INFRA_MANAGER_PID_DIR=/var/run/ambari-infra-manager
#export INFRA_MANAGER_PID_FILE=infra-manager.pid

# SSL settings"
# export INFRA_MANAGER_SSL="true"
# export INFRA_MANAGER_KEYSTORE_LOCATION="/my/path/keystore.jks"
# export INFRA_MANAGER_KEYSTORE_TYPE="jks"
# export INFRA_MANAGER_TRUSTSTORE_LOCATION="/my/path/trutstore.jks"
# export INFRA_MANAGER_TRUSTSTORE_TYPE="jks"