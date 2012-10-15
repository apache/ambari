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
class hdp2-nagios::server::packages(
  $service_state = $hdp2::params::cluster_service_state
)
{
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
      hdp2-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons']:
      ensure => 'uninstalled'
    }
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    case $hdp2::params::hdp_os_type {
      centos6, rhel6: {
        hdp2-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons']:
          ensure => 'present'
        }
      }
      default: {
        hdp2-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons','nagios-php-pecl-json']:
          ensure => 'present'
        }
      }
    }
  }

  Hdp2-nagios::Server::Package['nagios-plugins'] -> Hdp2::Package['nagios-addons'] #other order produces package conflict

  anchor{'hdp2-nagios::server::packages::begin':} -> Hdp2-nagios::Server::Package<||> -> anchor{'hdp2-nagios::server::packages::end':}
  Anchor['hdp2-nagios::server::packages::begin'] -> Hdp2::Package['nagios-addons'] -> Anchor['hdp2-nagios::server::packages::end']
  Hdp2-nagios::Server::Package['nagios-fping'] -> Hdp2-nagios::Server::Package['nagios-plugins']
}


define hdp2-nagios::server::package(
  $ensure = present
)
{
  hdp2::package { $name: 
    ensure      => $ensure,
    java_needed => false
  }
}
