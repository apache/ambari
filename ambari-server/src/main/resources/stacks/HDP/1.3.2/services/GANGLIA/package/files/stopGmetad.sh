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

gmetadRunningPid=`getGmetadRunningPid`;

# Only go ahead with the termination if we could find a running PID.
if [ -n "${gmetadRunningPid}" ]
then
    kill -KILL ${gmetadRunningPid};
    echo "Stopped ${GMETAD_BIN} (with PID ${gmetadRunningPid})";
fi

# Poll again.
gmetadRunningPid=`getGmetadRunningPid`;

# Once we've killed gmetad, there should no longer be a running PID.
if [ -z "${gmetadRunningPid}" ]
then
    # It's safe to stop rrdcached now.
    ./stopRrdcached.sh;
fi
