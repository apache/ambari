#!/bin/bash
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
MAPRED_LOCAL_DIRS=$1
CRITICAL=`echo $2 | cut -d % -f 1`
IFS=","
for mapred_dir in $MAPRED_LOCAL_DIRS
do
  percent=`df -hl $mapred_dir | awk '{percent=$5;} END{print percent}' | cut -d % -f 1`
  if [ $percent -ge $CRITICAL ]; then
    echo "CRITICAL: MapReduce local dir is full."
    exit 2
  fi
done
echo "OK: MapReduce local dir space is available."
exit 0
