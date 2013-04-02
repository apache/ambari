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
class hdp-hue::service(
  $ensure,
  $initial_wait = undef
)
{
  include $hdp-hue::params

  $hue_user = $hdp-hue::params::hue_server_user
  $hue_start_cmd = "/etc/init.d/hue start --USER=${hue_user} --LOGDIR=${hue_log_dir} --LOCKFILE=${hue_lock_file} --PIDFILE=${hue_pid_dir}/supervisor.pid"
  $hue_stop_cmd = "/etc/init.d/hue stop"

  $pid_dir = $hdp-hue::params::hue_pid_dir
  $log_dir = $hdp-hue::params::hue_log_dir
  $pid_file = "${pid_dir}/supervisor.pid"
  $no_op_test = "ls ${pid_file} >/dev/null 2>&1 && ps `cat ${pid_file}` >/dev/null 2>&1"

  if ($ensure == 'running') {
    $daemon_cmd = $hue_start_cmd
  } elsif ($ensure == 'stopped') {
    $daemon_cmd = $hue_stop_cmd
  } else {
    $daemon_cmd = undef
  }

  hdp-hue::service::directory { $pid_dir :
    service_state => $ensure,
  }

  hdp-hue::service::directory { $log_dir :
    service_state => $ensure,
  }

  anchor {'hdp-hue::service::begin': } -> Hdp-hue::Service::Directory<||> -> anchor {'hdp-hue::service::end': }

  if ($daemon_cmd != undef) {
    hdp::exec { $daemon_cmd:
      command => $daemon_cmd,
      unless  => $no_op_test,
      initial_wait => $initial_wait
    }
    Hdp-hue::Service::Directory<||> -> Hdp::Exec[$daemon_cmd] -> Anchor['hdp-hue::service::end']
  }

}

define hdp-hue::service::directory(
 $service_state
)
{
  hdp::directory_recursive_create { $name:
    owner => $hdp-hue::params::hue_server_user,
    mode => '0755',
    service_state => $service_state,
    force => true
  }
}