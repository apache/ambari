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

export oozie_conf_dir=$1
export oozie_examples_dir=$2
export hadoop_conf_dir=$3

function getValueFromField {
  xmllint $1 | grep "<name>$2</name>" -C 2 | grep '<value>' | cut -d ">" -f2 | cut -d "<" -f1
  return $?
}

export JOBTRACKER=`getValueFromField ${hadoop_conf_dir}/yarn-site.xml yarn.resourcemanager.address`
export NAMENODE=`getValueFromField ${hadoop_conf_dir}/core-site.xml fs.defaultFS`

cd $oozie_examples_dir

/var/lib/ambari-agent/ambari-sudo.sh tar -zxf oozie-examples.tar.gz
/var/lib/ambari-agent/ambari-sudo.sh chmod -R o+rx examples

/var/lib/ambari-agent/ambari-sudo.sh sed -i "s|nameNode=hdfs://localhost:8020|nameNode=$NAMENODE|g"  examples/apps/map-reduce/job.properties
/var/lib/ambari-agent/ambari-sudo.sh sed -i "s|nameNode=hdfs://localhost:9000|nameNode=$NAMENODE|g"  examples/apps/map-reduce/job.properties
/var/lib/ambari-agent/ambari-sudo.sh sed -i "s|jobTracker=localhost:8021|jobTracker=$JOBTRACKER|g" examples/apps/map-reduce/job.properties
/var/lib/ambari-agent/ambari-sudo.sh sed -i "s|jobTracker=localhost:9001|jobTracker=$JOBTRACKER|g" examples/apps/map-reduce/job.properties
/var/lib/ambari-agent/ambari-sudo.sh sed -i "s|jobTracker=localhost:8032|jobTracker=$JOBTRACKER|g" examples/apps/map-reduce/job.properties
/var/lib/ambari-agent/ambari-sudo.sh sed -i "s|oozie.wf.application.path=hdfs://localhost:9000|oozie.wf.application.path=$NAMENODE|g" examples/apps/map-reduce/job.properties
