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
class hdp-ganglia::monitor_and_server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-ganglia::params
{
  $ganglia_shell_cmds_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir
  $ganglia_conf_dir = $hdp-ganglia::params::ganglia_conf_dir
  $ganglia_runtime_dir = $hdp-ganglia::params::ganglia_runtime_dir

  #note: includes the common package ganglia-monitor
  class { 'hdp-ganglia':
    service_state => $service_state
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
    class { 'hdp-ganglia::server::packages':
      ensure => 'uninstalled'
      }

    hdp::directory { [$ganglia_conf_dir,$ganglia_runtime_dir]:
      service_state => $service_state,
      force => true
    }
    
    class { 'hdp-ganglia::config':
      service_state => $service_state
    }

    Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> 
      Hdp::Directory[$ganglia_conf_dir] -> Hdp::Directory[$ganglia_runtime_dir] ->
      Class['hdp-ganglia::config']
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    class { 'hdp-ganglia::server::packages': }

    class { 'hdp-ganglia::config': 
     ganglia_server_host => $hdp::params::host_address,
     service_state       => $service_state
     }

    class {'hdp-ganglia::monitor::config-gen': }      
    
    
    hdp-ganglia::config::generate_daemon { 'gmetad':
      ganglia_service => 'gmetad'
    }

    class { 'hdp-ganglia::service::change_permission':
      ensure => $service_state
    }

    #top level no anchors needed
    Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] -> 
      Class['hdp-ganglia::monitor::config-gen'] -> Hdp-ganglia::Config::Generate_daemon['gmetad'] ->
      Class['hdp-ganglia::service::change_permission']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
