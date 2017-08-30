#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

## Runs superset as a daemon
## Environment Variables used by this script -
## SUPERSET_CONFIG_DIR - directory having superset config files
## SUPERSET_LOG_DIR - directory used to store superset logs
## SUPERSET_PID_DIR - directory used to store pid file

usage="Usage: superset.sh (start|stop|status)"

if [ $# -le 0 ]; then
  echo $usage
  exit 1
fi

command=$1

CONF_DIR="${SUPERSET_CONFIG_DIR:=/etc/superset/conf}"
LOG_DIR="${SUPERSET_LOG_DIR:=/var/log/superset}"
PID_DIR="${SUPERSET_PID_DIR:=/var/run/superset}"
TIMEOUT="${SUPERSET_TIMEOUT:=60}"
WEBSERVER_ADDRESS="${SUPERSET_WEBSERVER_ADDRESS:=0.0.0.0}"
WEBSERVER_PORT="${SUPERSET_WEBSERVER_PORT:=9088}"
WORKERS="${SUPERSET_WORKERS:=4}"
BIN_DIR="${SUPERSET_BIN_DIR}"

pid=$PID_DIR/superset.pid

case $command in
  (start)

    if [ -f $pid ]; then
      if kill -0 `cat $pid| head -n 1` > /dev/null 2>&1; then
        echo Superset node running as process `cat $pid | head -n 1`.  Stop it first.
        exit 1
      fi
    fi

    $BIN_DIR/gunicorn -D --workers $WORKERS -p $pid --log-file $LOG_DIR/superset.log -t $TIMEOUT -b $WEBSERVER_ADDRESS:$WEBSERVER_PORT --limit-request-line 0 --limit-request-field_size 0 superset:app

    echo "Started Superset"
    ;;

  (stop)

    if [ -f $pid ]; then
      TARGET_PID=`cat $pid | head -n 1`
      if kill -0 $TARGET_PID > /dev/null 2>&1; then
        echo Stopping process `cat $pid | head -n 1`...
        kill $TARGET_PID
      else
        echo No superset node to stop
      fi
      rm -f $pid
    else
      echo No superset node to stop
    fi
    ;;

   (status)
    if [ -f $pid ]; then
      if kill -0 `cat $pid | head -n 1` > /dev/null 2>&1; then
        echo RUNNING
        exit 0
      else
        echo STOPPED
      fi
    else
      echo STOPPED
    fi
    ;;

  (*)
    echo $usage
    exit 1
    ;;
esac