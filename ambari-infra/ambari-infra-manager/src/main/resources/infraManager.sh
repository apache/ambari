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

if [ "$INFRA_MANAGER_JAVA_MEM" = "" ]; then
    INFRA_MANAGER_JAVA_MEM="-Xmx1g"
fi

readlinkf(){
  # get real path on mac OSX
  perl -MCwd -e 'print Cwd::abs_path shift' "$1";
}

if [ "$(uname -s)" = 'Linux' ]; then
  SCRIPT_DIR="`dirname "$(readlink -f "$0")"`"
else
  SCRIPT_DIR="`dirname "$(readlinkf "$0")"`"
fi

INFRA_MANAGER_ROOT_DIR="`dirname \"$SCRIPT_DIR\"`"
INFRA_MANAGER_LIBS_DIR="$INFRA_MANAGER_ROOT_DIR/libs"

if [ "$INFRA_MANAGER_CONF_DIR" = "" ]; then
  if [ -d "$INFRA_MANAGER_ROOT_DIR/conf" ]; then
    INFRA_MANAGER_CONF_DIR="$INFRA_MANAGER_ROOT_DIR/conf"
  fi
fi

if [ -f "$INFRA_MANAGER_CONF_DIR/infra-manager-env.sh" ]; then
  source $INFRA_MANAGER_CONF_DIR/infra-manager-env.sh
fi

JVM="java"

if [ -x $JAVA_HOME/bin/java ]; then
  JVM=$JAVA_HOME/bin/java
fi

if [ ! -z "$INFRA_MANAGER_SOLR_CLIENT_SSL_INCLUDE" ]; then
  source $INFRA_MANAGER_SOLR_CLIENT_SSL_INCLUDE
fi

if [ -z "$INFRA_MANAGER_PID_FILE" ]; then
  INFRA_MANAGER_PID_DIR=$HOME
  export INFRA_MANAGER_PID_FILE=$INFRA_MANAGER_PID_DIR/infra-manager.pid
fi

if [ -z "$LOG_FILE" ]; then
  export LOG_FILE="infra-manager.log"
fi

INFRA_MANAGER_GC_LOGFILE="infra-manager-gc.log"

if [ -z "$LOG_PATH" ]; then
  LOG_FILE="$HOME/$LOG_FILE"
  INFRA_MANAGER_GC_LOGFILE="$HOME/$INFRA_MANAGER_GC_LOGFILE"
else
  LOG_PATH_WITHOUT_SLASH=${LOG_PATH%/}
  LOG_FILE="$LOG_PATH_WITHOUT_SLASH/$LOG_FILE"
  INFRA_MANAGER_GC_LOGFILE="$LOG_PATH_WITHOUT_SLASH/$INFRA_MANAGER_GC_LOGFILE"
fi

INFRA_MANAGER_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$INFRA_MANAGER_GC_LOGFILE"

function print_usage() {
  cat << EOF

   Usage: [<command>] [<arguments with flags>]

   commands:
     start                         Start Infra Manager
     stop                          Stop Infra Manager
     status                        Check Infra Manager is running or not (pid file)
     help                          Print usage


   start command arguments:
     -d, --debug                   Start java process in debug mode
     -f, --foreground              Start java process in foreground

EOF
}

function spinner() {
  local pid=$1
  local delay=0.5
  local spinstr='|/-\'
  while [ "$(ps aux | awk '{print $2}' | grep -w $pid)" ]; do
      local temp=${spinstr#?}
      printf " [%c]  " "$spinstr"
      local spinstr=$temp${spinstr%"$temp"}
      sleep $delay
      printf "\b\b\b\b\b\b"
  done
  printf "    \b\b\b\b"
}

function status() {
  echo "Checking Infra Manager status ..." >&2
  if [ -f "$INFRA_MANAGER_PID_FILE" ]; then
    INFRA_MANAGER_PID=`cat "$INFRA_MANAGER_PID_FILE"`
  else
    echo "Infra Manager pid not exists. (probably the process is not running)" >&2
    return 1
  fi

  if ps -p $INFRA_MANAGER_PID > /dev/null
   then
   echo "Infra Manager process is running. (pid: $INFRA_MANAGER_PID)" >&2
   return 0
  else
   echo "Infra Manager process is not running." >&2
   return 1
  fi
}

function start() {
  exit_status=$(status; echo $?)
  if [ "$exit_status" = "0" ]; then
    echo "Skipping start process."
    exit 0
  fi

  FG="false"
  INFRA_MANAGER_DEBUG_SUSPEND=${INFRA_MANAGER_DEBUG_SUSPEND:-n}
  INFRA_MANAGER_DEBUG_PORT=${INFRA_MANAGER_DEBUG_PORT:-"5005"}

  if [ "$INFRA_MANAGER_DEBUG" = "true" ]; then
    INFRA_MANAGER_JAVA_OPTS="$INFRA_MANAGER_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$INFRA_MANAGER_DEBUG_PORT,server=y,suspend=$INFRA_MANAGER_DEBUG_SUSPEND "
  fi

  if [ "$INFRA_MANAGER_SSL" = "true" ]; then
    INFRA_MANAGER_JAVA_OPTS="$INFRA_MANAGER_JAVA_OPTS -Djavax.net.ssl.keyStore=$INFRA_MANAGER_KEYSTORE_LOCATION -Djavax.net.ssl.keyStoreType=$INFRA_MANAGER_KEYSTORE_TYPE -Djavax.net.ssl.trustStore=$INFRA_MANAGER_TRUSTSTORE_LOCATION -Djavax.net.ssl.trustStoreType=$INFRA_MANAGER_TRUSTSTORE_TYPE"
  fi

  if [ "$INFRA_MANAGER_JMX" = "true" ]; then
   INFRA_MANAGER_JAVA_OPTS="$INFRA_MANAGER_JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=2099"
  fi

  if [ $# -gt 0 ]; then
    while true; do
      case "$1" in
          -f|--foreground)
              FG="true"
              shift
          ;;
          -d|--debug)
              if [ "$INFRA_MANAGER_DEBUG" != "true" ]; then
                INFRA_MANAGER_JAVA_OPTS="$INFRA_MANAGER_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$INFRA_MANAGER_DEBUG_PORT,server=y,suspend=$INFRA_MANAGER_DEBUG_SUSPEND "
              fi
              shift
          ;;
          *)
              if [ "${1:0:2}" == "-D" ]; then
                # pass thru any opts that begin with -D (java system props)
                INFRA_MANAGER_JAVA_OPTS+=("$1")
                echo "$INFRA_MANAGER_JAVA_OPTS"
                shift
              else
                if [ "$1" != "" ]; then
                  print_usage
                  exit 1
                else
                  break
                fi
              fi
          ;;
      esac
    done
  fi

  if [ $FG == "true" ]; then
    echo "Starting Infra Manager... (foreground) pid_file=$INFRA_MANAGER_PID_FILE"
    echo "Run command $JVM -cp '$INFRA_MANAGER_CONF_DIR:$INFRA_MANAGER_LIBS_DIR/*' $INFRA_MANAGER_GC_OPTS $INFRA_MANAGER_JAVA_OPTS $INFRA_MANAGER_JAVA_MEM org.apache.ambari.infra.InfraManager"
    $JVM -cp "$INFRA_MANAGER_CONF_DIR:$INFRA_MANAGER_LIBS_DIR/*" $INFRA_MANAGER_GC_OPTS $INFRA_MANAGER_JAVA_OPTS $INFRA_MANAGER_JAVA_MEM org.apache.ambari.infra.InfraManager
  else
   echo "Starting Infra Manager... Output file=$LOG_FILE pid_file=$INFRA_MANAGER_PID_FILE"
   echo "Run command nohup $JVM -cp '$INFRA_MANAGER_CONF_DIR:$INFRA_MANAGER_LIBS_DIR/*' $INFRA_MANAGER_GC_OPTS $INFRA_MANAGER_JAVA_OPTS $INFRA_MANAGER_JAVA_MEM org.apache.ambari.infra.InfraManager"
   nohup $JVM -cp "$INFRA_MANAGER_CONF_DIR:$INFRA_MANAGER_LIBS_DIR/*" $INFRA_MANAGER_GC_OPTS $INFRA_MANAGER_JAVA_OPTS $INFRA_MANAGER_JAVA_MEM org.apache.ambari.infra.InfraManager > $LOG_FILE 2>&1 &
  fi
}

function stop() {
  INFRA_MANAGER_STOP_WAIT=3
  if [ -f "$INFRA_MANAGER_PID_FILE" ]; then
    INFRA_MANAGER_PID=`cat "$INFRA_MANAGER_PID_FILE"`
  fi

  if [ "$INFRA_MANAGER_PID" != "" ]; then
    echo -e "Sending stop command to Infra Manager... Checking PID: $INFRA_MANAGER_PID."
    kill $INFRA_MANAGER_PID
      (loops=0
      while true
      do
        CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $INFRA_MANAGER_PID | sort -r | tr -d ' '`
        if [ "$CHECK_PID" != "" ]; then
          slept=$((loops * 2))
          if [ $slept -lt $INFRA_MANAGER_STOP_WAIT ]; then
            sleep 2
            loops=$[$loops+1]
          else
            exit # subshell!
          fi
        else
          exit # subshell!
        fi
      done) &
    spinner $!
    rm -f "$INFRA_MANAGER_PID_FILE"
  else
    echo -e "No Infra Manager process found to stop."
    exit 0
  fi

  CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $INFRA_MANAGER_PID | sort -r | tr -d ' '`
  if [ "$CHECK_PID" != "" ]; then
    echo -e "Infra Manager process $INFRA_MANAGER_PID is still running; forcefully killing it now."
    kill -9 $INFRA_MANAGER_PID
    echo "Killed process $INFRA_MANAGER_PID"
    rm -f "$INFRA_MANAGER_PID_FILE"
    sleep 1
  else
    echo "Infra Manager is stopped."
  fi

  CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $INFRA_MANAGER_PID | sort -r | tr -d ' '`
  if [ "$CHECK_PID" != "" ]; then
    echo "ERROR: Failed to kill Infra Manager Java process $INFRA_MANAGER_PID ... script fails."
    exit 1
  fi
}

if [ $# -gt 0 ]; then
    SCRIPT_CMD="$1"
    shift
else
   print_usage
   exit 1
fi

case $SCRIPT_CMD in
  start)
    start ${1+"$@"}
  ;;
  stop)
    stop
  ;;
  status)
    status
  ;;
  help)
    print_usage
    exit 0
  ;;
  *)
    print_usage
    exit 1
  ;;

esac