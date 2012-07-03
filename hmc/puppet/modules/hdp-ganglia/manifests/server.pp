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

  #top level does not need anchors
  Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] -> 
    Hdp-ganglia::Config::Generate_server<||> -> Class['hdp-ganglia::server::gmetad'] -> Class['hdp-ganglia::service::change_permission']
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
      unless => "/bin/ps auwx | /bin/grep [g]metad",
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    }
  }
}
