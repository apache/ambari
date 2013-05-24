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
class hdp-hbase::hbase::service_check()
{
  $smoke_test_user = $hdp::params::smokeuser

  $output_file = "/apps/hbase/data/ambarismoketest"
  $conf_dir = $hdp::params::hbase_conf_dir

  $test_cmd = "fs -test -e ${output_file}"
  $serviceCheckData = hdp_unique_id_and_date()

  anchor { 'hdp-hbase::hbase::service_check::begin':}

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

  exec { $hbase_servicecheck_file:
    command   => "su - ${smoke_test_user} -c 'hbase --config $conf_dir  shell $hbase_servicecheck_file'",
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }

  exec { '/tmp/hbaseSmokeVerify.sh':
    command   => "su - ${smoke_test_user} -c '/tmp/hbaseSmokeVerify.sh $conf_dir ${serviceCheckData}'",
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

  Anchor['hdp-hbase::hbase::service_check::begin'] ->  File['/tmp/hbaseSmokeVerify.sh']
  File[$hbase_servicecheck_file] -> Exec[$hbase_servicecheck_file] -> Exec['/tmp/hbaseSmokeVerify.sh']
  -> Anchor['hdp-hbase::hbase::service_check::end']

  anchor{ 'hdp-hbase::hbase::service_check::end':}
}