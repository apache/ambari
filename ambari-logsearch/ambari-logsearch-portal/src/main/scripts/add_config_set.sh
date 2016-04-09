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
    echo "Usage: $0 <solr_home> <zk_host_with_path> <config_name> <config_folder>"
    echo "Example: $0 /opt/solr MY_ZKHOST/solr hadoop_logs `dirname $0`/configsets/hadoop_logs"
    exit 1
fi

curr_dir=`pwd`
cd `dirname $0`; script_dir=`pwd`; cd $curr_dir


solr_home=$1
zk_host=$2
config_name=$3
config_folder=$4

tmp_folder=/tmp/solr_config_${config_name}_$USER
rm -rf $tmp_folder

$solr_home/server/scripts/cloud-scripts/zkcli.sh -zkhost $zk_host -cmd downconfig -confdir $tmp_folder -confname $config_name > /dev/null 2>&1 

if [ -d $tmp_folder ]; then
    echo "Config $config_name already existing. Will not add to zookeeper"
else
    echo "Adding config to $config_name to $zk_host"
    $solr_home/server/scripts/cloud-scripts/zkcli.sh  -zkhost $zk_host -cmd upconfig -confdir $config_folder -confname $config_name
    echo "Added config to $config_name to $zk_host"
fi
