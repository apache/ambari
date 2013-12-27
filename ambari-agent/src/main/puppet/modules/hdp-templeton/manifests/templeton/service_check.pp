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
class hdp-templeton::templeton::service_check()
{
  include hdp-templeton::params
  $smoke_test_user = $hdp::params::smokeuser
  $security_enabled=$hdp::params::security_enabled
  if ($security_enabled == true) {
    $security = "true"
  } else {
    $security = "false"
  }
  $kinit_path = $hdp::params::kinit_path_local
  $smoke_user_keytab = $hdp::params::smokeuser_keytab

  $templeton_host = $hdp::params::webhcat_server_host

  $smoke_shell_files = ['templetonSmoke.sh']

  anchor { 'hdp-templeton::templeton::service_check::begin':}

  hdp-templeton::smoke_shell_file { $smoke_shell_files: }

  anchor{ 'hdp-templeton::templeton::service_check::end':}
}

define hdp-templeton::smoke_shell_file()
{
  $smoke_test_user = $hdp::params::smokeuser
    
  $security = $hdp-templeton::templeton::service_check::security

  $kinit_path = $hdp::params::kinit_path_local
  $smoke_user_keytab = $hdp::params::smokeuser_keytab

  $templeton_host = $hdp::params::webhcat_server_host

  file { '/tmp/templetonSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-templeton/templetonSmoke.sh",
    mode => '0755'
  }
  
  exec { '/tmp/templetonSmoke.sh':
    command   => "sh /tmp/templetonSmoke.sh ${templeton_host} ${smoke_test_user} ${smoke_user_keytab} ${security} ${kinit_path}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/templetonSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
