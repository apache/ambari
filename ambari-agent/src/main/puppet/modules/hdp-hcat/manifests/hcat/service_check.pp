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
class hdp-hcat::hcat::service_check() 
{
  include hdp-hcat::params
  $unique = hdp_unique_id_and_date()
  $smoke_test_user = $hdp::params::smokeuser
  $output_file = "/apps/hive/warehouse/hcatsmoke${unique}"
  $security_enabled=$hdp::params::security_enabled
  $smoke_user_keytab = $hdp::params::smokeuser_keytab

  if ($security_enabled == true) {
    $smoke_user_kinitcmd="${hdp::params::kinit_path_local} -kt ${smoke_user_keytab} ${smoke_test_user}; "
  } else {
    $smoke_user_kinitcmd=""
  }

  $test_cmd = "fs -test -e ${output_file}" 
  
  anchor { 'hdp-hcat::hcat::service_check::begin':}

  file { '/tmp/hcatSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-hcat/hcatSmoke.sh",
    mode => '0755',
  }

  exec { 'hcatSmoke.sh prepare':
    command   => "su - ${smoke_test_user} -c '${smoke_user_kinitcmd}sh /tmp/hcatSmoke.sh hcatsmoke${unique} prepare'",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/hcatSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    notify    => Hdp-hadoop::Exec-hadoop['hcat::service_check::test'],
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'hcat::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Exec['hcatSmoke.sh prepare'],
  }

  exec { 'hcatSmoke.sh cleanup':
    command   => "su - ${smoke_test_user} -c '${smoke_user_kinitcmd}sh /tmp/hcatSmoke.sh hcatsmoke${unique} cleanup'",
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    require   => Hdp-hadoop::Exec-hadoop['hcat::service_check::test'],
    before    => Anchor['hdp-hcat::hcat::service_check::end'],
    logoutput => "true"
  }
  
  anchor{ 'hdp-hcat::hcat::service_check::end':}
}
