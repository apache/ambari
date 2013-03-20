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

# Slurp in all our user-customizable settings.
source ./gangliaEnv.sh;

# Get all our common constants etc. set up.
source ./rrdcachedLib.sh;

rrdcachedRunningPid=`getRrdcachedRunningPid`;

# Only attempt to start rrdcached if there's not already one running.
if [ -z "${rrdcachedRunningPid}" ]
then
    #changed because problem puppet had with nobody user
    #sudo -u ${GMETAD_USER} ${RRDCACHED_BIN} -p ${RRDCACHED_PID_FILE} \
    #         -m 664 -l unix:${RRDCACHED_ALL_ACCESS_UNIX_SOCKET} \
    #         -m 777 -P FLUSH,STATS,HELP -l unix:${RRDCACHED_LIMITED_ACCESS_UNIX_SOCKET} \
    #         -b /var/lib/ganglia/rrds -B
    su - ${GMETAD_USER} -c "${RRDCACHED_BIN} -p ${RRDCACHED_PID_FILE} \
             -m 664 -l unix:${RRDCACHED_ALL_ACCESS_UNIX_SOCKET} \
             -m 777 -P FLUSH,STATS,HELP -l unix:${RRDCACHED_LIMITED_ACCESS_UNIX_SOCKET} \
             -b ${RRDCACHED_BASE_DIR} -B"

    # Ideally, we'd use ${RRDCACHED_BIN}'s -s ${WEBSERVER_GROUP} option for 
    # this, but it doesn't take sometimes due to a lack of permissions,
    # so perform the operation explicitly to be super-sure.
    chgrp ${WEBSERVER_GROUP} ${RRDCACHED_ALL_ACCESS_UNIX_SOCKET};
    chgrp ${WEBSERVER_GROUP} ${RRDCACHED_LIMITED_ACCESS_UNIX_SOCKET};

    # Check to make sure rrdcached actually started up.
    rrdcachedRunningPid=`getRrdcachedRunningPid`;

    if [ -n "${rrdcachedRunningPid}" ]
    then
        echo "Started ${RRDCACHED_BIN} with PID ${rrdcachedRunningPid}";
    else
        echo "Failed to start ${RRDCACHED_BIN}";
        exit 1;
    fi
else
    echo "${RRDCACHED_BIN} already running with PID ${rrdcachedRunningPid}";
fi
