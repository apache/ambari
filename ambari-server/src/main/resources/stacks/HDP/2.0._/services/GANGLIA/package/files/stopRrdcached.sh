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
source ./rrdcachedLib.sh;

rrdcachedRunningPid=`getRrdcachedRunningPid`;

# Only go ahead with the termination if we could find a running PID.
if [ -n "${rrdcachedRunningPid}" ]
then
    kill -TERM ${rrdcachedRunningPid};
    # ${RRDCACHED_BIN} takes a few seconds to drain its buffers, so wait 
    # until we're sure it's well and truly dead. 
    #
    # Without this, an immediately following startRrdcached.sh won't do
    # anything, because it still sees this soon-to-die instance alive,
    # and the net result is that after a few seconds, there's no
    # ${RRDCACHED_BIN} running on the box anymore.
    sleep 5;
    echo "Stopped ${RRDCACHED_BIN} (with PID ${rrdcachedRunningPid})";
fi 
