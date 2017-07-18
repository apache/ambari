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

# Init script for Gremlin Server so it automatically starts/stops with the machine.
#
# To install:
# 1)  Add a symlink to this file in /etc/init.d/ under the name you'd like to see the service
#     For example, to name the service "gremlin-server": ln -s /usr/local/packages/dynamodb-titan100-storage-backend-1.0.0-hadoop1/bin/gremlin-server-service.sh /etc/init.d/gremlin-server
# 2a) If you're running RH: chkconfig --add gremlin-server
# 2b) If you're running Ubuntu: update-rc.d gremlin-server defaults
#
# You have to SET the Gremlin Server installation directory here:
PID_FILE=$2
GREMLIN_SERVER_LOG_FILE=$3
GREMLIN_SERVER_ERR_FILE=$4
GREMLIN_SERVER_BIN_DIR=$5
GREMLIN_SERVER_CONF_DIR=$6


usage() {
  echo "Usage: `basename $0`: start|stop|status"
  exit 1
}

status() {
  echo "get program status"
  local pid
  if [[ -f "$PID_FILE" && -s "$PID_FILE" ]]; then
  	#statements
        pid=$(cat $PID_FILE)
  	if kill -0 $pid > /dev/null 2>&1; then
  		# pid exists
                echo "program is running"
  		return 0
  	fi
  else
  	echo "program is not running"
  fi
  return 1
}

start() {
  if ! status ; then
      echo "start program"
      /usr/bin/nohup ${GREMLIN_SERVER_BIN_DIR}/gremlin-server.sh ${GREMLIN_SERVER_CONF_DIR}/gremlin-server.yaml 1>$GREMLIN_SERVER_LOG_FILE 2>${GREMLIN_SERVER_ERR_FILE} &
      echo $! > $PID_FILE
      sleep 50
  fi
}

stop() {
	local pid
	if status ; then
		echo "stop program"
		pid=`cat $PID_FILE`
		kill -9 $pid
                rm -f $PID_FILE
	fi
}

case "$1" in
	start)
  start
  ;;
  stop)
  stop
  ;;
  status)
  status
  ;;
  *)
  usage
  ;;
esac
