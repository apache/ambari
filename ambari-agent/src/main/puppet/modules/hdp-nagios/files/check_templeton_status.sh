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
# out='{"status":"ok","version":"v1"}<status_code:200>'
HOST=$1
PORT=$2
VERSION=$3
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
regex="^.*\"status\":\"ok\".*<status_code:200>$"
out=`curl --negotiate -u : -s -w '<status_code:%{http_code}>' http://$HOST:$PORT/templeton/$VERSION/status 2>&1`
if [[ $out =~ $regex ]]; then 
  echo "OK: WebHCat Server status [$out]";
  exit 0;
fi
echo "CRITICAL: Error accessing WebHCat Server, status [$out]";
exit 2;
