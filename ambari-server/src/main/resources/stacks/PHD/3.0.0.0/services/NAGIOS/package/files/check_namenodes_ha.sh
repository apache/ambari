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

IFS=',' read -a namenodes <<< "$1"
port=$2
totalNN=${#namenodes[@]}
activeNN=()
standbyNN=()
unavailableNN=()

for nn in "${namenodes[@]}"
do
  export no_proxy=$nn
  status=$(curl -m 5 -s http://$nn:$port/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem | grep -i "tag.HAState" | grep -o -E "standby|active")
  if [ "$status" == "active" ]; then
    activeNN[${#activeNN[*]}]="$nn"
  elif [ "$status" == "standby" ]; then
    standbyNN[${#standbyNN[*]}]="$nn"
  elif [ "$status" == "" ]; then
    unavailableNN[${#unavailableNN[*]}]="$nn"
  fi
done

message=""
critical=false

if [ ${#activeNN[@]} -gt 1 ]; then
  critical=true
  message=$message" Only one NN can have HAState=active;"
elif [ ${#activeNN[@]} == 0 ]; then
  critical=true
  message=$message" No Active NN available;"
elif [ ${#standbyNN[@]} == 0 ]; then
  critical=true
  message=$message" No Standby NN available;"
fi

NNstats=" Active<"
for nn in "${activeNN[@]}"
do
  NNstats="$NNstats$nn;"
done
NNstats=${NNstats%\;}
NNstats=$NNstats">, Standby<"
for nn in "${standbyNN[@]}"
do
  NNstats="$NNstats$nn;"
done
NNstats=${NNstats%\;}
NNstats=$NNstats">, Unavailable<"
for nn in "${unavailableNN[@]}"
do
  NNstats="$NNstats$nn;"
done
NNstats=${NNstats%\;}
NNstats=$NNstats">"

if [ $critical == false ]; then
  echo "OK: NameNode HA healthy;"$NNstats
  exit 0
fi

echo "CRITICAL:"$message$NNstats
exit 2
