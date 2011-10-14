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
# See the License for the specific language governing permissions and
# limitations under the License.

# included in all the hadoop scripts with source command
# should not be executable directly
# also should not be passed any arguments, since we need original $*

# resolve links - $0 may be a softlink

this="$0"
while [ -h "$this" ]; do
  ls=`ls -ld "$this"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    this="$link"
  else
    this=`dirname "$this"`/"$link"
  fi
done

# convert relative path to absolute path
bin=`dirname "$this"`
script=`basename "$this"`
bin=`cd "$bin"; pwd`
this="$bin/$script"

#check to see if the conf dir or ambari home are given as an optional arguments
if [ $# -gt 1 ]
then
  if [ "--config" = "$1" ]
  then
    shift
    confdir=$1
    shift
    AMBARI_CONF_DIR=$confdir
  fi
fi

# the root of the ambari installation
export AMBARI_HOME=`dirname "$this"`/..

if [ -z ${AMBARI_LOG_DIR} ]; then
    export AMBARI_LOG_DIR="${AMBARI_HOME}/var/log"
fi

if [ -z ${AMBARI_PID_DIR} ]; then
    export AMBARI_PID_DIR="${AMBARI_HOME}/var/run"
fi

AMBARI_VERSION=`cat ${AMBARI_HOME}/share/ambari/VERSION`

# Allow alternate conf dir location.
if [ -z "${AMBARI_CONF_DIR}" ]; then
    if [ -e "${AMBARI_HOME}/conf" ]; then
      AMBARI_CONF_DIR="$AMBARI_HOME/conf"
    fi
    if [ -e "${AMBARI_HOME}/etc/ambari" ]; then
      AMBARI_CONF_DIR="$AMBARI_HOME/etc/ambari"
    fi
fi

if [ -f "${AMBARI_CONF_DIR}/ambari-env.sh" ]; then
  . "${AMBARI_CONF_DIR}/ambari-env.sh"
fi

COMMON="${AMBARI_HOME}/share/ambari/*:${AMBARI_HOME}/share/ambari/lib/*"

export AMBARI_CORE=${AMBARI_HOME}/ambari-core-${AMBARI_VERSION}.jar
export AMBARI_AGENT=${AMBARI_HOME}/ambari-agent-${AMBARI_VERSION}.jar
export CURRENT_DATE=`date +%Y%m%d%H%M`

if [ -z "$JAVA_HOME" ] ; then
  echo ERROR! You forgot to set JAVA_HOME in conf/ambari-env.sh
fi

export JPS="ps ax"

