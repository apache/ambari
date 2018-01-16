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

JVM="java"

if [ -x $JAVA_HOME/bin/java ]; then
  JVM=$JAVA_HOME/bin/java
fi

if [ "$LOGFEEDER_JAVA_MEM" = "" ]; then
  LOGFEEDER_JAVA_MEM="-Xmx512m"
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

LOGFEEDER_ROOT_DIR="`dirname \"$SCRIPT_DIR\"`"
LOGFEEDER_LIBS_DIR="$LOGFEEDER_ROOT_DIR/libs"

if [ "$LOGFEEDER_CONF_DIR" = "" ]; then
  LOGFEEDER_CONF_DIR="/etc/ambari-logsearch-logfeeder/conf"
  if [ ! -d $LOGFEEDER_CONF_DIR ]; then
    if [ -d "$LOGFEEDER_ROOT_DIR/conf" ]; then
      LOGFEEDER_CONF_DIR="$LOGFEEDER_ROOT_DIR/conf"
    fi
  fi
fi

if [ -f "$LOGFEEDER_CONF_DIR/logfeeder-env.sh" ]; then
  source $LOGFEEDER_CONF_DIR/logfeeder-env.sh
fi

if [ ! -z "$LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE" ]; then
  source $LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE
fi

if [ -z "$LOGFEEDER_PID_FILE" ]; then
  LOGFEEDER_PID_DIR=$HOME
  export LOGFEEDER_PID_FILE=$LOGFEEDER_PID_DIR/logfeeder.pid
fi

if [ -z "$LOG_FILE" ]; then
  export LOG_FILE="logfeeder.log"
fi

LOGFEEDER_GC_LOGFILE="logfeeder_gc.log"

if [ -z "$LOG_PATH" ]; then
  LOG_FILE="$HOME/$LOG_FILE"
  LOGFEEDER_GC_LOGFILE="$HOME/$LOGFEEDER_GC_LOGFILE"
else
  LOG_PATH_WITHOUT_SLASH=${LOG_PATH%/}
  LOG_FILE="$LOG_PATH_WITHOUT_SLASH/$LOG_FILE"
  LOGFEEDER_GC_LOGFILE="$LOG_PATH_WITHOUT_SLASH/$LOGFEEDER_GC_LOGFILE"
fi

LOGFEEDER_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$LOGFEEDER_GC_LOGFILE"

function print_usage() {
  cat << EOF

   Usage: [<command>] [<arguments with flags>]

   commands:
     start                         Start Log Feeder
     stop                          Stop Log Feeder
     status                        Check Log Feeder status (pid file)
     test                          Test Log Feeder shipper configs
     help                          Print usage


   start command arguments:
     -d, --debug                   Start java process in debug mode
     -f, --foreground              Start java process in foreground

   test command arguments:
     -h, --help                    Print usage
     -tle, --test-log-entry        Log entry to test if it's parseable (required)
     -tsc, --test-shipper-config   Shipper configuration file for testing if log entry is parseable (required)
     -tgc, --test-global-config    Global configuration files (comma separated list) for testing if log entry is parseable
     -tli, --test-log-id           The id of the log to test

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
  echo "Checking Log Feeder status ..." >&2
  if [ -f "$LOGFEEDER_PID_FILE" ]; then
    LOGFEEDER_PID=`cat "$LOGFEEDER_PID_FILE"`
  else
    echo "Log Feeder pid not exists. (probably the process is not running)" >&2
    return 1
  fi

  if ps -p $LOGFEEDER_PID > /dev/null
   then
   echo "Log Feeder process is running. (pid: $LOGFEEDER_PID)" >&2
   return 0
  else
   echo "Log Feeder process is not running." >&2
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
  LOGFEEDER_DEBUG_SUSPEND=${LOGFEEDER_DEBUG_SUSPEND:-n}
  LOGFEEDER_DEBUG_PORT=${LOGFEEDER_DEBUG_PORT:-"5006"}

  if [ "$LOGFEEDER_DEBUG" = "true" ]; then
    LOGFEEDER_JAVA_OPTS="$LOGFEEDER_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$LOGFEEDER_DEBUG_PORT,server=y,suspend=$LOGFEEDER_DEBUG_SUSPEND "
  fi

  if [ "$LOGFEEDER_SSL" = "true" ]; then
    LOGFEEDER_JAVA_OPTS="$LOGFEEDER_JAVA_OPTS -Djavax.net.ssl.keyStore=$LOGFEEDER_KEYSTORE_LOCATION -Djavax.net.ssl.keyStoreType=$LOGFEEDER_KEYSTORE_TYPE -Djavax.net.ssl.trustStore=$LOGFEEDER_TRUSTSTORE_LOCATION -Djavax.net.ssl.trustStoreType=$LOGFEEDER_TRUSTSTORE_TYPE"
  fi

  if [ "$LOGFEEDER_JMX" = "true" ]; then
   LOGFEEDER_JAVA_OPTS="$LOGFEEDER_JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=2098"
  fi

  if [ $# -gt 0 ]; then
    while true; do
      case "$1" in
          -f|--foreground)
              FG="true"
              shift
          ;;
          -d|--debug)
              if [ "$LOGFEEDER_DEBUG" != "true" ]; then
                LOGFEEDER_JAVA_OPTS="$LOGFEEDER_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$LOGFEEDER_DEBUG_PORT,server=y,suspend=$LOGFEEDER_DEBUG_SUSPEND "
              fi
              shift
          ;;
          *)
              if [ "${1:0:2}" == "-D" ]; then
                # pass thru any opts that begin with -D (java system props)
                LOGFEEDER_JAVA_OPTS+=("$1")
                echo "$LOGFEEDER_JAVA_OPTS"
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
    echo "Starting logfeeder... (foreground) pid_file=$LOGFEEDER_PID_FILE"
    echo "Run command $JVM -cp '$LOGFEEDER_CONF_DIR:$LOGFEEDER_LIBS_DIR/*' $LOGFEEDER_GC_OPTS $LOGFEEDER_JAVA_OPTS $LOGFEEDER_JAVA_MEM org.apache.ambari.logfeeder.LogFeeder"
    $JVM -cp "$LOGFEEDER_CONF_DIR:$LOGFEEDER_LIBS_DIR/*" $LOGFEEDER_GC_OPTS $LOGFEEDER_JAVA_OPTS $LOGFEEDER_JAVA_MEM org.apache.ambari.logfeeder.LogFeeder
  else
   echo "Starting logfeeder... Output file=$LOG_FILE pid_file=$LOGFEEDER_PID_FILE"
   echo "Run command nohup $JVM -cp '$LOGFEEDER_CONF_DIR:$LOGFEEDER_LIBS_DIR/*' $LOGFEEDER_GC_OPTS $LOGFEEDER_JAVA_OPTS $LOGFEEDER_JAVA_MEM org.apache.ambari.logfeeder.LogFeeder"
   nohup $JVM -cp "$LOGFEEDER_CONF_DIR:$LOGFEEDER_LIBS_DIR/*" $LOGFEEDER_GC_OPTS $LOGFEEDER_JAVA_OPTS $LOGFEEDER_JAVA_MEM org.apache.ambari.logfeeder.LogFeeder > $LOG_FILE 2>&1 &
  fi
}

function stop() {
  LOGFEEDER_STOP_WAIT=3
  if [ -f "$LOGFEEDER_PID_FILE" ]; then
    LOGFEEDER_PID=`cat "$LOGFEEDER_PID_FILE"`
  fi

  if [ "$LOGFEEDER_PID" != "" ]; then
    echo -e "Sending stop command to Log Feeder... Checking PID: $LOGFEEDER_PID."
    kill $LOGFEEDER_PID
      (loops=0
      while true
      do
        CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $LOGFEEDER_PID | sort -r | tr -d ' '`
        if [ "$CHECK_PID" != "" ]; then
          slept=$((loops * 2))
          if [ $slept -lt $LOGFEEDER_STOP_WAIT ]; then
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
    rm -f "$LOGFEEDER_PID_FILE"
  else
    echo -e "No LogFeeder process found to stop."
    exit 0
  fi

  CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $LOGFEEDER_PID | sort -r | tr -d ' '`
  if [ "$CHECK_PID" != "" ]; then
    echo -e "Log Feeder process $LOGFEEDER_PID is still running; forcefully killing it now."
    kill -9 $LOGFEEDER_PID
    echo "Killed process $LOGFEEDER_PID"
    rm -f "$LOGFEEDER_PID_FILE"
    sleep 1
  else
    echo "Log Feeder is stopped."
  fi

  CHECK_PID=`ps auxww | awk '{print $2}' | grep -w $LOGFEEDER_PID | sort -r | tr -d ' '`
  if [ "$CHECK_PID" != "" ]; then
    echo "ERROR: Failed to kill Log Feeder Java process $LOGFEEDER_PID ... script fails."
    exit 1
  fi
}

function test() {
  echo "Running command: $JVM -cp "$LOGFEEDER_CONF_DIR:$LOGFEEDER_LIBS_DIR/*" org.apache.ambari.logfeeder.LogFeederCommandLine --test ${@}"
  $JVM -cp "$LOGFEEDER_CONF_DIR:$LOGFEEDER_LIBS_DIR/*" $LOGFEEDER_JAVA_OPTS org.apache.ambari.logfeeder.LogFeederCommandLine --test ${@}
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
  test)
    test ${1+"$@"}
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