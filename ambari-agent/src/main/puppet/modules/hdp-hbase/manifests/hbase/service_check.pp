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
class hdp-hbase::hbase::service_check() inherits hdp-hbase::params
{
  $smoke_test_user = $hdp::params::smokeuser
  $security_enabled = $hdp::params::security_enabled
  $conf_dir = $hdp::params::hbase_conf_dir
  $smoke_user_keytab = $hdp::params::smokeuser_keytab
  $hbase_user = $hdp-hbase::params::hbase_user
  $hbase_keytab = $hdp::params::hbase_user_keytab
  $serviceCheckData = hdp_unique_id_and_date()
  $kinit_cmd = "${hdp::params::kinit_path_local} -kt ${smoke_user_keytab} ${smoke_test_user};"

  anchor { 'hdp-hbase::hbase::service_check::begin':}

  if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2){
    $output_file = "${hbase_hdfs_root_dir}/data/default/ambarismoketest"
  } else {
    $output_file = "${hbase_hdfs_root_dir}/ambarismoketest"
  }

  $test_cmd = "fs -test -e ${output_file}"

  $hbase_servicecheck_file = '/tmp/hbase-smoke.sh'

  file { '/tmp/hbaseSmokeVerify.sh':
    ensure => present,
    source => "puppet:///modules/hdp-hbase/hbaseSmokeVerify.sh",
    mode => '0755',
  }

  file { $hbase_servicecheck_file:
    mode => '0755',
    content => template('hdp-hbase/hbase-smoke.sh.erb'),
  }
  if ($security_enabled == true) {
    $servicecheckcmd = "su - ${smoke_test_user} -c '$kinit_cmd hbase --config $conf_dir  shell $hbase_servicecheck_file'"
    $smokeverifycmd = "su - ${smoke_test_user} -c '$kinit_cmd /tmp/hbaseSmokeVerify.sh $conf_dir ${serviceCheckData}'"
  } else {
    $servicecheckcmd = "su - ${smoke_test_user} -c 'hbase --config $conf_dir  shell $hbase_servicecheck_file'"
    $smokeverifycmd = "su - ${smoke_test_user} -c '/tmp/hbaseSmokeVerify.sh $conf_dir ${serviceCheckData}'"
  }

  exec { $hbase_servicecheck_file:
    command   => $servicecheckcmd,
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }

  exec { '/tmp/hbaseSmokeVerify.sh':
    command   => $smokeverifycmd,
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    notify    => Hdp-hadoop::Exec-hadoop['hbase::service_check::test'],
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'hbase::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Exec['/tmp/hbaseSmokeVerify.sh'],
    before      => Anchor['hdp-hbase::hbase::service_check::end'] #TODO: remove after testing
  }

  if ($security_enabled == true) {
    $hbase_grant_premissions_file = '/tmp/hbase_grant_permissions.sh'
    $hbase_kinit_cmd = "${hdp::params::kinit_path_local} -kt ${hbase_keytab} ${hbase_user};"
    $grantprivelegecmd = "$hbase_kinit_cmd hbase shell ${hbase_grant_premissions_file}"

    file { $hbase_grant_premissions_file:
      owner   => $hbase_user,
      group   => $hdp::params::user_group,
      mode => '0644',
      content => template('hdp-hbase/hbase_grant_permissions.erb')
      }
      hdp-hadoop::exec-hadoop { '${smokeuser}_grant_privileges' :
        command => $grantprivelegecmd,
        require => File[$hbase_grant_premissions_file],
        user => $hbase_user
      }
     Anchor['hdp-hbase::hbase::service_check::begin'] ->  File['/tmp/hbaseSmokeVerify.sh']
       File[$hbase_servicecheck_file] ->  File[$hbase_grant_premissions_file] ->
       Hdp-hadoop::Exec-hadoop['${smokeuser}_grant_privileges'] ->
       Exec[$hbase_servicecheck_file] ->
       Exec['/tmp/hbaseSmokeVerify.sh'] -> Anchor['hdp-hbase::hbase::service_check::end']
  } else {
    Anchor['hdp-hbase::hbase::service_check::begin'] ->  File['/tmp/hbaseSmokeVerify.sh']
    File[$hbase_servicecheck_file] -> Exec[$hbase_servicecheck_file] -> Exec['/tmp/hbaseSmokeVerify.sh']
    -> Anchor['hdp-hbase::hbase::service_check::end']
  }
  anchor{ 'hdp-hbase::hbase::service_check::end':}
}