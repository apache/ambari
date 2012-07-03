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
class hdp-oozie::oozie::service_check()
{
  include hdp-oozie::params

  $smoke_shell_files = ['oozieSmoke.sh']
  anchor { 'hdp-oozie::oozie::service_check::begin':}

  hdp-oozie::smoke_shell_file { $smoke_shell_files: }

  anchor{ 'hdp-oozie::oozie::service_check::end':}
}

define hdp-oozie::smoke_shell_file()
{
  $smoke_test_user = $hdp::params::smokeuser
  $conf_dir = $hdp::params::oozie_conf_dir
  $hadoopconf_dir = $hdp::params::hadoop_conf_dir 

  file { '/tmp/oozieSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-oozie/oozieSmoke.sh",
    mode => '0755'
  }

  exec { '/tmp/oozieSmoke.sh':
    command   => "sh /tmp/oozieSmoke.sh ${conf_dir} ${hadoopconf_dir} ${smoke_test_user}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/oozieSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
