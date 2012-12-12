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
define hdp-hadoop::service(
  $ensure = 'running',
  $user,
  $initial_wait = undef,
  $create_pid_dir = true,
  $create_log_dir = true
)
{

  $security_enabled = $hdp::params::security_enabled

  #NOTE does not work if namenode and datanode are on same host 
  $pid_dir = "${hdp-hadoop::params::hadoop_piddirprefix}/${user}"
  
  if (($security_enabled == true) and ($name == 'datanode')) {
    $run_as_root = true
  } else {       
    $run_as_root = false
  }

  if (($security_enabled == true) and ($name == 'datanode')) {
    $hdfs_user = $hdp::params::hdfs_user
    $pid_file = "${hdp-hadoop::params::hadoop_piddirprefix}/${hdfs_user}/hadoop-${hdfs_user}-${name}.pid"
  } else {
    $pid_file = "${pid_dir}/hadoop-${user}-${name}.pid"
  } 

  $log_dir = "${hdp-hadoop::params::hadoop_logdirprefix}/${user}"
  $hadoop_daemon = "${hdp::params::hadoop_bin}/hadoop-daemon.sh"
   
  $cmd = "${hadoop_daemon} --config ${hdp-hadoop::params::conf_dir}"
  if ($ensure == 'running') {
    if ($run_as_root == true) {
      $daemon_cmd = "${cmd} start ${name}"
    } else {
      $daemon_cmd = "su - ${user} -c  '${cmd} start ${name}'"
    }
    $service_is_up = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    if ($run_as_root == true) {
      $daemon_cmd = "${cmd} stop ${name}"
    } else {
      $daemon_cmd = "su - ${user} -c  '${cmd} stop ${name}'"
    }
    $service_is_up = undef
  } else {
    $daemon_cmd = undef
  }
 
  if ($create_pid_dir == true) {
    hdp::directory_recursive_create { $pid_dir: 
      owner       => $user,
      context_tag => 'hadoop_service',
      service_state => $service_state,
      force => true
    }
  }
  
  if ($create_log_dir == true) {
    hdp::directory_recursive_create { $log_dir: 
      owner       => $user,
      context_tag => 'hadoop_service',
      service_state => $service_state,
      force => true
    }
  }
  if ($daemon_cmd != undef) {  
    hdp::exec { $daemon_cmd:
      command      => $daemon_cmd,
      unless       => $service_is_up,
      initial_wait => $initial_wait
    }
  }

  anchor{"hdp-hadoop::service::${name}::begin":}
  anchor{"hdp-hadoop::service::${name}::end":}
  if ($daemon_cmd != undef) {
    Anchor["hdp-hadoop::service::${name}::begin"] -> Hdp::Exec[$daemon_cmd] -> Anchor["hdp-hadoop::service::${name}::end"]

    if ($create_pid_dir == true) {
      Anchor["hdp-hadoop::service::${name}::begin"] -> Hdp::Directory_recursive_create[$pid_dir] -> Hdp::Exec[$daemon_cmd] 
    }
     if ($create_log_dir == true) {
      Anchor["hdp-hadoop::service::${name}::begin"] -> Hdp::Directory_recursive_create[$log_dir] -> Hdp::Exec[$daemon_cmd] 
    }
  }
  if ($ensure == 'running') {
    #TODO: look at Puppet resource retry and retry_sleep
    #TODO: can make sleep contingent on $name
    $sleep = 5
    $post_check = "sleep ${sleep}; ${service_is_up}"
    hdp::exec { $post_check:
      command => $post_check,
      unless  => $service_is_up
    }
    Hdp::Exec[$daemon_cmd] -> Hdp::Exec[$post_check] -> Anchor["hdp-hadoop::service::${name}::end"]
  }  
}
