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

export oozie_lib_dir=$1
export oozie_conf_dir=$2
export oozie_bin_dir=$3
export hadoop_conf_dir=$4
export hadoop_bin_dir=$5
export smoke_test_user=$6
export security_enabled=$7
export smoke_user_keytab=$8
export kinit_path_local=$9
export smokeuser_principal=$10

function getValueFromField {
  xmllint $1 | grep "<name>$2</name>" -C 2 | grep '<value>' | cut -d ">" -f2 | cut -d "<" -f1
  return $?
}

function checkOozieJobStatus {
  local job_id=$1
  local num_of_tries=$2
  #default num_of_tries to 10 if not present
  num_of_tries=${num_of_tries:-10}
  local i=0
  local rc=1
  local cmd="source ${oozie_conf_dir}/oozie-env.sh ; ${oozie_bin_dir}/oozie job -oozie ${OOZIE_SERVER} -info $job_id"
  sudo su ${smoke_test_user} -s /bin/bash - -c "$cmd"
  while [ $i -lt $num_of_tries ] ; do
    cmd_output=`sudo su ${smoke_test_user} -s /bin/bash - -c "$cmd"`
    (IFS='';echo $cmd_output)
    act_status=$(IFS='';echo $cmd_output | grep ^Status | cut -d':' -f2 | sed 's| ||g')
    echo "workflow_status=$act_status"
    if [ "RUNNING" == "$act_status" ]; then
      #increment the counter and get the status again after waiting for 15 secs
      sleep 15
      (( i++ ))
      elif [ "SUCCEEDED" == "$act_status" ]; then
        rc=0;
        break;
      else
        rc=1
        break;
      fi
    done
    return $rc
}

export OOZIE_EXIT_CODE=0
export JOBTRACKER=`getValueFromField ${hadoop_conf_dir}/yarn-site.xml yarn.resourcemanager.address`
export NAMENODE=`getValueFromField ${hadoop_conf_dir}/core-site.xml fs.defaultFS`
export OOZIE_SERVER=`getValueFromField ${oozie_conf_dir}/oozie-site.xml oozie.base.url | tr '[:upper:]' '[:lower:]'`

# search for the oozie examples JAR and, if found, store the directory name
export OOZIE_EXAMPLES_DIR=`find "${oozie_lib_dir}/" -name "oozie-examples.tar.gz" | xargs dirname`
if [[ -z "$OOZIE_EXAMPLES_DIR" ]] ; then
  export OOZIE_EXAMPLES_DIR='/usr/hdp/current/oozie-client/doc/'
else
  echo "Located Oozie examples JAR at $OOZIE_EXAMPLES_DIR"
fi

cd $OOZIE_EXAMPLES_DIR

sudo tar -zxf oozie-examples.tar.gz
sudo chmod -R o+rx examples

sudo sed -i "s|nameNode=hdfs://localhost:8020|nameNode=$NAMENODE|g"  examples/apps/map-reduce/job.properties
sudo sed -i "s|nameNode=hdfs://localhost:9000|nameNode=$NAMENODE|g"  examples/apps/map-reduce/job.properties
sudo sed -i "s|jobTracker=localhost:8021|jobTracker=$JOBTRACKER|g" examples/apps/map-reduce/job.properties
sudo sed -i "s|jobTracker=localhost:9001|jobTracker=$JOBTRACKER|g" examples/apps/map-reduce/job.properties
sudo sed -i "s|jobTracker=localhost:8032|jobTracker=$JOBTRACKER|g" examples/apps/map-reduce/job.properties
sudo sed -i "s|oozie.wf.application.path=hdfs://localhost:9000|oozie.wf.application.path=$NAMENODE|g" examples/apps/map-reduce/job.properties

if [[ $security_enabled == "True" ]]; then
  kinitcmd="${kinit_path_local} -kt ${smoke_user_keytab} ${smokeuser_principal}; "
else 
  kinitcmd=""
fi

sudo su ${smoke_test_user} -s /bin/bash - -c "${hadoop_bin_dir}/hdfs --config ${hadoop_conf_dir} dfs -rm -r examples"
sudo su ${smoke_test_user} -s /bin/bash - -c "${hadoop_bin_dir}/hdfs --config ${hadoop_conf_dir} dfs -rm -r input-data"
sudo su ${smoke_test_user} -s /bin/bash - -c "${hadoop_bin_dir}/hdfs --config ${hadoop_conf_dir} dfs -copyFromLocal $OOZIE_EXAMPLES_DIR/examples examples"
sudo su ${smoke_test_user} -s /bin/bash - -c "${hadoop_bin_dir}/hdfs --config ${hadoop_conf_dir} dfs -copyFromLocal $OOZIE_EXAMPLES_DIR/examples/input-data input-data"

cmd="${kinitcmd}source ${oozie_conf_dir}/oozie-env.sh ; ${oozie_bin_dir}/oozie -Doozie.auth.token.cache=false job -oozie $OOZIE_SERVER -config $OOZIE_EXAMPLES_DIR/examples/apps/map-reduce/job.properties  -run"
echo $cmd
job_info=`sudo su ${smoke_test_user} -s /bin/bash - -c "$cmd" | grep "job:"`
job_id="`echo $job_info | cut -d':' -f2`"
checkOozieJobStatus "$job_id" 15
OOZIE_EXIT_CODE="$?"
exit $OOZIE_EXIT_CODE
