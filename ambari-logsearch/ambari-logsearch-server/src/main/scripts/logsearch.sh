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

if [ "$LOGSEARCH_JAVA_MEM" = "" ]; then
    LOGSEARCH_JAVA_MEM="-Xmx1g"
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

LOGSEARCH_ROOT_DIR="`dirname \"$SCRIPT_DIR\"`"
LOGSEARCH_LIBS_DIR="$LOGSEARCH_ROOT_DIR/libs"
LOGSEARCH_WEBAPP_DIR="$LOGSEARCH_ROOT_DIR/webapp"

if [ "$LOGSEARCH_CONF_DIR" = "" ]; then
  if [ -d "$LOGSEARCH_ROOT_DIR/conf" ]; then
    LOGSEARCH_CONF_DIR="$LOGSEARCH_ROOT_DIR/conf"
  fi
fi

if [ -f "$LOGSEARCH_CONF_DIR/logsearch-env.sh" ]; then
  source $LOGSEARCH_CONF_DIR/logsearch-env.sh
fi

JVM="java"

if [ -x $JAVA_HOME/bin/java ]; then
  JVM=$JAVA_HOME/bin/java
fi

if [ ! -z "$LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE" ]; then
  source $LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE
fi

if [ -z "$LOGSEARCH_PID_FILE" ]; then
  LOGSEARCH_DEFAULT_PID_DIR="/var/run/ambari-logsearch-portal"
  if [ -d "$LOGSEARCH_DEFAULT_PID_DIR" ]; then
    LOGSEARCH_PID_DIR=$LOGSEARCH_DEFAULT_PID_DIR
  else
    LOGSEARCH_PID_DIR=$HOME
  fi
  export LOGSEARCH_PID_FILE=$LOGSEARCH_PID_DIR/logsearch.pid
fi

if [ -z "$LOG_FILE" ]; then
  export LOG_FILE="logsearch.log"
fi

LOGSEARCH_GC_LOGFILE="logsearch_gc.log"

if [ -z "$LOG_PATH" ]; then
  LOG_FILE="$HOME/$LOG_FILE"
  LOGSEARCH_GC_LOGFILE="$HOME/$LOGSEARCH_GC_LOGFILE"
else
  LOG_PATH_WITHOUT_SLASH=${LOG_PATH%/}
  LOG_FILE="$LOG_PATH_WITHOUT_SLASH/$LOG_FILE"
  LOGSEARCH_GC_LOGFILE="$LOG_PATH_WITHOUT_SLASH/$LOGSEARCH_GC_LOGFILE"
fi

LOGSEARCH_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$LOGSEARCH_GC_LOGFILE"

function print_usage() {
  cat << EOF

   Usage: [<command>] [<arguments with flags>]

   commands:
     start                         Start Log Search
     stop                          Stop Log Search
     status                        Check Log Search status (pid file)
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
  echo "Checking Log Search status ..." >&2
  if [ -f "$LOGSEARCH_PID_FILE" ]; then
    LOGSEARCH_PID=`cat "$LOGSEARCH_PID_FILE"`
  else
    echo "Log Search pid not exists. (probably the process is not running)" >&2
    return 1
  fi

  if ps -p $LOGSEARCH_PID > /dev/null
   then
   echo "Log Search process is running. (pid: $LOGSEARCH_PID)" >&2
   return 0
  else
   echo "Log Search process is not running." >&2
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
  LOGSEARCH_DEBUG_SUSPEND=${LOGSEARCH_DEBUG_SUSPEND:-n}
  LOGSEARCH_DEBUG_PORT=${LOGSEARCH_DEBUG_PORT:-"5005"}

  if [ "$LOGSEARCH_DEBUG" = "true" ]; then
    LOGSEARCH_JAVA_OPTS="$LOGSEARCH_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$LOGSEARCH_DEBUG_PORT,server=y,suspend=$LOGSEARCH_DEBUG_SUSPEND "
  fi

  if [ "$LOGSEARCH_SSL" = "true" ]; then
    LOGSEARCH_JAVA_OPTS="$LOGSEARCH_JAVA_OPTS -Djavax.net.ssl.keyStore=$LOGSEARCH_KEYSTORE_LOCATION -Djavax.net.ssl.keyStoreType=$LOGSEARCH_KEYSTORE_TYPE -Djavax.net.ssl.trustStore=$LOGSEARCH_TRUSTSTORE_LOCATION -Djavax.net.ssl.trustStoreType=$LOGSEARCH_TRUSTSTORE_TYPE"
  fi

  if [ "$LOGSEARCH_JMX" = "true" ]; then
   LOGSEARCH_JAVA_OPTS="$LOGSEARCH_JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=2099"
  fi

  if [ $# -gt 0 ]; then
    while true; do
      case "$1" in
          -f|--foreground)
              FG="true"
              shift
          ;;
          -d|--debug)
              if [ "$LOGSEARCH_DEBUG" != "true" ]; then
                LOGSEARCH_JAVA_OPTS="$LOGSEARCH_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$LOGSEARCH_DEBUG_PORT,server=y,suspend=$LOGSEARCH_DEBUG_SUSPEND "
              fi
              shift
          ;;
          *)
              if [ "${1:0:2}" == "-D" ]; then
                # pass thru any opts that begin with -D (java system props)
                LOGSEARCH_JAVA_OPTS+=("$1")
                echo "$LOGSEARCH_JAVA_OPTS"
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
    echo "Starting Log Search... (foreground) pid_file=$LOGSEARCH_PID_FILE"
    echo "Run command $JVM -cp '$LOGSEARCH_CONF_DIR:$LOGSEARCH_WEBAPP_DIR:$LOGSEARCH_LIBS_DIR/*' $LOGSEARCH_GC_OPTS $LOGSEARCH_JAVA_OPTS $LOGSEARCH_JAVA_MEM org.apache.ambari.logsearch.LogSearch"
    $JVM -cp "$LOGSEARCH_CONF_DIR:$LOGSEARCH_WEBAPP_DIR:$LOGSEARCH_LIBS_DIR/*" $LOGSEARCH_GC_OPTS $LOGSEARCH_JAVA_OPTS $LOGSEARCH_JAVA_MEM org.apache.ambari.logsearch.LogSearch
  else
   echo "Starting Log Search... Output file=$LOG_FILE pid_file=$LOGSEARCH_PID_FILE"
   echo "Run command nohup $JVM -cp '$LOGSEARCH_CONF_DIR:$LOGSEARCH_WEBAPP_DIR:$LOGSEARCH_LIBS_DIR/*' $LOGSEARCH_GC_OPTS $LOGSEARCH_JAVA_OPTS $LOGSEARCH_JAVA_MEM org.apache.ambari.logsearch.LogSearch"
   nohup $JVM -cp "$LOGSEARCH_CONF_DIR:$LOGSEARCH_WEBAPP_DIR:$LOGSEARCH_LIBS_DIR/*" $LOGSEARCH_GC_OPTS $LOGSEARCH_JAVA_OPTS $LOGSEARCH_JAVA_MEM org.apache.ambari.logsearch.LogSearch > $LOG_FILE 2>&1 &
  fi
}

function stop() {
  LOGSEARCH_STOP_WAIT=3
  if [ -f "$LOGSEARCH_PID_FILE" ]; then
    LOGSEARCH_PID=`cat "$LOGSEARCH_PID_FILE"`
  fi

  if [ "$LOGSEARCH_PID" != "" ]; then
    echo -e "Sending stop command to Log Search... Checking PID: $LOGSEARCH_PID."
    kill $LOGSEARCH_PID
      (loops=0
      while true
      do
        CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $LOGSEARCH_PID | sort -r | tr -d ' '`
        if [ "$CHECK_PID" != "" ]; then
          slept=$((loops * 2))
          if [ $slept -lt $LOGSEARCH_STOP_WAIT ]; then
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
    rm -f "$LOGSEARCH_PID_FILE"
  else
    echo -e "No Log Search process found to stop."
    exit 0
  fi

  CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $LOGSEARCH_PID | sort -r | tr -d ' '`
  if [ "$CHECK_PID" != "" ]; then
    echo -e "Log Search process $LOGSEARCH_PID is still running; forcefully killing it now."
    kill -9 $LOGSEARCH_PID
    echo "Killed process $LOGSEARCH_PID"
    rm -f "$LOGSEARCH_PID_FILE"
    sleep 1
  else
    echo "Log Search is stopped."
  fi

  CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $LOGSEARCH_PID | sort -r | tr -d ' '`
  if [ "$CHECK_PID" != "" ]; then
    echo "ERROR: Failed to kill Log Search Java process $LOGSEARCH_PID ... script fails."
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