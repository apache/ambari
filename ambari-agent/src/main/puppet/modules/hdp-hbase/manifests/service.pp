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
define hdp-hbase::service(
  $ensure = 'running',
  $create_pid_dir = true,
  $create_conf_dir = true,
  $initial_wait = undef)
{
  include hdp-hbase::params

  $role = $name
  $user = $hdp-hbase::params::hbase_user

  $conf_dir = $hdp::params::hbase_conf_dir
  $hbase_daemon = $hdp::params::hbase_daemon_script
  $cmd = "$hbase_daemon --config ${conf_dir}"
  $pid_dir = $hdp-hbase::params::hbase_pid_dir
  $pid_file = "${pid_dir}/hbase-hbase-${role}.pid"
  $hbase_log_dir = $hdp-hbase::params::hbase_log_dir
  $hbase_tmp_dir = $hdp-hbase::params::hbase_tmp_dir

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} start ${role}'"
    $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "su - ${user} -c  '${cmd} stop ${role}' && rm -f ${pid_file}"
    $no_op_test = undef
  } else {
    $daemon_cmd = undef
  }

  $tag = "hbase_service-${name}"
  
  if ($create_pid_dir == true) {
    hdp::directory_recursive_create { $pid_dir: 
      owner => $user,
      tag   => $tag,
      service_state => $ensure,
      force => true
    }
  }
  if ($create_conf_dir == true) {
   # To avoid duplicate resource definitions
    $hbase_conf_dirs = hdp_set_from_comma_list("${hbase_tmp_dir},${hbase_log_dir}")

    hdp::directory_recursive_create_ignore_failure { $hbase_conf_dirs:
      owner => $user,
      context_tag => 'hbase_service',
      service_state => $ensure,
      force => true
    }
  }

  if ($daemon_cmd != undef) { 
    hdp::exec { $daemon_cmd:
      command      => $daemon_cmd,
      unless       => $no_op_test,
      initial_wait => $initial_wait
    }
    anchor{"hdp-hbase::service::${name}::begin":} -> Hdp::Directory_recursive_create<|tag == $tag|> -> Hdp::Exec[$daemon_cmd] -> anchor{"hdp-hbase::service::${name}::end":}
  } else {
    anchor{"hdp-hbase::service::${name}::begin":} -> Hdp::Directory_recursive_create<|tag == $tag|> -> anchor{"hdp-hbase::service::${name}::end":}  
  }
}