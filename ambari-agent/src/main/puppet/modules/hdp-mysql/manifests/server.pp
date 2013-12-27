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
class hdp-mysql::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-mysql::params
{ 
  if ($service_state in ['no_op','uninstalled']) {
   } elsif ($service_state in ['running','stopped','installed_and_configured']) {
   
    $db_user = $hdp-mysql::params::db_user
    $db_pw = hdp_escape_spec_characters($hdp-mysql::params::db_pw)
    $db_name = $hdp-mysql::params::db_name
    $host = $hdp::params::hive_mysql_host 

    anchor { 'hdp-mysql::server::begin':}

    hdp::package { 'mysql' :
      size   => 64,
      require   => Anchor['hdp-mysql::server::begin']
    }


    if ($hdp::params::hdp_os_type == "suse") {
      # On Suse, creating symlink from default mysqld pid file to expected /var/run location
	  
      hdp::directory_recursive_create {'/var/run/mysqld/':
        require => Hdp::Package['mysql'],
        owner => $mysql_user,
        group => $mysql_group
      }
	  
      file { '/var/run/mysqld/mysqld.pid':
        ensure => 'link',
        target => '/var/lib/mysql/mysqld.pid',
        require => Hdp::Directory_recursive_create['/var/run/mysqld/'],
      }
    }


    if hdp_is_empty($hdp::params::services_names[mysql]) {
      hdp_fail("There is no service name for service mysql")
    }
    else {
      $service_name_by_os = $hdp::params::services_names[mysql]
    }

    if hdp_is_empty($service_name_by_os[$hdp::params::hdp_os_type]) {
      
      if hdp_is_empty($service_name_by_os['ALL']) {
        hdp_fail("There is no service name for service mysql")
      }
      else {
        $service_name = $service_name_by_os['ALL']
      }
    }
    else {
      $service_name = $service_name_by_os[$hdp::params::hdp_os_type]
    }

    $mysqld_state = $service_state ? {
     'running' => 'running',
     'installed_and_configured' => 'running',
      default =>  'stopped',
    }

    if ($mysqld_state == 'running') {
      $command = "service $service_name start"
    } else {
      $command = "service $service_name stop"
    }

    if ($hdp::params::hdp_os_type == "suse") {
      exec { $service_name:
        command => $command,
        path    => "/usr/local/bin/:/bin/:/sbin/",
        require => File['/var/run/mysqld/mysqld.pid'],
        notify  => File['/tmp/addMysqlUser.sh']
       }
     } else {
      exec { $service_name:
        command => $command,
        path    => "/usr/local/bin/:/bin/:/sbin/", 
        require => Hdp::Package['mysql'],
        notify  => File['/tmp/addMysqlUser.sh']
       }
     }


    if ($service_state == 'installed_and_configured') {

      file {'/tmp/addMysqlUser.sh':
        ensure => present,
        source => "puppet:///modules/hdp-mysql/addMysqlUser.sh",
        mode => '0755',
        require => Exec[$service_name],
        notify => Exec['/tmp/addMysqlUser.sh'],
      }
      # We start the DB and add a user
      exec { '/tmp/addMysqlUser.sh':
        command   => "bash -x /tmp/addMysqlUser.sh ${service_name} ${db_user} \"${db_pw}\" ${host}",
        tries     => 3,
        try_sleep => 5,
        require   => File['/tmp/addMysqlUser.sh'],
        path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
        notify   => Anchor['hdp-mysql::server::end'],
        logoutput => "true",
      }
    } else {
      # Now MySQL is running so we remove the temporary file
      file {'/tmp/addMysqlUser.sh':
        ensure => absent,
        require => Exec[$service_name],
        notify => Anchor['hdp-mysql::server::end'],
      }
    }

    anchor { 'hdp-mysql::server::end':}

  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
