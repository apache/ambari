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
class hdp-hive(
  $service_state,
  $server = false
) 
{
  include hdp-hive::params

  $hive_user = $hdp-hive::params::hive_user
  $hive_config_dir = $hdp-hive::params::hive_conf_dir

  anchor { 'hdp-hive::begin': }
  anchor { 'hdp-hive::end': } 

  if ($service_state == 'uninstalled') {
    hdp::package { 'hive' : 
      ensure => 'uninstalled'
    }

    hdp::directory { $hive_config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp-hive::begin'] -> Hdp::Package['hive'] -> Hdp::Directory[$hive_config_dir] ->  Anchor['hdp-hive::end']

  } else {
    hdp::package { 'hive' : }
    if ($server == true ) {
      class { 'hdp-hive::mysql-connector': }
    }
  
    hdp::user{ $hive_user:}
  
    hdp::directory { $hive_config_dir: 
      service_state => $service_state,
      force => true
    }

    hdp-hive::configfile { ['hive-env.sh','hive-site.xml']: }
  
    Anchor['hdp-hive::begin'] -> Hdp::Package['hive'] -> Hdp::User[$hive_user] ->  
     Hdp::Directory[$hive_config_dir] -> Hdp-hive::Configfile<||> ->  Anchor['hdp-hive::end']

     if ($server == true ) {
       Hdp::Package['hive'] -> Hdp::User[$hive_user] -> Class['hdp-hive::mysql-connector'] -> Anchor['hdp-hive::end']
    }
  }
}

### config files
define hdp-hive::configfile(
  $mode = undef,
  $hive_server_host = undef
) 
{
  hdp::configfile { "${hdp-hive::params::hive_conf_dir}/${name}":
    component        => 'hive',
    owner            => $hdp-hive::params::hive_user,
    mode             => $mode,
    hive_server_host => $hive_server_host 
  }
}
