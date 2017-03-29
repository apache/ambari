#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

curr_dir=`pwd`
cd `dirname $0`; script_dir=`pwd`; cd $curr_dir

if [ "$PID_FILE" = "" ]; then
    LOGSEARCH_PID_DIR=$HOME
    PID_FILE=$LOGSEARCH_PID_DIR/logsearch-search-$USER.pid
fi

if [ -f ${PID_FILE} ]; then
    PID=`cat ${PID_FILE}`
    if kill -0 $PID 2>/dev/null; then
	echo "logsearch running with process id (${PID}). Killing..."
	kill $PID 2>/dev/null
	sleep 5
	if kill -0 $PID 2>/dev/null; then
	    echo "logsearch still running. Will kill process forcefully in another 10 seconds..."
	    sleep 10
	    kill -9 $PID 2>/dev/null
	    sleep 2
	fi
    fi

    if kill -0 $PID 2>/dev/null; then
	echo "ERROR: Even after all efforts to stop logsearch, it is still running. pid=$PID. Please manually kill the service and try again."
	exit 1
    fi
fi

