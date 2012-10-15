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
class hdp2-nagios::server(
  $service_state = $hdp2::params::cluster_service_state
) inherits hdp2-nagios::params
{

  $nagios_config_dir = $hdp2-nagios::params::conf_dir
  $plugins_dir = $hdp2-nagios::params::plugins_dir
  $nagios_obj_dir = $hdp2-nagios::params::nagios_obj_dir

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
    class { 'hdp2-nagios::server::packages' : 
      service_state => uninstalled
    }

    hdp2::exec { "rm -f /var/nagios/rw/nagios.cmd" :
      command => "rm -f /var/nagios/rw/nagios.cmd",
      unless => "test ! -e  /var/nagios/rw/nagios.cmd"
    }

    hdp2::exec { "rm -rf /tmp/hadoop-nagios" :
      command => "rm -rf /tmp/hadoop-nagios",
      unless => "test ! -e  /tmp/hadoop-nagios"
    }

    hdp2::directory { $nagios_config_dir:
      service_state => $service_state,
      force => true
    }

    hdp2::directory { $plugins_dir:
      service_state => $service_state,
      force => true
    }

    hdp2::directory { $nagios_obj_dir:
      service_state => $service_state,
      force => true
    }

     Class['hdp2-nagios::server::packages'] -> Exec['rm -f /var/nagios/rw/nagios.cmd'] -> Exec['rm -rf /tmp/hadoop-nagios'] -> Hdp2::Directory[$nagios_config_dir] -> Hdp2::Directory[$plugins_dir] -> Hdp2::Directory[$nagios_obj_dir]

  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    class { 'hdp2-nagios::server::packages' : }

    hdp2::directory { $nagios_config_dir:
      service_state => $service_state,
      force => true
    }

    hdp2::directory { $plugins_dir:
      service_state => $service_state,
      force => true
    }

    hdp2::directory { $nagios_obj_dir:
      service_state => $service_state,
      force => true
    }


    class { 'hdp2-nagios::server::config': 
      notify => Class['hdp2-nagios::server::services']
    }

    class { 'hdp2-nagios::server::web_permisssions': }

    class { 'hdp2-nagios::server::services': ensure => $service_state}

    Class['hdp2-nagios::server::packages'] -> Hdp2::Directory[$nagios_config_dir] -> Hdp2::Directory[$plugins_dir] -> Hdp2::Directory[$nagios_obj_dir] -> Class['hdp2-nagios::server::config'] -> 
    Class['hdp2-nagios::server::web_permisssions'] -> Class['hdp2-nagios::server::services']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp2-nagios::server::web_permisssions()
{
  $web_login = $hdp2-nagios::params::nagios_web_login
  $cmd = "htpasswd -c -b  /etc/nagios/htpasswd.users ${web_login} ${hdp2-nagios::params::nagios_web_password}"
  $test = "grep ${web_user} /etc/nagios/htpasswd.users"
  hdp2::exec { $cmd :
    command => $cmd,
    unless => $test
  }
}

class hdp2-nagios::server::services($ensure)
{
  if ($ensure in ['running','stopped']) {
    service { 'nagios': ensure => $ensure}
    anchor{'hdp2-nagios::server::services::begin':} ->  Service['nagios'] ->  anchor{'hdp2-nagios::server::services::end':}
  }
}
