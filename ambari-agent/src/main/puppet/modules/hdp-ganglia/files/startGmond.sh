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

function startGmondForCluster()
{
    gmondClusterName=${1};

    gmondRunningPid=`getGmondRunningPid ${gmondClusterName}`;

    # Only attempt to start gmond if there's not already one running.
    if [ -z "${gmondRunningPid}" ]
    then
      gmondCoreConfFileName=`getGmondCoreConfFileName ${gmondClusterName}`;

      if [ -e "${gmondCoreConfFileName}" ]
      then 
        gmondPidFileName=`getGmondPidFileName ${gmondClusterName}`;

        ${GMOND_BIN} --conf=${gmondCoreConfFileName} --pid-file=${gmondPidFileName};
  
        gmondRunningPid=`getGmondRunningPid ${gmondClusterName}`;
  
        if [ -n "${gmondRunningPid}" ]
        then
            echo "Started ${GMOND_BIN} for cluster ${gmondClusterName} with PID ${gmondRunningPid}";
        else
            echo "Failed to start ${GMOND_BIN} for cluster ${gmondClusterName}";
            exit 1;
        fi
      fi 
    else
      echo "${GMOND_BIN} for cluster ${gmondClusterName} already running with PID ${gmondRunningPid}";
    fi
}

# main()
gmondClusterName=${1};

if [ "x" == "x${gmondClusterName}" ]
then
    # No ${gmondClusterName} passed in as command-line arg, so start 
    # all the gmonds we know about.
    for gmondClusterName in `getConfiguredGangliaClusterNames`
    do
        startGmondForCluster ${gmondClusterName};
    done
else
    # Just start the one ${gmondClusterName} that was asked for.
    startGmondForCluster ${gmondClusterName};
fi
