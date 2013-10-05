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
class hdp-pig::pig::service_check() 
{
  $smoke_test_user = $hdp::params::smokeuser
  $input_file = 'passwd'
  $output_file = "pigsmoke.out"

  $cleanup_cmd = "dfs -rmr ${output_file} ${input_file}"
  #cleanup put below to handle retries; if retrying there wil be a stale file that needs cleanup; exit code is fn of second command
  $create_file_cmd = "${cleanup_cmd}; hadoop dfs -put /etc/passwd ${input_file} " #TODO: inconsistent that second command needs hadoop
  $test_cmd = "fs -test -e ${output_file}" 
  
  anchor { 'hdp-pig::pig::service_check::begin':}


  hdp-hadoop::exec-hadoop { 'pig::service_check::create_file':
    command   => $create_file_cmd,
    tries     => 3,
    try_sleep => 5,
    require   => Anchor['hdp-pig::pig::service_check::begin'],
    notify    => File['/tmp/pigSmoke.sh'],
    user      => $smoke_test_user
  }

  file { '/tmp/pigSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-pig/pigSmoke.sh",
    mode => '0755',
    require     => Hdp-hadoop::Exec-hadoop['pig::service_check::create_file']
  }

  exec { '/tmp/pigSmoke.sh':
    command   => "su - ${smoke_test_user} -c 'pig /tmp/pigSmoke.sh'",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/pigSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    notify    => Hdp-hadoop::Exec-hadoop['pig::service_check::test'],
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'pig::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Exec['/tmp/pigSmoke.sh'],
    before      => Anchor['hdp-pig::pig::service_check::end'], #TODO: remove after testing
    user      => $smoke_test_user
  }
  
  anchor{ 'hdp-pig::pig::service_check::end':}
}
