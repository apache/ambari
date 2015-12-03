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

zk_cli_shell=$1
smoke_user=$2
conf_dir=$3
client_port=$4
security_enabled=$5
kinit_path_local=$6
smoke_user_keytab=$7
smokeuser_principal=$8
test_output_file=$9
export ZOOKEEPER_EXIT_CODE=0
errors_expr="ERROR|Exception"
acceptable_expr="SecurityException"
zkhosts=` grep "^\s*server\.[[:digit:]]"  $conf_dir/zoo.cfg  | cut -f 2 -d '=' | cut -f 1 -d ':' | tr '\n' ' ' `
zk_node1=`echo $zkhosts | tr ' ' '\n' | head -n 1`  
echo "zk_node1=$zk_node1"
if [[ $security_enabled == "True" ]]; then
  kinitcmd="$kinit_path_local -kt $smoke_user_keytab $smokeuser_principal"
  /var/lib/ambari-agent/ambari-sudo.sh su $smoke_user -s /bin/bash - -c "$kinitcmd"
fi

function verify_output() {
  if [ -f $test_output_file ]; then
    errors=`grep -E $errors_expr $test_output_file | grep -v $acceptable_expr`
    if [ "$?" -eq 0 ]; then
      echo "Error found in the zookeeper smoke test. Exiting."
      echo $errors
      exit 1
    fi
  fi
}

# Delete /zk_smoketest znode if exists
/var/lib/ambari-agent/ambari-sudo.sh su $smoke_user -s /bin/bash - -c "source $conf_dir/zookeeper-env.sh ;  echo delete /zk_smoketest | ${zk_cli_shell} -server $zk_node1:$client_port" 2>&1>$test_output_file
# Create /zk_smoketest znode on one zookeeper server
/var/lib/ambari-agent/ambari-sudo.sh su $smoke_user -s /bin/bash - -c "source $conf_dir/zookeeper-env.sh ; echo create /zk_smoketest smoke_data | ${zk_cli_shell} -server $zk_node1:$client_port" 2>&1>>$test_output_file
verify_output

for i in $zkhosts ; do
  echo "Running test on host $i"
  # Verify the data associated with znode across all the nodes in the zookeeper quorum
  /var/lib/ambari-agent/ambari-sudo.sh su $smoke_user -s /bin/bash - -c "source $conf_dir/zookeeper-env.sh ; echo 'get /zk_smoketest' | ${zk_cli_shell} -server $i:$client_port"
  /var/lib/ambari-agent/ambari-sudo.sh su $smoke_user -s /bin/bash - -c "source $conf_dir/zookeeper-env.sh ; echo 'ls /' | ${zk_cli_shell} -server $i:$client_port"
  output=$(/var/lib/ambari-agent/ambari-sudo.sh su $smoke_user -s /bin/bash - -c "source $conf_dir/zookeeper-env.sh ; echo 'get /zk_smoketest' | ${zk_cli_shell} -server $i:$client_port")
  echo $output | grep smoke_data
  if [[ $? -ne 0 ]] ; then
    echo "Data associated with znode /zk_smoketests is not consistent on host $i"
    ((ZOOKEEPER_EXIT_CODE=$ZOOKEEPER_EXIT_CODE+1))
  fi
done

/var/lib/ambari-agent/ambari-sudo.sh su $smoke_user -s /bin/bash - -c "source $conf_dir/zookeeper-env.sh ; echo 'delete /zk_smoketest' | ${zk_cli_shell} -server $zk_node1:$client_port"
if [[ "$ZOOKEEPER_EXIT_CODE" -ne "0" ]] ; then
  echo "Zookeeper Smoke Test: Failed" 
else
   echo "Zookeeper Smoke Test: Passed" 
fi
exit $ZOOKEEPER_EXIT_CODE
