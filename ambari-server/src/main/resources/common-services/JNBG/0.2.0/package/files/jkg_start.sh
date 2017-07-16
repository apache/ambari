#!/usr/bin/env bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
set -x

START_CMD=$1
SPARK_HOME=$2
PY_EXEC=$3
PY_VENV_PATH_PREFIX=$4
KINIT_CMD=$5
LOG=$6
PIDFILE=$7

source ${PY_VENV_PATH_PREFIX}/python2.7/bin/activate

PLATFORM=`uname -p`
rhver=7

if [ "$PY_EXEC" = "/opt/rh/python27/root/usr/bin/python" ]; then
  rhscl=1
else
  rhscl=0
fi

if [ "$PLATFORM" == "x86_64" ]
then
  if [ -x /usr/bin/lsb_release ]; then
    rhver=$(/usr/bin/lsb_release -rs | cut -f1 -d.)
  fi

  if [ "$rhver" -eq 6 ];then
    if [ "$rhscl" -eq 1 ] && [ ! -f /opt/rh/python27/enable ]; then
      echo "Detected invalid installation state: Install Python 2.7 using Red Hat Software Collections and try reinstalling the service."
      exit 1
    elif [ "$rhscl" -eq 1 ]; then
      source /opt/rh/python27/enable
      # uninstall older pip version that accompanies SCL
      pip uninstall -y pip
    fi
  fi
fi

pyver=`echo $(${PY_EXEC} -V 2>&1 | awk '{ print $2 }') | sed -e 's/\.//g'`
if [ "$pyver" -lt 270 ]; then
  echo "Detected invalid installation state: Ensure that the specified python_interpreter_path is Python version 2.7."
  exit 1
fi

if [ ! -d "${PY_VENV_PATH_PREFIX}/python2.7" ]; then
  echo "Did not find necessary virtual environment to execute service startup. This state in unexpected and inconsistent when the service is in the INSTALLED state. Delete the service and reinstall."
  exit 1
fi
source ${PY_VENV_PATH_PREFIX}/python2.7/bin/activate

# Required for supporting Python 2 kernel
export PYTHONPATH=${SPARK_HOME}/python/lib/pyspark.zip:${SPARK_HOME}/python:${SPARK_HOME}/python/lib/py4j-0.10.4-src.zip

export SPARK_CONF_DIR=$SPARK_HOME/conf
source $SPARK_CONF_DIR/spark-env.sh
set +x
eval "$START_CMD >> $LOG 2>&1 &"
if [ $? -eq 0 ]; then
  echo $! > $PIDFILE
  exit 0
fi
exit 1
