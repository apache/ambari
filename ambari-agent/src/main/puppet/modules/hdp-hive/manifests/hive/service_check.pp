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
class hdp-hive::hive::service_check() inherits hdp-hive::params
{
  $smoke_test_user = $hdp::params::smokeuser
  $smoke_test_sql = "/tmp/$smoke_test_sql_file"
  $smoke_test_path = "/tmp/$smoke_test_script"
  $security_enabled = $hdp::params::security_enabled
  $smoke_user_keytab = $hdp::params::smokeuser_keytab

  if ($security_enabled == true) {
    $kinit_cmd = "${hdp::params::kinit_path_local} -kt ${smoke_user_keytab} ${smoke_test_user};"
    $hive_principal_ext = "principal=${hdp-hive::params::hive_metatore_keytab_path}"
    $hive_url_ext = "${hive_url}/\\;${hive_principal_ext}"
    $smoke_cmd = "${kinit_cmd} env JAVA_HOME=${hdp::params::java64_home} ${smoke_test_path} ${hive_url_ext} ${smoke_test_sql}"
  } else {
    $smoke_cmd = "env JAVA_HOME=$hdp::params::java64_home $smoke_test_path $hive_url $smoke_test_sql"
  }


  file { $smoke_test_path:
    ensure => present,
    source => "puppet:///modules/hdp-hive/$smoke_test_script",
    mode => '0755',
  }

  file { $smoke_test_sql:
    ensure => present,
    source => "puppet:///modules/hdp-hive/$smoke_test_sql_file"
  }

  exec { $smoke_test_path:
    command   => $smoke_cmd,
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true",
    user => $smoke_test_user
  }

#  $unique = hdp_unique_id_and_date()
#  $output_file = "/apps/hive/warehouse/hivesmoke${unique}"
#  $test_cmd = "fs -test -e ${output_file}"

#  file { '/tmp/hiveSmoke.sh':
#    ensure => present,
#    source => "puppet:///modules/hdp-hive/hiveSmoke.sh",
#    mode => '0755',
#  }
#
#  exec { '/tmp/hiveSmoke.sh':
#    command => "su - ${smoke_test_user} -c 'env JAVA_HOME=$hdp::params::java64_home sh /tmp/hiveSmoke.sh hivesmoke${unique}'",
#    tries => 3,
#    try_sleep => 5,
#    path => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
#    notify => Hdp-hadoop::Exec-hadoop['hive::service_check::test'],
#    logoutput => "true"
#  }

#  hdp-hadoop::exec-hadoop { 'hive::service_check::test':
#    command => $test_cmd,
#    refreshonly => true
#  }

#  File[$smoke_test_path] -> File[$smoke_test_sql] -> Exec[$smoke_test_path] -> File['/tmp/hiveSmoke.sh'] -> Exec['/tmp/hiveSmoke.sh'] -> Hdp-Hadoop::Exec-Hadoop['hive::service_check::test']

  include hdp-hcat::hcat::service_check  

  File[$smoke_test_path] -> File[$smoke_test_sql] -> Exec[$smoke_test_path]
}
