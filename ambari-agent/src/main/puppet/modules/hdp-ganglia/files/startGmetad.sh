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
source ./gmetadLib.sh;

# To get access to ${RRDCACHED_ALL_ACCESS_UNIX_SOCKET}.
source ./rrdcachedLib.sh;

# Before starting gmetad, start rrdcached.
./startRrdcached.sh;

if [ $? -eq 0 ] 
then
    gmetadRunningPid=`getGmetadRunningPid`;

    # Only attempt to start gmetad if there's not already one running.
    if [ -z "${gmetadRunningPid}" ]
    then
        env RRDCACHED_ADDRESS=${RRDCACHED_ALL_ACCESS_UNIX_SOCKET} \
                    ${GMETAD_BIN} --conf=${GMETAD_CONF_FILE} --pid-file=${GMETAD_PID_FILE};

        gmetadRunningPid=`getGmetadRunningPid`;

        if [ -n "${gmetadRunningPid}" ]
        then
            echo "Started ${GMETAD_BIN} with PID ${gmetadRunningPid}";
        else
            echo "Failed to start ${GMETAD_BIN}";
            exit 1;
        fi
    else
        echo "${GMETAD_BIN} already running with PID ${gmetadRunningPid}";
    fi
else
    echo "Not starting ${GMETAD_BIN} because starting ${RRDCACHED_BIN} failed.";
    exit 2;
fi
