#!/usr/bin/env bash
#
#/**
# * Copyright 2007 The Apache Software Foundation
# *
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
# 
# Runs a Hadoop hms command as a daemon.
#
# Environment Variables
#
#   HMS_CONF_DIR   Alternate hms conf dir. Default is ${HMS_HOME}/conf.
#   HMS_LOG_DIR    Where log files are stored.  PWD by default.
#   HMS_PID_DIR    The pid files are stored. /tmp by default.
#   HMS_IDENT_STRING   A string representing this instance of hadoop. $USER by default
#   HMS_NICENESS The scheduling priority for daemons. Defaults to 0.
#
# Modelled after $HADOOP_HOME/bin/hadoop-daemon.sh

usage="Usage: hms-daemon.sh [--config <conf-dir>]\
 (start|stop|restart) <hms-command> \
 <args...>"

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`

. "$bin"/hms-config.sh

# get arguments
startStop=$1
shift

command=$1
shift

hms_rotate_log ()
{
    log=$1;
    num=5;
    if [ -n "$2" ]; then
    num=$2
    fi
    if [ -f "$log" ]; then # rotate logs
    while [ $num -gt 1 ]; do
        prev=`expr $num - 1`
        [ -f "$log.$prev" ] && mv -f "$log.$prev" "$log.$num"
        num=$prev
    done
    mv -f "$log" "$log.$num";
    fi
}

wait_until_done ()
{
    p=$1
    cnt=${HMS_SLAVE_TIMEOUT:-300}
    origcnt=$cnt
    while kill -0 $p > /dev/null 2>&1; do
      if [ $cnt -gt 1 ]; then
        cnt=`expr $cnt - 1`
        sleep 1
      else
        echo "Process did not complete after $origcnt seconds, killing."
        kill -9 $p
        exit 1
      fi
    done
    return 0
}

# get log directory
if [ "$HMS_LOG_DIR" = "" ]; then
  export HMS_LOG_DIR="$HMS_HOME/logs"
fi
mkdir -p "$HMS_LOG_DIR"

if [ "$HMS_PID_DIR" = "" ]; then
  HMS_PID_DIR=/tmp
fi

if [ "$HMS_IDENT_STRING" = "" ]; then
  export HMS_IDENT_STRING="$USER"
fi

# Some variables
# Work out java location so can print version into log.
if [ "$JAVA_HOME" != "" ]; then
  #echo "run java in $JAVA_HOME"
  JAVA_HOME=$JAVA_HOME
fi
if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi
JAVA=$JAVA_HOME/bin/java
export HMS_LOGFILE=hms-$HMS_IDENT_STRING-$command-$HOSTNAME.log
export HMS_ROOT_LOGGER="INFO,DRFA"
logout=$HMS_LOG_DIR/hms-$HMS_IDENT_STRING-$command-$HOSTNAME.out  
loglog="${HMS_LOG_DIR}/${HMS_LOGFILE}"
pid=$HMS_PID_DIR/hms-$HMS_IDENT_STRING-$command.pid

# Set default scheduling priority
if [ "$HMS_NICENESS" = "" ]; then
    export HMS_NICENESS=0
fi

case $startStop in

  (start)
    mkdir -p "$HMS_PID_DIR"
    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
    fi

    hms_rotate_log $logout
    echo starting $command, logging to $logout
    # Add to the command log file vital stats on our environment.
    echo "`date` Starting $command on `hostname`" >> $loglog
    echo "ulimit -n `ulimit -n`" >> $loglog 2>&1
    nohup nice -n $HMS_NICENESS "$HMS_HOME"/bin/hms \
        --config "${HMS_CONF_DIR}" \
        $command $startStop "$@" > "$logout" 2>&1 < /dev/null &
    echo $! > $pid
    sleep 1; head "$logout"
    ;;

  (stop)
    if [ -f $pid ]; then
      # kill -0 == see if the PID exists 
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo -n stopping $command
        if [ "$command" = "master" ]; then
          echo "`date` Killing $command" >> $loglog
          kill -9 `cat $pid` > /dev/null 2>&1
        else
          echo "`date` Killing $command" >> $loglog
          kill `cat $pid` > /dev/null 2>&1
        fi
        while kill -0 `cat $pid` > /dev/null 2>&1; do
          echo -n "."
          sleep 1;
        done
        rm $pid
        echo
      else
        retval=$?
        echo no $command to stop because kill -0 of pid `cat $pid` failed with status $retval
      fi
    else
      echo no $command to stop because no pid file $pid
    fi
    ;;

  (restart)
    thiscmd=$0
    args=$@
    # stop the command
    $thiscmd --config "${HMS_CONF_DIR}" stop $command $args &
    wait_until_done $!
    # wait a user-specified sleep period
    sp=${HMS_RESTART_SLEEP:-3}
    if [ $sp -gt 0 ]; then
      sleep $sp
    fi
    # start the command
    $thiscmd --config "${HMS_CONF_DIR}" start $command $args &
    wait_until_done $!
    ;;

  (*)
    echo $usage
    exit 1
    ;;

esac
