#!/usr/bin/env bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
AMBARI_AGENT_PID_PATH="/var/run/ambari-agent/ambari-agent.pid";
RES="3";
if [ -f $AMBARI_AGENT_PID_PATH ]
then
  RES=`cat $AMBARI_AGENT_PID_PATH | xargs ps -f -p | wc -l`;
  AMBARI_AGENT_PID=`cat $AMBARI_AGENT_PID_PATH`; 
else 
  RES=-1; 
fi

if [ $RES -eq "2" ]
then
  echo "OK: Ambari Agent is running [PID:$AMBARI_AGENT_PID]";
  exit 0;
else
  echo "CRITICAL: Ambari Agent is not running [$AMBARI_AGENT_PID_PATH not found]";
  exit 2;
fi