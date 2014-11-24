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

service=$1
hosts=$2
port=$3

checkurl () {
  url=$1
  host=$2
  export no_proxy=$host
  curl $url -k -o /dev/null
  echo $?
}

if [[ -z "$service" || -z "$hosts" ]]; then
  echo "UNKNOWN: Invalid arguments; Usage: check_webui_ha.sh service_name, host_name";
  exit 3;
fi

case "$service" in
resourcemanager)
    url_end_part="/cluster"
    ;;
*) echo "UNKNOWN: Invalid service name [$service], valid options [resourcemanager]"
   exit 3
   ;;
esac

OIFS="$IFS"
IFS=','
read -a hosts_array <<< "${hosts}"
IFS="$OIFS"

for host in "${hosts_array[@]}"
do
  weburl="http://${host}:${port}${url_end_part}"
  if [[ `checkurl "$weburl" "$host"` -eq 0 ]]; then
    echo "OK: Successfully accessed $service Web UI"
    exit 0;
  fi
done

echo "WARNING: $service Web UI not accessible : $weburl";
exit 1;