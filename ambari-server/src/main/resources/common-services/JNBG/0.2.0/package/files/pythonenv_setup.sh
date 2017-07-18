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

PY_EXEC=$1
PY_VENV_PATH_PREFIX=$2
PY_VENV_OWNER=$3

checkPipInstall()
{
  pip show $1 2>&1 > /dev/null
}

checkSuccess()
{
  if [ $? != 0 ]
  then
    set +x
    echo "Error encountered at line $1 while attempting to: "
    if [ -n "$2" ]
    then
      echo $2
    fi
    echo Exiting.
    exit 1
  fi
  set -x
}

if [ -d "${PY_VENV_PATH_PREFIX}/python2.7" ]; then
  echo "Python client installation detected. Nothing to do."
  exit 0
fi

# /etc/pip.conf overrides all
if [ -f /etc/pip.conf ]; then
  PYPI_URL=$(cat  /etc/pip.conf | grep -i extra-index-url | awk '{print $3}')
  PYPI_HOST=$(cat  /etc/pip.conf | grep -i extra-index-url | awk '{print $3}' | sed -e 's/^.*\/\///' | sed -e 's/:.*$//')
  PYPI_PORT=$(cat  /etc/pip.conf | grep -i extra-index-url | awk '{print $3}'  | sed -e 's/^.*:*://' | sed -e 's/\/.*$//')
else
  # If no pip.conf then try to determine based on repo URLs in use
  if [ -f /etc/yum.repos.d/IOP.repo ]; then
    cat /etc/yum.repos.d/IOP.repo | grep baseurl |  grep -w http
    httpurl=$?
    cat /etc/yum.repos.d/IOP.repo | grep baseurl |  grep -w https
    httpsurl=$?
    if [ "$httpurl" -eq 0 ]; then
      PYPI_HOST=$(cat /etc/yum.repos.d/IOP.repo | grep baseurl | sed -e 's/baseurl=http:\/\///' | cut -f1 -d"/")
      PYPI_PORT=8080
      PYPI_URL=http://${PYPI_HOST}:${PYPI_PORT}/simple/
    elif [ "$httpsurl" -eq 0 ]; then
      PYPI_HOST=$(cat /etc/yum.repos.d/IOP.repo | grep baseurl | sed -e 's/baseurl=https:\/\///' | cut -f1 -d"/")
      PYPI_PORT=8080
      PYPI_URL=http://${PYPI_HOST}:${PYPI_PORT}/simple/
    fi
  else
    # fallback default
    PYPI_HOST=ibm-open-platform.ibm.com
    PYPI_PORT=8080
    PYPI_URL=http://ibm-open-platform.ibm.com:8080/simple/
  fi
fi

if [[ -z "${PYPI_URL}" || -z "${PYPI_HOST}" || -z "${PYPI_PORT}" ]];then
  PYPI_HOST=ibm-open-platform.ibm.com
  PYPI_PORT=8080
  PYPI_URL=http://ibm-open-platform.ibm.com:8080/simple/
fi

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
      echo "Installation failed; Install Python 2.7 using Red Hat Software Collections and retry."
      exit 1
    elif [ "$rhscl" -eq 1 ]; then
      #Install Python 2.7 using Red Hat Software Collections and retry."
      source /opt/rh/python27/enable
      # uninstall older pip version that accompanies SCL
      pip uninstall -y pip
    fi
  fi
fi

pyver=`echo $(${PY_EXEC} -V 2>&1 | awk '{ print $2 }') | sed -e 's/\.//g'`
if [ "$pyver" -lt 270 ]; then
  echo "Installation failed; Ensure that the specified python_interpreter_path is Python version 2.7."
  exit 1
fi

easy_install pip
checkSuccess $LINENO "-  easy_install pip"
pip -V

pip install --trusted-host ${PYPI_HOST} --no-cache-dir --index-url http://${PYPI_HOST}:${PYPI_PORT}/simple virtualenv --upgrade
checkPipInstall virtualenv
checkSuccess $LINENO "-  pip install virtualenv"

virtualenv -p ${PY_EXEC} ${PY_VENV_PATH_PREFIX}/python2.7
checkSuccess $LINENO "-  create virtualenv using ${PY_EXEC}"

# Set ownership of the created virtualenv if configured via python_virtualenv_restrictive
if [ "${PY_VENV_OWNER}" != "root" ]; then
  echo ====== Virtualenv owner = $PY_VENV_OWNER =========
  chown -R ${PY_VENV_OWNER}: ${PY_VENV_PATH_PREFIX}/python2.7
fi
