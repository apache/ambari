#!/usr/bin/env bash

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
# See the License for the specific

PIDFILE=/var/run//var/run/ambari-metrics-anomaly-detection/ambari-metrics-admanager.pid
OUTFILE=/var/log/ambari-metrics-anomaly-detection/ambari-metrics-admanager.out

CONF_DIR=/etc/ambari-metrics-anomaly-detection/conf
DAEMON_NAME=ams_admanager

STOP_TIMEOUT=5

function write_pidfile
{
    local pidfile="$1"
    echo $! > "${pidfile}" 2>/dev/null
    if [[ $? -gt 0 ]]; then
      echo "ERROR:  Cannot write pid ${pidfile}." | tee -a $STARTUPFILE
      exit 1;
    fi
}

function java_setup
{
  # Bail if we did not detect it
  if [[ -z "${JAVA_HOME}" ]]; then
    echo "ERROR: JAVA_HOME is not set and could not be found."
    exit 1
  fi

  if [[ ! -d "${JAVA_HOME}" ]]; then
    echo "ERROR: JAVA_HOME ${JAVA_HOME} does not exist."
    exit 1
  fi

  JAVA="${JAVA_HOME}/bin/java"

  if [[ ! -x "$JAVA" ]]; then
    echo "ERROR: $JAVA is not executable."
    exit 1
  fi
}

function daemon_status()
{
  #
  # LSB 4.1.0 compatible status command (1)
  #
  # 0 = program is running
  # 1 = dead, but still a pid (2)
  # 2 = (not used by us)
  # 3 = not running
  #
  # 1 - this is not an endorsement of the LSB
  #
  # 2 - technically, the specification says /var/run/pid, so
  #     we should never return this value, but we're giving
  #     them the benefit of a doubt and returning 1 even if
  #     our pid is not in in /var/run .
  #

  local pidfile="$1"
  shift

  local pid

  if [[ -f "${pidfile}" ]]; then
    pid=$(cat "${pidfile}")
    if ps -p "${pid}" > /dev/null 2>&1; then
      return 0
    fi
    return 1
  fi
  return 3
}

function start()
{
  java_setup

  daemon_status "${PIDFILE}"
  if [[ $? == 0  ]]; then
    echo "AMS AD Manager is running as process $(cat "${PIDFILE}"). Exiting" | tee -a $STARTUPFILE
    exit 0
  else
    # stale pid file, so just remove it and continue on
    rm -f "${PIDFILE}" >/dev/null 2>&1
  fi

  nohup "${JAVA}" "-Xms$AMS_AD_HEAPSIZE" "-Xmx$AMS_AD_HEAPSIZE" ${AMS_AD_OPTS} "-Dlog4j.configuration=file://$CONF_DIR/log4j.properties" "-jar" "/usr/lib/ambari-metrics-anomaly-detection/ambari-metrics-anomaly-detection-service.jar" "server" "${CONF_DIR}/config.yaml" "$@" > $OUTFILE 2>&1 &
  PID=$!
  write_pidfile "${PIDFILE}"
  sleep 2

  echo "Verifying ${DAEMON_NAME} process status..."
  if [ -z "`ps ax -o pid | grep ${PID}`" ]; then
    if [ -s ${OUTFILE} ]; then
      echo "ERROR: ${DAEMON_NAME} start failed. For more details, see ${OUTFILE}:"
      echo "===================="
      tail -n 10 ${OUTFILE}
      echo "===================="
    else
      echo "ERROR: ${DAEMON_NAME} start failed"
      rm -f ${PIDFILE}
    fi
    echo "Anomaly Detection Manager out at: ${OUTFILE}"
    exit -1
  fi

  rm -f $STARTUPFILE #Deleting startup file
  echo "Anomaly Detection Manager successfully started."
  }

function stop()
{
  pidfile=${PIDFILE}

  if [[ -f "${pidfile}" ]]; then
    pid=$(cat "$pidfile")

    kill "${pid}" >/dev/null 2>&1
    sleep "${STOP_TIMEOUT}"

    if kill -0 "${pid}" > /dev/null 2>&1; then
      echo "WARNING: ${DAEMON_NAME} did not stop gracefully after ${STOP_TIMEOUT} seconds: Trying to kill with kill -9"
      kill -9 "${pid}" >/dev/null 2>&1
    fi

    if ps -p "${pid}" > /dev/null 2>&1; then
      echo "ERROR: Unable to kill ${pid}"
    else
      rm -f "${pidfile}" >/dev/null 2>&1
    fi
  fi
}

# execute ams-env.sh
if [[ -f "${CONF_DIR}/ams-admanager-env.sh" ]]; then
  . "${CONF_DIR}/ams-admanager-env.sh"
else
  echo "ERROR: Cannot execute ${CONF_DIR}/ams-admanager-env.sh." 2>&1
  exit 1
fi

# set these env variables only if they were not set by ams-env.sh
: ${AMS_AD_LOG_DIR:=/var/log/ambari-metrics-anomaly-detection}

# set pid dir path
if [[ -n "${AMS_AD_PID_DIR}" ]]; then
  PIDFILE=${AMS_AD_PID_DIR}/admanager.pid
fi

# set out file path
if [[ -n "${AMS_AD_LOG_DIR}" ]]; then
  OUTFILE=${AMS_AD_LOG_DIR}/ambari-metrics-admanager.out
fi

#TODO manage 3 hbase daemons for start/stop/status
case "$1" in

	start)
    start

    ;;
	stop)
    stop

    ;;
	status)
	    daemon_status "${PIDFILE}"
	    if [[ $? == 0  ]]; then
            echo "AMS AD Manager is running as process $(cat "${PIDFILE}")."
        else
            echo "AMS AD Manager is not running."
        fi
    ;;
	restart)
	  stop
	  start
	;;

esac
