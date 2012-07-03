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
class hdp-nagios::server::packages(
  $service_state = $hdp::params::cluster_service_state
)
{
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
     hdp-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons']:
      ensure => 'uninstalled'
    }
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    case $hdp::params::hdp_os_type {
      centos6, rhel6: {
        hdp-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons']:
          ensure => 'present'
        }
      }
      default: {
        hdp-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons','nagios-php-pecl-json']:
          ensure => 'present'
        }
      }
    }
  } 
  Hdp-nagios::Server::Package['nagios-plugins'] -> Hdp::Package['nagios-addons'] #other order produces package conflict

  anchor{'hdp-nagios::server::packages::begin':} -> Hdp-nagios::Server::Package<||> -> anchor{'hdp-nagios::server::packages::end':}
  Anchor['hdp-nagios::server::packages::begin'] -> Hdp::Package['nagios-addons'] -> Anchor['hdp-nagios::server::packages::end']
  Hdp-nagios::Server::Package['nagios-fping'] -> Hdp-nagios::Server::Package['nagios-plugins']
}


define hdp-nagios::server::package(
  $ensure = present
)
{
  hdp::package { $name: 
    ensure      => $ensure,
    java_needed => false
  }
}
