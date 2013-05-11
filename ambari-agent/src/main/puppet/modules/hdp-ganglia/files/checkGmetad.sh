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

# Before checking gmetad, check rrdcached.
./checkRrdcached.sh;

gmetadRunningPid=`getGmetadRunningPid`;

if [ -n "${gmetadRunningPid}" ]
then
  echo "${GMETAD_BIN} running with PID ${gmetadRunningPid}";
else
  echo "Failed to find running ${GMETAD_BIN}";
  exit 1;
fi
