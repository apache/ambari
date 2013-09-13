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
class hdp-templeton::service(
  $ensure,
  $initial_wait = undef
)
{
  include $hdp-templeton::params
  
  $user = "$hdp-templeton::params::webhcat_user"
  $hadoop_home = $hdp-templeton::params::hadoop_prefix
  $cmd = "env HADOOP_HOME=${hadoop_home} /usr/lib/hcatalog/sbin/webhcat_server.sh"
  $pid_file = "${hdp-templeton::params::templeton_pid_dir}/webhcat.pid" 

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} start'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  '${cmd} stop' && rm -f ${pid_file}"
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }

  hdp-templeton::service::directory { $hdp-templeton::params::templeton_pid_dir : }
  hdp-templeton::service::directory { $hdp-templeton::params::hcat_log_dir : }

  anchor{'hdp-templeton::service::begin':} -> Hdp-templeton::Service::Directory<||> -> anchor{'hdp-templeton::service::end':}
  
  if ($daemon_cmd != undef) {
    hdp::exec { $daemon_cmd:
      command => $daemon_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
    }
    Hdp-templeton::Service::Directory<||> -> Hdp::Exec[$daemon_cmd] -> Anchor['hdp-templeton::service::end']
  }
}

define hdp-templeton::service::directory()
{
  hdp::directory_recursive_create { $name: 
    owner => $hdp-templeton::params::webhcat_user,
    mode => '0755',
    service_state => $hdp-templeton::service::ensure,
    force => true
  }
}

