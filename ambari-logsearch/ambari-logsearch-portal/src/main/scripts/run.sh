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

if [ ! -z "$LOGSEARCH_INCLUDE" ]; then
   source $LOGSEARCH_INCLUDE
fi

if [ ! -z "$LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE" ]; then
   source $LOGSEARCH_SOLR_CLIENT_SSL_INCLUDE
fi

JAVA=java
if [ -x $JAVA_HOME/bin/java ]; then
    JAVA=$JAVA_HOME/bin/java
fi

if [ "$LOGSEARCH_JAVA_MEM" = "" ]; then
    LOGSEARCH_JAVA_MEM="-Xmx1g"
fi

if [ "$LOGFILE" = "" ]; then
    LOGFILE="/var/log/logsearch/logsearch.out"
    touch $LOGFILE 2> /dev/null
    if [ $? -ne 0 ]; then
	LOGFILE=/tmp/${USER}_logsearch.out
    fi
fi


#Temporarily enabling JMX so we can monitor the memory and CPU utilization of the process
#JMX="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=3098"

LOGSEARCH_DEBUG_SUSPEND=${LOGSEARCH_DEBUG_SUSPEND:-n}
if [ "$LOGSEARCH_DEBUG" = "true" ] && [ ! -z "$LOGSEARCH_DEBUG_PORT" ]; then
  LOGSEARCH_JAVA_OPTS="$LOGSEARCH_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=$LOGSEARCH_DEBUG_PORT,server=y,suspend=$LOGSEARCH_DEBUG_SUSPEND "
fi

if [ "$LOGSEARCH_SSL" = "true" ]; then
  LOGSEARCH_JAVA_OPTS="$LOGSEARCH_JAVA_OPTS -Djavax.net.ssl.keyStore=$LOGSEARCH_KEYSTORE_LOCATION -Djavax.net.ssl.keyStoreType=$LOGSEARCH_KEYSTORE_TYPE -Djavax.net.ssl.trustStore=$LOGSEARCH_TRUSTSTORE_LOCATION -Djavax.net.ssl.trustStoreType=$LOGSEARCH_TRUSTSTORE_TYPE"
fi

if [ "$PID_FILE" = "" ]; then
    LOGSEARCH_PID_DIR=$HOME
    PID_FILE=$LOGSEARCH_PID_DIR/logsearch-search-$USER.pid
fi

if [ -f ${PID_FILE} ]; then
    PID=`cat ${PID_FILE}`
    if kill -0 $PID 2>/dev/null; then
	echo "logsearch already running (${PID}) killing..."
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

if [ -z "$LOGSEARCH_CONF_DIR" ]; then
  LOGSEARCH_CONF_DIR="/etc/logsearch/conf"
  if [ ! -d $LOGSEARCH_CONF_DIR ]; then
      if [ -d $script_dir/classes ]; then
	  LOGSEARCH_CONF_DIR=$script_dir/classes
      fi
  fi
  echo "LOGSEARCH_CONF_DIR not found. Use default: $LOGSEARCH_CONF_DIR"
fi

LOGSEARCH_GC_LOGFILE=`dirname $LOGFILE`/logsearch_gc.log
LOGSEARCH_GC_OPTS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$LOGSEARCH_GC_LOGFILE"

echo "Starting logsearch. Output file=$LOGFILE pid_file=$PID_FILE"
#LOGSEARCH_CLI_CLASSPATH=
#set -x
nohup $JAVA -cp "$LOGSEARCH_CLI_CLASSPATH:$LOGSEARCH_CONF_DIR:$script_dir/libs/*:$script_dir/classes" $LOGSEARCH_GC_OPTS $LOGSEARCH_JAVA_MEM $LOGSEARCH_JAVA_OPTS $JMX org.apache.ambari.logsearch.LogSearch $LOGSEARCH_PORT $* > $LOGFILE 2>&1 &
echo $! > $PID_FILE
