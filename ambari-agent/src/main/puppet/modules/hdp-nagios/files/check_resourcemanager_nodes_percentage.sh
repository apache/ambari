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
HOST=$1
PORT=$2
#Resource manager nodes, with selected status, which number we want to know
NODE_STATUS=$3
WARN_PERCENT=$4
CRIT_PERCENT=$5
NODES="Nodes"

RESOURCEMANAGER_URL="http://$HOST:$PORT/ws/v1/cluster/metrics"
export PATH="/usr/bin:$PATH"
RESPONSE=`curl -s $RESOURCEMANAGER_URL`

if [ -z "$RESPONSE" ]; then 
  echo "CRITICAL: Can't get data from http://$HOST:$PORT/ws/v1/cluster/metrics" 
  exit 2;
fi 

#code below is parsing RESPONSE that we get from resourcemanager api, for number between "activeNodes": and ','
ACTIVE_NODES=`echo "$RESPONSE" | sed -nre 's/^.*"activeNodes":([[:digit:]]+).*$/\1/gp'`
LOST_NODES=`echo "$RESPONSE" | sed -nre 's/^.*"lostNodes":([[:digit:]]+).*$/\1/gp'`
UNHEALTHY_NODES=`echo "$RESPONSE" | sed -nre 's/^.*"unhealthyNodes":([[:digit:]]+).*$/\1/gp'`
DECOMMISSIONED_NODES=`echo "$RESPONSE" | sed -nre 's/^.*"decommissionedNodes":([[:digit:]]+).*$/\1/gp'`
REBOOTED_NODES=`echo "$RESPONSE" | sed -nre 's/^.*"rebootedNodes":([[:digit:]]+).*$/\1/gp'`

TOTAL_NODES_NUM=$(($ACTIVE_NODES+$LOST_NODES+$UNHEALTHY_NODES+$DECOMMISSIONED_NODES+$REBOOTED_NODES))
NODES_NUM=`echo "$RESPONSE" | sed -nre "s/^.*\"$NODE_STATUS$NODES\":([[:digit:]]+).*$/\1/gp"`
PERCENT=$(($NODES_NUM*100/$TOTAL_NODES_NUM))

if [[ "$PERCENT" -lt "$WARN_PERCENT" ]]; then
  echo "OK: total:<$TOTAL_NODES_NUM>, affected:<$NODES_NUM>"
  exit 0;
elif [[ "$PERCENT" -lt "$CRIT_PERCENT" ]]; then
  echo "WARNING: total:<$TOTAL_NODES_NUM>, affected:<$NODES_NUM>"
  exit 1;
else 
  echo "CRITICAL: total:<$TOTAL_NODES_NUM>, affected:<$NODES_NUM>"
  exit 2;
fi
