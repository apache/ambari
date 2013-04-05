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
class hdp-hive::service(
  $ensure,
  $service_type
)
{
  include $hdp-hive::params
  
  $user = $hdp-hive::params::hive_user
  $hadoop_home = $hdp::params::hadoop_home
  $hive_log_dir = $hdp-hive::params::hive_log_dir

  $start_hiveserver2_path = "/tmp/$start_hiveserver2_script"
  $start_metastore_path = "/tmp/$start_metastore_script"

  if ($service_type == 'metastore') {

    $pid_file = "${hdp-hive::params::hive_pid_dir}/hive.pid" 
    $cmd = "env HADOOP_HOME=${hadoop_home} JAVA_HOME=$hdp::params::java64_home $start_metastore_path ${hive_log_dir}/hive.out ${hive_log_dir}/hive.log $pid_file"
    
  } elsif ($service_type == 'hiveserver2') {
    $pid_file = "$hive_pid_dir/$hive_pid" 
    $cmd = "env JAVA_HOME=$hdp::params::java64_home $start_hiveserver2_path ${hive_log_dir}/hive-server2.out  ${hive_log_dir}/hive-server2.log $pid_file"
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_type}")
  }


  $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"

  if ($ensure == 'running') {
    $daemon_cmd = "su - ${user} -c  '${cmd} '"
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = "kill `cat $pid_file` >/dev/null 2>&1"
  } else {
    $daemon_cmd = undef
  }

  hdp-hive::service::directory { $hive_pid_dir : }
  hdp-hive::service::directory { $hive_log_dir : }
  hdp-hive::service::directory { $hive_var_lib : }

  file { $start_hiveserver2_path:
    ensure => present,
    source => "puppet:///modules/hdp-hive/$start_hiveserver2_script",
    mode => '0755',
  }

  file { $start_metastore_path:
    ensure => present,
    source => "puppet:///modules/hdp-hive/$start_metastore_script",
    mode => '0755',
  }

  anchor{'hdp-hive::service::begin':} -> Hdp-hive::Service::Directory<||> -> anchor{'hdp-hive::service::end':}
  
  if ($daemon_cmd != undef) {
    if ($ensure == 'running') {

      $pid_file_state = 'present'
      hdp::exec { $daemon_cmd:
        command => $daemon_cmd,
        unless  => $no_op_test
      }
    } elsif ($ensure == 'stopped') {
      $pid_file_state = 'absent'
      hdp::exec { $daemon_cmd:
        command => $daemon_cmd,
        onlyif  => $no_op_test
      }
    }


  file { $pid_file:
    ensure => $pid_file_state
  }

    Hdp-hive::Service::Directory<||> File[ $start_metastore_path]-> File[ $start_hiveserver2_path]-> Hdp::Exec[$daemon_cmd] -> File[$pid_file] -> Anchor['hdp-hive::service::end']
  }
}

define hdp-hive::service::directory()
{
  hdp::directory_recursive_create { $name: 
    owner => $hive_user,
    mode => '0755',
    service_state => $ensure,
    force => true
  }
}

