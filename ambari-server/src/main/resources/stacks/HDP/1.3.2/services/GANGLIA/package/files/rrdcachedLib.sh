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

# Get access to Ganglia-wide constants etc.
source ./gangliaLib.sh;

RRDCACHED_BIN=/usr/bin/rrdcached;
RRDCACHED_PID_FILE=${GANGLIA_RUNTIME_DIR}/rrdcached.pid;
RRDCACHED_ALL_ACCESS_UNIX_SOCKET=${GANGLIA_RUNTIME_DIR}/rrdcached.sock;
RRDCACHED_LIMITED_ACCESS_UNIX_SOCKET=${GANGLIA_RUNTIME_DIR}/rrdcached.limited.sock;

function getRrdcachedLoggedPid()
{
    if [ -e "${RRDCACHED_PID_FILE}" ]
    then
        echo `cat ${RRDCACHED_PID_FILE}`;
    fi
}

function getRrdcachedRunningPid()
{
    rrdcachedLoggedPid=`getRrdcachedLoggedPid`;

    if [ -n "${rrdcachedLoggedPid}" ]
    then
        echo `ps -o pid=MYPID -p ${rrdcachedLoggedPid} | tail -1 | awk '{print $1}' | grep -v MYPID`;
    fi
}
