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
define hdp-yarn::service(
  $ensure = 'running',
  $user,
  $initial_wait = undef,
  $create_pid_dir = true,
  $create_log_dir = true
)
{

  $security_enabled = $hdp::params::security_enabled
  
  if ($name == 'historyserver') {
    $log_dir = "${hdp-yarn::params::mapred_log_dir_prefix}/${user}"
    $pid_dir = "${hdp-yarn::params::mapred_pid_dir_prefix}/${user}"
    $daemon = "${hdp::params::mapred_bin}/mr-jobhistory-daemon.sh"
    $pid_file = "${pid_dir}/mapred-${user}-${name}.pid"
    $job_summary_log = "${hdp-yarn::params::mapred_log_dir_prefix}/${user}/hadoop-mapreduce.jobsummary.log"
  } else {
    $log_dir = "${hdp-yarn::params::yarn_log_dir_prefix}/${user}"
    $pid_dir = "${hdp-yarn::params::yarn_pid_dir_prefix}/${user}"
    $daemon = "${hdp::params::yarn_bin}/yarn-daemon.sh"
    $pid_file = "${pid_dir}/yarn-${user}-${name}.pid"
    $job_summary_log = "${hdp-yarn::params::yarn_log_dir_prefix}/${user}/hadoop-mapreduce.jobsummary.log"
  }
  
  $hadoop_libexec_dir = $hdp-yarn::params::hadoop_libexec_dir
   
  $cmd = "export HADOOP_LIBEXEC_DIR=${hadoop_libexec_dir} && ${daemon} --config ${hdp-yarn::params::conf_dir}"
  
  if ($ensure == 'running') {
    if ($run_as_root == true) {
      $daemon_cmd = "${cmd} start ${name}"
    } else {
      $daemon_cmd = "su - ${user} -c  '${cmd} start ${name}'"
    }
    $service_is_up = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    if ($run_as_root == true) {
      $daemon_cmd = "${cmd} stop ${name} && rm -f ${pid_file}"
    } else {
      $daemon_cmd = "su - ${user} -c  '${cmd} stop ${name}' && rm -f ${pid_file}"
    }
    $service_is_up = undef
  } else {
    $daemon_cmd = undef
  }
 
   if ($create_pid_dir == true) {
    hdp::directory_recursive_create { $pid_dir: 
      owner       => $user,
      context_tag => 'yarn_service',
      service_state => $ensure,
      force => true
    }
  }
 
  if ($create_log_dir == true) {
    hdp::directory_recursive_create { $log_dir: 
      owner       => $user,
      context_tag => 'yarn_service',
      service_state => $ensure,
      force => true
    }

    file {$job_summary_log:
      path => $job_summary_log,
      owner => $user,
    }
  }
 
  if ($daemon_cmd != undef) {  
    hdp::exec { $daemon_cmd:
      command      => $daemon_cmd,
      unless       => $service_is_up,
      initial_wait => $initial_wait
    }
  }

  anchor{"hdp-yarn::service::${name}::begin":}
  anchor{"hdp-yarn::service::${name}::end":}
  if ($daemon_cmd != undef) {
    Anchor["hdp-yarn::service::${name}::begin"] -> Hdp::Directory_recursive_create<|title == $pid_dir or title == $log_dir|> -> File[$job_summary_log] -> Hdp::Exec[$daemon_cmd] -> Anchor["hdp-yarn::service::${name}::end"]

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
    Hdp::Exec[$daemon_cmd] -> Hdp::Exec[$post_check] -> Anchor["hdp-yarn::service::${name}::end"]
  }  
}
