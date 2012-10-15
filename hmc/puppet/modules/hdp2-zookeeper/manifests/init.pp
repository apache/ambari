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
class hdp2-zookeeper(
  $type = server,
  $service_state = $hdp2::params::cluster_service_state,
  $myid = 1,
  $opts = {}
) inherits hdp2-zookeeper::params 
{

 if ($service_state == 'no_op') {
   if ($type == 'server') {
     $hdp2::params::service_exists['hdp2-zookeeper'] = true
  }
 } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 
   $zk_user = $hdp2-zookeeper::params::zk_user
   $zk_config_dir = $hdp2-zookeeper::params::conf_dir
 
   anchor{'hdp2-zookeeper::begin':}
   anchor{'hdp2-zookeeper::end':}

   if ($service_state == 'uninstalled') {
     if ($type == 'server') {
       $hdp2::params::service_exists['hdp2-zookeeper'] = true
    }
     hdp2::package { 'zookeeper':
       ensure => 'uninstalled'
     }
     hdp2::directory_recursive_create { $zk_config_dir:
       service_state => $service_state,
       force => true
     }

     if ($type == 'server') {
        class { 'hdp2-zookeeper::service':
          ensure => $service_state,
          myid   => $myid
        }
       }

     if ($type == 'server') {
       Anchor['hdp2-zookeeper::begin'] -> Hdp2::Package['zookeeper'] -> Hdp2::Directory_recursive_create[$zk_config_dir] -> Class['hdp2-zookeeper::service']  -> Anchor['hdp2-zookeeper::end']
     } else {
       Anchor['hdp2-zookeeper::begin'] -> Hdp2::Package['zookeeper'] -> Hdp2::Directory_recursive_create[$zk_config_dir] -> Anchor['hdp2-zookeeper::end']
     }
   } else {
     hdp2::package { 'zookeeper':}

     hdp2::user{ $zk_user:}

     hdp2::directory_recursive_create { $zk_config_dir: 
      service_state => $service_state,
      force => true
     }

     hdp2-zookeeper::configfile { ['zoo.cfg','zookeeper-env.sh','configuration.xsl','log4j.properties']: }
 
     if ($hdp2::params::update_zk_shell_files == true) {
       hdp2-zookeeper::shell_file{ ['zkServer.sh','zkEnv.sh']: }
     }

     if ($type == 'server') {
       $hdp2::params::service_exists['hdp2-zookeeper'] = true
       class { 'hdp2-zookeeper::service': 
         ensure => $service_state,
         myid   => $myid
       }
      }

      Anchor['hdp2-zookeeper::begin'] -> Hdp2::Package['zookeeper'] -> Hdp2::User[$zk_user] -> 
        Hdp2::Directory_recursive_create[$zk_config_dir] -> Hdp2-zookeeper::Configfile<||> -> Anchor['hdp2-zookeeper::end']
      if ($type == 'server') {
        Hdp2::Directory_recursive_create[$zk_config_dir] -> Hdp2-zookeeper::Configfile<||> -> Class['hdp2-zookeeper::service'] -> Anchor['hdp2-zookeeper::end']
      }
      if ($hdp2::params::update_zk_shell_files == true) {
        Hdp2::Package['zookeeper'] -> Hdp2-zookeeper::Shell_file<||> -> Anchor['hdp2-zookeeper::end']
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp2-zookeeper::configfile(
  $mode = undef
) 
{
  hdp2::configfile { "${hdp2-zookeeper::params::conf_dir}/${name}":
    component       => 'zookeeper',
    owner           => $hdp2-zookeeper::params::zk_user,
    mode            => $mode
  }
}

### 
define hdp2-zookeeper::shell_file()
{
  file { "${hdp2::params::zk_bin}/${name}":
    source => "puppet:///modules/hdp2-zookeeper/${name}", 
    mode => '0755'
  }
}
