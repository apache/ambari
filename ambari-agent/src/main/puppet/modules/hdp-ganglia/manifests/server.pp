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
class hdp-ganglia::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-ganglia::params
{
  $hdp::params::service_exists['hdp-ganglia::server'] = true

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {

   class { 'hdp-ganglia::server::packages':
      ensure => 'uninstalled',
      service_state => $service_state
   }

   class { 'hdp-ganglia::server::files':
      ensure => 'absent'
   }

  } else {
  class { 'hdp-ganglia':
    service_state => $service_state
  }

  class { 'hdp-ganglia::server::packages':
    ensure => 'present',
    service_state => $service_state
  }

  class { 'hdp-ganglia::config': 
    ganglia_server_host => $hdp::params::host_address,
    service_state       => $service_state 
  }

  if ($hdp::params::has_namenodes) {
    hdp-ganglia::config::generate_daemon { 'HDPNameNode':
      ganglia_service => 'gmond',
      role => 'server'
    }
  }
  
  if ($hdp::params::has_jobtracker) {
    hdp-ganglia::config::generate_daemon { 'HDPJobTracker':
      ganglia_service => 'gmond',
      role => 'server'
    }
  }
  
  if ($hdp::params::has_hbase_masters) {
    hdp-ganglia::config::generate_daemon { 'HDPHBaseMaster':
      ganglia_service => 'gmond',
      role => 'server'
    }
  }

  if ($hdp::params::has_resourcemanager) {
    hdp-ganglia::config::generate_daemon { 'HDPResourceManager':
      ganglia_service => 'gmond',
      role => 'server'
    }
  }
  
  if ($hdp::params::has_histroryserver) {
    hdp-ganglia::config::generate_daemon { 'HDPHistoryServer':
      ganglia_service => 'gmond',
      role => 'server'
    }
  }

  hdp-ganglia::config::generate_daemon { 'HDPSlaves':
    ganglia_service => 'gmond',
    role => 'server'
  }

  hdp-ganglia::config::generate_daemon { 'gmetad':
    ganglia_service => 'gmetad',
    role => 'server'
  }

  class { 'hdp-ganglia::server::gmetad': ensure => $service_state}

  class { 'hdp-ganglia::service::change_permission': ensure => $service_state }

  if ($service_state == 'installed_and_configured') {
    $webserver_state = 'restart'
  } elsif ($service_state == 'running') {
    $webserver_state = 'running'
  } else {
    # We are never stopping httpd
    #$webserver_state = $service_state
  }

  class { 'hdp-monitor-webserver': service_state => $webserver_state}

  class { 'hdp-ganglia::server::files':
     ensure => 'present'
  }

  file { "${hdp-ganglia::params::ganglia_dir}/gmetad.conf":
    owner => 'root',
    group => $hdp::params::user_group
  }

  #top level does not need anchors
  Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] ->
    Hdp-ganglia::Config::Generate_daemon<||> ->
    File["${hdp-ganglia::params::ganglia_dir}/gmetad.conf"] -> Class['hdp-ganglia::service::change_permission'] ->
    Class['hdp-ganglia::server::files'] -> Class['hdp-ganglia::server::gmetad'] -> Class['hdp-monitor-webserver']
 }
}

class hdp-ganglia::server::packages(
  $ensure = present,
  $service_state = 'installed_and_configured'
)
{
  hdp::package { ['libganglia','ganglia-devel','ganglia-server','ganglia-web']: 
    ensure      => $ensure,
    java_needed => false,
    require => Hdp::Package ['rrdtool-python']
  }

  # Removing conflicting packages only once to workaround "/bin/rpm -e absent-absent-absent.absent" bug (BUG-2881)
  if ($service_state == 'installed_and_configured' and $hdp::params::hdp_os_type == 'centos5') {
    # Remove conflicting 32bit package
    hdp::package { ['rrdtool-devel']:
      ensure      => 'absent',
      java_needed => false,
      before => Hdp::Package ['rrdtool']
    }

    # Remove conflicting 32bit package
    hdp::package { ['rrdtool']:
      ensure      => 'absent',
      java_needed => false,
      before => Hdp::Package ['rrdtool-python']
    }
  }

  hdp::package { ['rrdtool-python']:
    ensure      => $ensure,
    java_needed => false
  }

}

class hdp-ganglia::server::files(
  $ensure = present 
)
{
  $rrd_py_path = $hdp::params::rrd_py_path [$hdp::params::hdp_os_type]
  hdp::directory_recursive_create{$rrd_py_path:
    ensure => "directory", 
    override_owner => false 
  }

  $rrd_py_file_path = "${rrd_py_path}/rrd.py"

  file{$rrd_py_file_path :
    ensure => $ensure,
    source => "puppet:///modules/hdp-ganglia/rrd.py",
    mode   => '0755'
  }

  anchor{ 'hdp-ganglia::server::files::begin' : } -> Hdp::Directory_recursive_create[$rrd_py_path] -> File[$rrd_py_file_path] -> anchor{ 'hdp-ganglia::server::files::end' : }

  $rrd_files_dir = $hdp-ganglia::params::rrdcached_base_dir
  $rrd_file_owner = $hdp-ganglia::params::gmetad_user
  $rrdcached_default_file_dir = $hdp-ganglia::params::rrdcached_default_base_dir

  ## If directory is different fr omdefault make sure it exists
  if ($rrdcached_default_file_dir != $rrd_files_dir) {
    hdp::directory_recursive_create{ $rrd_files_dir :
      ensure => "directory",
      owner => $rrd_file_owner,
      group => $rrd_file_owner,
      mode => '0755'
    }

    file { $rrdcached_default_file_dir :
      ensure => link,
      target => $rrd_files_dir,
      force => true
    }

    File[$rrd_py_file_path] -> Hdp::Directory_recursive_create[$rrd_files_dir] -> File[$rrdcached_default_file_dir] -> Anchor['hdp-ganglia::server::files::end']
  }
  elsif ($rrd_file_owner != $hdp::params::NOBODY_USER) {
    #owner of rrdcached_default_file_dir is 'nobody' by default 
    #need to change owner to gmetad_user for proper gmetad service start
    
    hdp::directory { $rrdcached_default_file_dir:
      owner => $rrd_file_owner,
      group => $rrd_file_owner,
      override_owner => true
    }
    
    File[$rrd_py_file_path] -> Hdp::Directory[$rrdcached_default_file_dir] -> Anchor['hdp-ganglia::server::files::end']
  }
}


class hdp-ganglia::service::change_permission(
  $ensure
)
{
  if ($ensure == 'running' or $ensure == 'installed_and_configured') {
    hdp::directory_recursive_create { '/var/lib/ganglia/dwoo' :
      mode => '0777',
      owner => $hdp-ganglia::params::gmetad_user
    }
  }
}

class hdp-ganglia::server::gmetad(
  $ensure
)
{
  if ($ensure == 'running') {
    class { 'hdp-ganglia::server::delete_default_gmetad_process': }
    $command = "service hdp-gmetad start >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1"
   } elsif  ($ensure == 'stopped') {
    $command = "service hdp-gmetad stop >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1"
  }
  if ($ensure == 'running' or $ensure == 'stopped') {
    hdp::exec { "hdp-gmetad service" :
      command => "$command",
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    }
  }
}

class hdp-ganglia::server::delete_default_gmetad_process() {
  hdp::exec { "delete_default_gmetad_process" :
    command => "chkconfig gmetad off",
    path => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    require => Class['hdp-ganglia::server::gmetad']
  }
}
