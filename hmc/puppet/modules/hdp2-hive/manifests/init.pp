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
class hdp2-hive(
  $service_state,
  $server = false
) 
{
  include hdp2-hive::params

  $hive_user = $hdp2-hive::params::hive_user
  $hive_config_dir = $hdp2-hive::params::hive_conf_dir

  anchor { 'hdp2-hive::begin': }
  anchor { 'hdp2-hive::end': } 

  if ($service_state == 'uninstalled') {
    hdp2::package { 'hive' : 
      ensure => 'uninstalled'
    }

    hdp2::directory { $hive_config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp2-hive::begin'] -> Hdp2::Package['hive'] -> Hdp2::Directory[$hive_config_dir] ->  Anchor['hdp2-hive::end']

  } else {
    hdp2::package { 'hive' : }
    if ($server == true ) {
      class { 'hdp2-hive::mysql-connector': }
    }
  
    hdp2::user{ $hive_user:}
  
    hdp2::directory { $hive_config_dir: 
      service_state => $service_state,
      force => true
    }

    hdp2-hive::configfile { ['hive-env.sh','hive-site.xml']: }
  
    Anchor['hdp2-hive::begin'] -> Hdp2::Package['hive'] -> Hdp2::User[$hive_user] ->  
     Hdp2::Directory[$hive_config_dir] -> Hdp2-hive::Configfile<||> ->  Anchor['hdp2-hive::end']

     if ($server == true ) {
       Hdp2::Package['hive'] -> Hdp2::User[$hive_user] -> Class['hdp2-hive::mysql-connector'] -> Anchor['hdp2-hive::end']
    }
  }
}

### config files
define hdp2-hive::configfile(
  $mode = undef,
  $hive_server_host = undef
) 
{
  hdp2::configfile { "${hdp2-hive::params::hive_conf_dir}/${name}":
    component        => 'hive',
    owner            => $hdp2-hive::params::hive_user,
    mode             => $mode,
    hive_server_host => $hive_server_host 
  }
}
