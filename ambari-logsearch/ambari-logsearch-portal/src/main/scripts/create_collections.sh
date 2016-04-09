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

if [ $# -ne 4 ]; then
    echo "Usage: $0 <solr_home> <number of shards> <number of replications> [configset folder]"
    exit 1
fi

curr_dir=`pwd`
cd `dirname $0`; script_dir=`pwd`; cd $curr_dir


solr_home=$1
shards=$2
replications=$3

configsets_folder=$4
if [ "$configsets_folder" = "" ]; then
    configsets_folder=${script_dir}/solr_configsets
fi

${solr_home}/bin/solr create -c hadoop_logs -d ${configsets_folder}/hadoop_logs/conf -s ${shards} -rf ${replications}
${solr_home}/bin/solr create -c history -d ${configsets_folder}/history/conf -s 1 -rf ${shards}
${solr_home}/bin/solr create -c audit_logs -d ${configsets_folder}/audit_logs/conf -s ${shards} -rf ${replications}
