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
class hdp-dashboard(
  $service_state = $hdp::params::cluster_client_state,
  $opts = {}
) inherits hdp-dashboard::params
{
   if ($service_state == 'no_op') {
   } elsif ($service_state == 'uninstalled') {
    hdp::package { 'dashboard' :
      ensure => 'uninstalled',
      java_needed => 'false',
      size   => 64
    }
    hdp::directory_recursive_create { $conf_dir :
      service_state => $service_state,
      force => true
    }

    Hdp::Package['dashboard'] -> Hdp::Directory_recursive_create[$conf_dir]

   } elsif ($service_state in ['running','installed_and_configured','stopped']) {
      hdp::package { 'dashboard' :
        java_needed => 'false',
        size => 64
       }
     $conf_dir =  $hdp-dashboard::params::conf_dir
  
     hdp::directory_recursive_create { $conf_dir :
       service_state => $service_state,
       force => true
     }
 
     hdp-dashboard::configfile { 'cluster_configuration.json' : }
     Hdp-Dashboard::Configfile<||>{dashboard_host => $hdp::params::host_address}
  
     #top level does not need anchors
     Hdp::Package['dashboard'] -> Hdp::Directory_recursive_create[$conf_dir] -> Hdp-Dashboard::Configfile<||> 
    } else {
     hdp_fail("TODO not implemented yet: service_state = ${service_state}")
   }
}

###config file helper
define hdp-dashboard::configfile(
  $dashboard_host = undef
)
{
  
  hdp::configfile { "${hdp-dashboard::params::conf_dir}/${name}":
    component      => 'dashboard',
    owner          => root,
    group          => root,
    dashboard_host => $dashboard_host
  }
}


