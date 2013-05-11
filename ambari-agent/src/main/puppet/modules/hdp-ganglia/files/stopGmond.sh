#!/bin/sh

#/*
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

cd `dirname ${0}`;

# Get all our common constants etc. set up.
# Pulls in gangliaLib.sh as well, so we can skip pulling it in again.
source ./gmondLib.sh;

function stopGmondForCluster()
{
    gmondClusterName=${1};

    gmondRunningPid=`getGmondRunningPid ${gmondClusterName}`;

    # Only go ahead with the termination if we could find a running PID.
    if [ -n "${gmondRunningPid}" ]
    then
      kill ${gmondRunningPid};
      echo "Stopped ${GMOND_BIN} for cluster ${gmondClusterName} (with PID ${gmondRunningPid})";
    fi
}

# main()
gmondClusterName=${1};

if [ "x" == "x${gmondClusterName}" ]
then
    # No ${gmondClusterName} passed in as command-line arg, so stop
    # all the gmonds we know about.
    for gmondClusterName in `getConfiguredGangliaClusterNames`
    do
        stopGmondForCluster ${gmondClusterName};
    done
else
    stopGmondForCluster ${gmondClusterName};
fi
