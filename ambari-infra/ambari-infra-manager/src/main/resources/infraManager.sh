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

: ${JAVA_HOME:?"Please set the JAVA_HOME variable!"}

JVM="java"
sdir="`dirname \"$0\"`"
ldir="`dirname "$(readlink -f "$0")"`"

DIR="$sdir"
if [ "$sdir" != "$ldir" ]; then
  DIR="$ldir"
fi

PATH=$JAVA_HOME/bin:$PATH nohup $JVM -classpath "/etc/ambari-infra-manager/conf:$DIR:$DIR/libs/*" $INFRA_MANAGER_OPTS org.apache.ambari.infra.InfraManager ${1+"$@"} &