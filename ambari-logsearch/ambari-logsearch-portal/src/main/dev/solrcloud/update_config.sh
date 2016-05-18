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

function usage {
    echo "Usage: $0 <Solr Install Folder> [zk_hosts]"
}

if [ $# -lt 1 ]; then
    usage
    exit 1
fi

curr_dir=`pwd`
cd `dirname $0`; script_dir=`pwd`; cd $curr_dir

SOLR_INSTALL=$1
if [ -x $SOLR_INSTALL/server/scripts/cloud-scripts/zkcli.sh ]; then
    ZK_CLI=$SOLR_INSTALL/server/scripts/cloud-scripts/zkcli.sh
else
    echo "ERROR: Invalid Solr install folder $SOLR_INSTALL"
    usage
    exit 1
fi

zk_hosts="localhost:9983"
if [ $# -eq 2 ]; then
    zk_hosts=$2
fi


CONFIGSET_FOLDER=$script_dir/../../configsets

set -x
$ZK_CLI -zkhost $zk_hosts -cmd upconfig -confdir $CONFIGSET_FOLDER/audit_logs/conf -confname audit_logs
$ZK_CLI -zkhost $zk_hosts -cmd upconfig -confdir $CONFIGSET_FOLDER/hadoop_logs/conf -confname hadoop_logs
$ZK_CLI -zkhost $zk_hosts -cmd upconfig -confdir $CONFIGSET_FOLDER/history/conf -confname history
