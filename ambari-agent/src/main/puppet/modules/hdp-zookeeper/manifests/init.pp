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
class hdp-zookeeper(
  $type = server,
  $service_state = $hdp::params::cluster_service_state,
  $myid = 1,
  $opts = {}
) inherits hdp-zookeeper::params 
{

 if ($service_state == 'no_op') {
   if ($type == 'server') {
     $hdp::params::service_exists['hdp-zookeeper'] = true
  }
 } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 
   $zk_user = $hdp-zookeeper::params::zk_user
   $zk_config_dir = $hdp-zookeeper::params::conf_dir
 
   anchor{'hdp-zookeeper::begin':}
   anchor{'hdp-zookeeper::end':}

   if ($service_state == 'uninstalled') {
     if ($type == 'server') {
       $hdp::params::service_exists['hdp-zookeeper'] = true
    }
     hdp::package { 'zookeeper':
       ensure => 'uninstalled'
     }
     hdp::directory_recursive_create { $zk_config_dir:
       service_state => $service_state,
       force => true
     }

     if ($type == 'server') {
        class { 'hdp-zookeeper::service':
          ensure => $service_state,
          myid   => $myid
        }
       }

     if ($type == 'server') {
       Anchor['hdp-zookeeper::begin'] -> Hdp::Package['zookeeper'] -> Hdp::Directory_recursive_create[$zk_config_dir] -> Class['hdp-zookeeper::service']  -> Anchor['hdp-zookeeper::end']
     } else {
       Anchor['hdp-zookeeper::begin'] -> Hdp::Package['zookeeper'] -> Hdp::Directory_recursive_create[$zk_config_dir] -> Anchor['hdp-zookeeper::end']
     }
   } else {
     hdp::package { 'zookeeper':}

     hdp::user{ 'zk_user':
       user_name => $zk_user
     }

     hdp::directory_recursive_create { $zk_config_dir: 
      service_state => $service_state,
      force => true,
      owner => $zk_user
     }

     hdp-zookeeper::configfile { ['zoo.cfg','zookeeper-env.sh','configuration.xsl']: }

     if ($service_state == 'installed_and_configured') {
       hdp-zookeeper::configfile { 'log4j.properties': }
     }
 
     if ($hdp::params::update_zk_shell_files == true) {
       hdp-zookeeper::shell_file{ ['zkServer.sh','zkEnv.sh']: }
     }

     if ($type == 'server') {
       $hdp::params::service_exists['hdp-zookeeper'] = true
       class { 'hdp-zookeeper::service': 
         ensure => $service_state,
         myid   => $myid
       }
      }

      if ($security_enabled == true) {
        if ($type == 'server') {
          hdp-zookeeper::configfile { 'zookeeper_jaas.conf' : }
          hdp-zookeeper::configfile { 'zookeeper_client_jaas.conf' : }
        } else {
          hdp-zookeeper::configfile { 'zookeeper_client_jaas.conf' : }
        }
      }

     file { "${zk_config_dir}/zoo_sample.cfg":
       owner => $zk_user,
       group => $hdp::params::user_group
     }

      Anchor['hdp-zookeeper::begin'] -> Hdp::Package['zookeeper'] -> Hdp::User['zk_user'] -> 
        Hdp::Directory_recursive_create[$zk_config_dir] -> Hdp-zookeeper::Configfile<||> -> File["${zk_config_dir}/zoo_sample.cfg"] -> Anchor['hdp-zookeeper::end']
      if ($type == 'server') {
        Hdp::Directory_recursive_create[$zk_config_dir] -> Hdp-zookeeper::Configfile<||> -> Class['hdp-zookeeper::service'] -> Anchor['hdp-zookeeper::end']
      }
      if ($hdp::params::update_zk_shell_files == true) {
        Hdp::Package['zookeeper'] -> Hdp-zookeeper::Shell_file<||> -> Anchor['hdp-zookeeper::end']
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp-zookeeper::configfile(
  $mode = undef
) 
{
  hdp::configfile { "${hdp-zookeeper::params::conf_dir}/${name}":
    component       => 'zookeeper',
    owner           => $hdp-zookeeper::params::zk_user,
    mode            => $mode
  }
}

### 
define hdp-zookeeper::shell_file()
{
  file { "${hdp::params::zk_bin}/${name}":
    source => "puppet:///modules/hdp-zookeeper/${name}", 
    mode => '0755'
  }
}
