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

foreground=0
if [ "$1" = "-foreground" ]; then
  foreground=1
  shift
fi

if [ ! -z "$LOGFEEDER_INCLUDE" ]; then
  source $LOGFEEDER_INCLUDE
fi

if [ ! -z "$LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE" ]; then
  source $LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE
fi

JAVA=java
if [ -x $JAVA_HOME/bin/java ]; then
  JAVA=$JAVA_HOME/bin/java
fi

if [ "$LOGFEEDER_JAVA_MEM" = "" ]; then
  LOGFEEDER_JAVA_MEM="-Xmx512m"
fi

if [ "$LOGFILE" = "" ]; then
  LOGFILE="/var/log/logfeeder/logfeeder.out"
fi

if [ "$PID_FILE" = "" ]; then
  LOGFEEDER_PID_DIR=$HOME
  PID_FILE=$LOGFEEDER_PID_DIR/logsearch-logfeeder-$USER.pid
fi

if [ "$LOGFEEDER_CONF_DIR" = "" ]; then
  LOGFEEDER_CONF_DIR="/etc/logfeeder/conf"
  if [ ! -d $LOGFEEDER_CONF_DIR ]; then
    if [ -d $script_dir/classes ]; then
      LOGFEEDER_CONF_DIR=$script_dir/classes
    fi
  fi
fi

LOGFEEDER_DEBUG_SUSPEND=${LOGFEEDER_DEBUG_SUSPEND:-n}
if [ "$LOGFEEDER_DEBUG" = "true" ] && [ ! -z "$LOGFEEDER_DEBUG_PORT" ]; then
  LOGFEEDER_JAVA_OPTS="$LOGFEEDER_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$LOGFEEDER_DEBUG_PORT,server=y,suspend=$LOGFEEDER_DEBUG_SUSPEND "
fi

LOGFEEDER_GC_LOGFILE=`dirname $LOGFILE`/logfeeder_gc.log
LOGFEEDER_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$LOGFEEDER_GC_LOGFILE"

#JMX="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=2098"

if [ "$LOGFEEDER_SSL" = "true" ]; then
  LOGFEEDER_JAVA_OPTS="$LOGFEEDER_JAVA_OPTS -Djavax.net.ssl.keyStore=$LOGFEEDER_KEYSTORE_LOCATION -Djavax.net.ssl.keyStoreType=$LOGFEEDER_KEYSTORE_TYPE -Djavax.net.ssl.trustStore=$LOGFEEDER_TRUSTSTORE_LOCATION -Djavax.net.ssl.trustStoreType=$LOGFEEDER_TRUSTSTORE_TYPE"
fi

if [ $foreground -eq 0 ]; then
  if [ -f ${PID_FILE} ]; then
  PID=`cat ${PID_FILE}`
    if kill -0 $PID 2>/dev/null; then
      echo "logfeeder already running (${PID}) killing..."
      kill $PID 2>/dev/null
      sleep 5
      if kill -0 $PID 2>/dev/null; then
        echo "logfeeder still running. Will kill process forcefully in another 10 seconds..."
        sleep 10
        kill -9 $PID 2>/dev/null
        sleep 2
      fi
    fi

    if kill -0 $PID 2>/dev/null; then
      echo "ERROR: Even after all efforts to stop logfeeder, it is still running. pid=$PID. Please manually kill the service and try again."
      exit 1
    fi
  fi

  echo "Starting logfeeder. Output file=$LOGFILE pid_file=$PID_FILE"
  #LOGFEEDER_CLI_CLASSPATH=set -x
  nohup $JAVA -cp "$LOGFEEDER_CLI_CLASSPATH:$LOGFEEDER_CONF_DIR:$script_dir/libs/*:$script_dir/classes" $LOGFEEDER_GC_OPTS $LOGFEEDER_JAVA_MEM $LOGFEEDER_JAVA_OPTS $JMX org.apache.ambari.logfeeder.LogFeeder $* > $LOGFILE 2>&1 &
  echo $! > $PID_FILE
else
  $JAVA -cp "$LOGFEEDER_CLI_CLASSPATH:$LOGFEEDER_CONF_DIR:$script_dir/libs/*:$script_dir/classes" $LOGFEEDER_JAVA_MEM $LOGFEEDER_JAVA_OPTS $JMX org.apache.ambari.logfeeder.LogFeeder $*
fi

