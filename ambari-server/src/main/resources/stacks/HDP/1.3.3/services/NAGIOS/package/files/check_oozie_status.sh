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
# OOZIE_URL is of the form http://<hostname>:<port>/oozie
HOST=`echo $1 | tr '[:upper:]' '[:lower:]'`
PORT=$2
JAVA_HOME=$3
SEC_ENABLED=$4
if [[ "$SEC_ENABLED" == "true" ]]; then
  NAGIOS_KEYTAB=$5
  NAGIOS_USER=$6
  KINIT_PATH=$7
  out1=`${KINIT_PATH} -kt ${NAGIOS_KEYTAB} ${NAGIOS_USER} 2>&1`
  if [[ "$?" -ne 0 ]]; then
    echo "CRITICAL: Error doing kinit for nagios [$out1]";
    exit 2;
  fi
fi
OOZIE_URL="http://$HOST:$PORT/oozie"
export JAVA_HOME=$JAVA_HOME
out=`oozie admin -oozie ${OOZIE_URL} -status 2>&1`
if [[ "$?" -ne 0 ]]; then 
  echo "CRITICAL: Error accessing Oozie Server status [$out]";
  exit 2;
fi
echo "OK: Oozie Server status [$out]";
exit 0;
