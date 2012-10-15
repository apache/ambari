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
class hdp2-mysql::server(
  $service_state = $hdp2::params::cluster_service_state,
  $opts = {}
) inherits  hdp2-mysql::params
{ 
  if ($service_state in ['no_op','uninstalled']) {
   } elsif ($service_state in ['running','stopped','installed_and_configured']) {
   
    $db_user = $hdp2-mysql::params::db_user
    $db_pw = $hdp2-mysql::params::db_pw
    $db_name = $hdp2-mysql::params::db_name
    $host = $hdp2::params::hive_mysql_host 

    anchor { 'hdp2-mysql::server::begin':}

    hdp2::package { 'mysql' :
      size   => 32,
      require   => Anchor['hdp2-mysql::server::begin']
    }

    hdp2::exec { 'mysqld start':
        command => '/etc/init.d/mysqld start',
        unless  => '/etc/init.d/mysqld status',
        require => Hdp2::Package['mysql'],
        notify  => File['/tmp/startMysql.sh']
    }

    file { '/tmp/startMysql.sh':
      ensure => present,
      source => "puppet:///modules/hdp2-mysql/startMysql.sh",
      mode => '0755',
      require => Hdp2::Exec['mysqld start'],
      notify => Exec['/tmp/startMysql.sh']
    }

    exec { '/tmp/startMysql.sh':
      command   => "sh /tmp/startMysql.sh ${db_user} ${db_pw} ${host}",
      tries     => 3,
      try_sleep => 5,
      require   => File['/tmp/startMysql.sh'],
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      notify   => Anchor['hdp2-mysql::server::end'],
      logoutput => "true"
    }

    anchor { 'hdp2-mysql::server::end':}

  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
