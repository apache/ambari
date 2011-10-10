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

#check to see if the conf dir or hms home are given as an optional arguments
if [ $# -gt 1 ]
then
  if [ "--config" = "$1" ]
  then
    shift
    confdir=$1
    shift
    HMS_CONF_DIR=$confdir
  fi
fi

# the root of the hms installation
export HMS_HOME=`dirname "$this"`/..

if [ -z ${HMS_LOG_DIR} ]; then
    export HMS_LOG_DIR="${HMS_HOME}/logs"
fi

if [ -z ${HMS_PID_DIR} ]; then
    export HMS_PID_DIR="${HMS_HOME}/var/run"
fi

HMS_VERSION=`cat ${HMS_HOME}/VERSION`
HMS_IDENT_STRING=`whoami`

# Allow alternate conf dir location.
if [ -z "${HMS_CONF_DIR}" ]; then
    HMS_CONF_DIR="${HMS_CONF_DIR:-$HMS_HOME/conf}"
    export HMS_CONF_DIR=${HMS_HOME}/conf
fi

if [ -f "${HMS_CONF_DIR}/hms-env.sh" ]; then
  . "${HMS_CONF_DIR}/hms-env.sh"
fi

COMMON="${HMS_HOME}/lib/*"
#export COMMON=`echo ${COMMON} | sed 'y/ /:/'`

export HMS_CORE=${HMS_HOME}/hms-core-${HMS_VERSION}.jar
export HMS_AGENT=${HMS_HOME}/hms-agent-${HMS_VERSION}.jar
export CURRENT_DATE=`date +%Y%m%d%H%M`

if [ -z "$JAVA_HOME" ] ; then
  echo ERROR! You forgot to set JAVA_HOME in conf/hms-env.sh
fi

export JPS="ps ax"

