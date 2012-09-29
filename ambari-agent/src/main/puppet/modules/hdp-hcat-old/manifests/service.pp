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
class hdp-hcat::service(
  $ensure,
  $initial_wait = undef
)
{
  include $hdp-hcat::params
  
  $user = $hdp::params::hcat_user
  $hadoop_home = $hdp::hadoop_home
  $cmd = "env HADOOP_HOME=${hadoop_home} /usr/sbin/hcat_server.sh"
  $pid_file = "${hdp-hcat::params::hcat_pid_dir}/hcat.pid" 

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} start'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  '${cmd} stop'"
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }

  hdp-hcat::service::directory { $hdp-hcat::params::hcat_pid_dir : }
  hdp-hcat::service::directory { $hdp-hcat::params::hcat_log_dir : }

  anchor{'hdp-hcat::service::begin':} -> Hdp-hcat::Service::Directory<||> -> anchor{'hdp-hcat::service::end':}
  
  if ($daemon_cmd != undef) {
    hdp::exec { $daemon_cmd:
      command => $daemon_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
    }
    Hdp-hcat::Service::Directory<||> -> Hdp::Exec[$daemon_cmd] -> Anchor['hdp-hcat::service::end']
  }
}

define hdp-hcat::service::directory()
{
  hdp::directory_recursive_create { $name: 
    owner => $hdp::params::hcat_user,
    mode => '0755'
  }
}

