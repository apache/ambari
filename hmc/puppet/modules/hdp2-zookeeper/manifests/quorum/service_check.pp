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
class hdp2-zookeeper::quorum::service_check()
{
  include hdp2-zookeeper::params
  $conf_dir = $hdp2-zookeeper::params::conf_dir

  $smoke_test_user = $hdp2::params::smokeuser

  $smoke_script = $hdp2::params::zk_smoke_test_script
  $quorum_smoke_shell_files = ['zkSmoke.sh']

  anchor { 'hdp2-zookeeper::quorum::service_check::begin':}

  hdp2-zookeeper::quorum_smoke_shell_file { $quorum_smoke_shell_files: }

  anchor{ 'hdp2-zookeeper::quorum::service_check::end':}
}

define hdp2-zookeeper::quorum_smoke_shell_file()
{
  file { '/tmp/zkSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp2-zookeeper/zkSmoke.sh",
    mode => '0755'
  }

  exec { '/tmp/zkSmoke.sh':
    command   => "sh /tmp/zkSmoke.sh ${smoke_script} ${smoke_test_user} ${conf_dir}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/zkSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
