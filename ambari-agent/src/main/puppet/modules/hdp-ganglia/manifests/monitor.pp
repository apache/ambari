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
class hdp-ganglia::monitor(
  $service_state = $hdp::params::cluster_service_state,
  $ganglia_server_host = undef,
  $opts = {}
) inherits hdp-ganglia::params
{
  if  ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {     

   hdp::package { 'ganglia-monitor':         
       ensure      => 'uninstalled', 
      java_needed => false      
   }

  } else {
    if ($hdp::params::service_exists['hdp-ganglia::server'] != true) {
      class { 'hdp-ganglia':
       service_state => $service_state
      }
    }

    hdp::package { 'ganglia-monitor': }

    hdp::package { 'ganglia-gmond-modules-python': }

    if ($hdp::params::service_exists['hdp-ganglia::server'] != true) {
      class { 'hdp-ganglia::config': ganglia_server_host => $ganglia_server_host}
    }

    if (($hdp::params::service_exists['hdp-hadoop::datanode'] == true) or ($hdp::params::service_exists['hdp-hadoop::namenode'] == true) or ($hdp::params::service_exists['hdp-hadoop::jobtracker'] == true) or ($hdp::params::service_exists['hdp-hadoop::tasktracker'] == true) or ($hdp::params::service_exists['hdp-yarn::resourcemanager'] == true) or ($hdp::params::service_exists['hdp-yarn::nodemanager'] == true) or ($hdp::params::service_exists['hdp-yarn::historyserver'] == true) or ($hdp::params::service_exists['hdp-hadoop::client'] == true) or ($hdp::params::service_exists['hdp-hadoop::snamenode'] == true)) {
     class { 'hdp-hadoop::enable-ganglia': }
   }

    if ($service_exists['hdp-hbase::master'] == true) {
      class { 'hdp-hbase::master::enable-ganglia': }
    }
  
    if ($service_exists['hdp-hbase::regionserver'] == true) {
      class { 'hdp-hbase::regionserver::enable-ganglia': }
    }

    class { 'hdp-ganglia::monitor::config-gen': }
  
    class { 'hdp-ganglia::monitor::gmond': ensure => $service_state}

    class { 'hdp-ganglia::monitor::ownership': }

    if ($hdp::params::service_exists['hdp-ganglia::server'] != true) {
      Class['hdp-ganglia'] -> Hdp::Package['ganglia-monitor'] -> Hdp::Package['ganglia-gmond-modules-python'] -> Class['hdp-ganglia::config'] -> 
        Class['hdp-ganglia::monitor::config-gen'] -> Class['hdp-ganglia::monitor::ownership'] ->
        Class['hdp-ganglia::monitor::gmond']
    } else {
      Hdp::Package['ganglia-monitor'] -> Hdp::Package['ganglia-gmond-modules-python'] -> Class['hdp-ganglia::monitor::config-gen'] ->
        Class['hdp-ganglia::monitor::ownership'] -> Class['hdp-ganglia::monitor::gmond']
    }
  }
}


class hdp-ganglia::monitor::config-gen()
{

  $service_exists = $hdp::params::service_exists

  if ($hdp::params::is_namenode_master) {
    hdp-ganglia::config::generate_daemon { 'HDPNameNode':}
  }
  if ($hdp::params::is_jtnode_master) {
    hdp-ganglia::config::generate_daemon { 'HDPJobTracker':}
  }
  if ($hdp::params::is_rmnode_master) {
    hdp-ganglia::config::generate_daemon { 'HDPResourceManager':}
  }
  if ($hdp::params::is_hsnode_master) {
    hdp-ganglia::config::generate_daemon { 'HDPHistoryServer':}
  }
  if ($hdp::params::is_hbase_master) {
    hdp-ganglia::config::generate_daemon { 'HDPHBaseMaster':}
  }
  
  if (($hdp::params::is_slave == true) 
    or (($hdp::params::is_namenode_master == false) 
      and ($hdp::params::is_jtnode_master == false) 
      and ($hdp::params::is_rmnode_master == false) 
      and ($hdp::params::is_hsnode_master == false) 
      and ($hdp::params::is_hbase_master ==  false))) {
    hdp-ganglia::config::generate_daemon { 'HDPSlaves':}
  }

  Hdp-ganglia::Config::Generate_daemon<||>{
    ganglia_service => 'gmond',
    role => 'monitor'
  }
   # 
  anchor{'hdp-ganglia::monitor::config-gen::begin':} -> Hdp-ganglia::Config::Generate_daemon<||> -> anchor{'hdp-ganglia::monitor::config-gen::end':}
}

class hdp-ganglia::monitor::gmond(
  $ensure
  )
{
  if ($ensure == 'running') {
    class { 'hdp-ganglia::server::delete_default_gmond_process': }
    $command = "service hdp-gmond start >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1"
   } elsif  ($ensure == 'stopped') {
    $command = "service hdp-gmond stop >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1"
  }
  if ($ensure == 'running' or $ensure == 'stopped') {
    hdp::exec { "hdp-gmond service" :
      command => $command,
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    }
  }
}

class hdp-ganglia::monitor::ownership() {

  file { "${hdp-ganglia::params::ganglia_dir}/conf.d":
    owner  => 'root',
    group  => $hdp::params::user_group
  }

  file { "${hdp-ganglia::params::ganglia_dir}/conf.d/modgstatus.conf":
    owner => 'root',
    group => $hdp::params::user_group
  }

  file { "${hdp-ganglia::params::ganglia_dir}/conf.d/multicpu.conf":
    owner => 'root',
    group => $hdp::params::user_group
  }

  file { "${hdp-ganglia::params::ganglia_dir}/gmond.conf":
    owner => 'root',
    group => $hdp::params::user_group
  }
}

class hdp-ganglia::server::delete_default_gmond_process() {
  hdp::exec { "delete_default_gmond_process" :
    command => "chkconfig gmond off",
    path => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    require => Class['hdp-ganglia::monitor::gmond']
  }
}
