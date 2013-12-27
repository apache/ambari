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
class hdp-zookeeper::zookeeper::service_check()
{
  include hdp-zookeeper::params
  $conf_dir = $hdp-zookeeper::params::conf_dir
  $smoke_script = $hdp::params::zk_smoke_test_script
  $security_enabled = $hdp::params::security_enabled
  $smoke_test_user = $hdp::params::smokeuser
  $zookeeper_smoke_shell_files = ['zkService.sh']
  $kinit_path = $hdp::params::kinit_path_local
  $smoke_user_keytab = $hdp::params::smokeuser_keytab
  anchor { 'hdp-zookeeper::zookeeper::service_check::begin':}

  hdp-zookeeper::zookeeper_smoke_shell_file { $zookeeper_smoke_shell_files: }

  anchor{ 'hdp-zookeeper::zookeeper::service_check::end':}
}

define hdp-zookeeper::zookeeper_smoke_shell_file()
{
  file { '/tmp/zkService.sh':
    ensure => present,
    source => "puppet:///modules/hdp-zookeeper/zkService.sh",
    mode => '0755'
  }

  exec { '/tmp/zkService.sh':
    command   => "sh /tmp/zkSmoke.sh ${smoke_script} ${smoke_test_user} ${conf_dir} ${clientPort} ${security_enabled} ${kinit_path} ${smoke_user_keytab}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/zkService.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
