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
define hdp2-hadoop::service(
  $ensure = 'running',
  $user,
  $initial_wait = undef,
  $create_pid_dir = true,
  $create_log_dir = true
)
{

  #NOTE does not work if namenode and datanode are on same host 
  $pid_dir = "${hdp2-hadoop::params::hadoop_piddirprefix}/${user}"
  $log_dir = "${hdp2-hadoop::params::hadoop_logdirprefix}/${user}"
  if (($name == 'resourcemanager') or ($name == 'nodemanager')) {
    $hadoop_daemon = "${hdp2::params::yarn_sbin}/yarn-daemon.sh"
    $cmd = "HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec ${hadoop_daemon} --config ${hdp2-hadoop::params::conf_dir}"
    $pid_file = "${pid_dir}/yarn-${user}-${name}.pid"
  } elsif ($name == 'historyserver') {
    $hadoop_daemon = "${hdp2::params::mapred_sbin}/mr-jobhistory-daemon.sh"
    # Temporary fix to not pass --config till Hadoop fixes it upstream.
    $cmd = "HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec HADOOP_CONF_DIR=${hdp2-hadoop::params::conf_dir} ${hadoop_daemon}"
    $pid_file = "${pid_dir}/mapred-${user}-${name}.pid"
  } else {
    $hadoop_daemon = "${hdp2::params::hadoop_sbin}/hadoop-daemon.sh"
    $cmd = "${hadoop_daemon} --config ${hdp2-hadoop::params::conf_dir}"
    $pid_file = "${pid_dir}/hadoop-${user}-${name}.pid"
  }
   
  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} start ${name}'"
    $service_is_up = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  '${cmd} stop ${name}'"
    $service_is_up = undef
  } else {
    $daemon_cmd = undef
  }
 
  if ($create_pid_dir == true) {
    hdp2::directory_recursive_create { $pid_dir: 
      owner       => $user,
      context_tag => 'hadoop_service',
      service_state => $service_state,
      force => true
    }
  }
  
  if ($create_log_dir == true) {
    hdp2::directory_recursive_create { $log_dir: 
      owner       => $user,
      context_tag => 'hadoop_service',
      service_state => $service_state,
      force => true
    }
  }
  if ($daemon_cmd != undef) {  
    hdp2::exec { $daemon_cmd:
      command      => $daemon_cmd,
      unless       => $service_is_up,
      initial_wait => $initial_wait
    }
  }

  anchor{"hdp2-hadoop::service::${name}::begin":}
  anchor{"hdp2-hadoop::service::${name}::end":}
  if ($daemon_cmd != undef) {
    Anchor["hdp2-hadoop::service::${name}::begin"] -> Hdp2::Exec[$daemon_cmd] -> Anchor["hdp2-hadoop::service::${name}::end"]

    if ($create_pid_dir == true) {
      Anchor["hdp2-hadoop::service::${name}::begin"] -> Hdp2::Directory_recursive_create[$pid_dir] -> Hdp2::Exec[$daemon_cmd] 
    }
     if ($create_log_dir == true) {
      Anchor["hdp2-hadoop::service::${name}::begin"] -> Hdp2::Directory_recursive_create[$log_dir] -> Hdp2::Exec[$daemon_cmd] 
    }
  }
  if ($ensure == 'running') {
    #TODO: look at Puppet resource retry and retry_sleep
    #TODO: can make sleep contingent on $name
    $sleep = 5
    $post_check = "sleep ${sleep}; ${service_is_up}"
    hdp2::exec { $post_check:
      command => $post_check,
      unless  => $service_is_up
    }
    Hdp2::Exec[$daemon_cmd] -> Hdp2::Exec[$post_check] -> Anchor["hdp2-hadoop::service::${name}::end"]
  }  
}

