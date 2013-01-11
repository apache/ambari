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
      ensure => 'uninstalled'
   }

   class { 'hdp-ganglia::server::files':
      ensure => 'absent'
   }

  } else {
  class { 'hdp-ganglia':
    service_state => $service_state
  }

  class { 'hdp-ganglia::server::packages': }

  class { 'hdp-ganglia::config': 
    ganglia_server_host => $hdp::params::host_address,
    service_state       => $service_state 
  }

  hdp-ganglia::config::generate_server { ['HDPHBaseMaster','HDPJobTracker','HDPNameNode','HDPSlaves']:
    ganglia_service => 'gmond',
    role => 'server'
  }
  hdp-ganglia::config::generate_server { 'gmetad':
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

  #top level does not need anchors
  Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] ->
 Hdp-ganglia::Config::Generate_server<||> ->
 Class['hdp-ganglia::server::gmetad'] -> Class['hdp-ganglia::service::change_permission'] -> Class['hdp-ganglia::server::files'] -> Class['hdp-monitor-webserver']
 }
}

class hdp-ganglia::server::packages(
  $ensure = present 
)
{
  hdp::package { ['ganglia-server','ganglia-gweb','ganglia-hdp-gweb-addons']: 
    ensure      => $ensure,
    java_needed => false  
  }

  hdp::package { ['rrdtool']:
        ensure      => 'absent',
        java_needed => false,
        before => Hdp::Package ['rrdtool-python']
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
    ensure => "directory"  
  }

  $rrd_py_file_path = "${rrd_py_path}/rrd.py"

  file{$rrd_py_file_path :
    ensure => $ensure,
    source => "puppet:///modules/hdp-ganglia/rrd.py",
    mode   => '0755',
    require => Hdp::Directory_recursive_create[$rrd_py_path]
  }
}


class hdp-ganglia::service::change_permission(
  $ensure
)
{
  if ($ensure == 'running' or $ensure == 'installed_and_configured') {
    hdp::directory_recursive_create { '/var/lib/ganglia/dwoo' :
      mode => '0777'
      }
  }
}

class hdp-ganglia::server::gmetad(
  $ensure
)
{
  if ($ensure == 'running') {
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
