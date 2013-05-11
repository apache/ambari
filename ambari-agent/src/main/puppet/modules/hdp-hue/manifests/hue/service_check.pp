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
class hdp-hue::hue::service_check() inherits hdp-hue::params
{
  $status_check_cmd = "/etc/init.d/hue status | grep 'is running'"
  $smoke_test_cmd = "${hue_home_dir}/build/env/bin/hue smoke_test"

  anchor { 'hdp-hue::hue::service_check::begin' : }

  exec { 'hue-status-check':
    command   => $status_check_cmd,
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }

  exec { 'hue-smoke-test':
      command   => $smoke_test_cmd,
      tries     => 3,
      try_sleep => 5,
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      require   => Exec['hue-status-check'],
      before    => Anchor['hdp-hue::hue::service_check::end'],
      logoutput => "true"
    }

  anchor { 'hdp-hue::hue::service_check::end' : }
}