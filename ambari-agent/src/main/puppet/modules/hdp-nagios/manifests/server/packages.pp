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

  hdp::package { 'perl':
    ensure      => present,
    java_needed => false
  }

  hdp::package { 'perl-Net-SNMP':
    ensure      => present,
    java_needed => false
  }

  hdp::package { 'nagios-plugins': 
    ensure      => present,
    java_needed => false
  }
  
  hdp::package { 'nagios-server':
    ensure      => present,
    java_needed => false
  }

  hdp::package { 'nagios-devel': 
    ensure      => present,
    java_needed => false
  }
  
  hdp::package { 'nagios-fping': 
    ensure      => present,
    java_needed => false
  }
  
  hdp::package { 'nagios-addons': 
    ensure      => present,
    java_needed => false
  }
  
  hdp::package { 'nagios-php-pecl-json': 
    ensure      => present,
    java_needed => false
  }
  
  
debug("## state: $service_state")
  if ($service_state == 'installed_and_configured') {
    
    hdp::package::remove_pkg { 'hdp_mon_nagios_addons':
      package_type => 'hdp_mon_nagios_addons'
    }

    hdp::package::remove_pkg { 'nagios-plugins':
      package_type => 'nagios-plugins'
    }

    exec { "remove_package nagios":
      path    => "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
      command => "rpm -e --allmatches --nopostun nagios ; true"
    }

    debug("##Adding removing dep")
    # Removing conflicting packages. Names of packages being removed are hardcoded and not resolved via hdp::params
    Hdp::Package::Remove_pkg['hdp_mon_nagios_addons'] -> Hdp::Package::Remove_pkg['nagios-plugins'] -> Exec['remove_package nagios'] -> Hdp::Package['nagios-plugins']
  }

  Hdp::Package['nagios-plugins'] -> Hdp::Package['nagios-server'] -> Hdp::Package['nagios-devel'] -> Hdp::Package['nagios-fping'] -> Hdp::Package['nagios-addons'] -> Hdp::Package['nagios-php-pecl-json']
    

} 

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
