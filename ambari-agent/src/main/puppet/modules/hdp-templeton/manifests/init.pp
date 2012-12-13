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
class hdp-templeton(
  $service_state = undef,
  $server = false
)
{
# Configs generation  

  if has_key($configuration, 'webhcat-site') {
    configgenerator::configfile{'webhcat-site': 
      modulespath => $hdp-templeton::params::conf_dir,
      filename => 'webhcat-site.xml',
      module => 'hdp-templeton',
      configuration => $configuration['webhcat-site']
    }
  }

 include hdp-templeton::params 
 
  if ($hdp::params::use_32_bits_on_slaves == false) {
    $size = 64
  } else {
    $size = 32
  }

  $templeton_user = $hdp-templeton::params::templeton_user
  $templeton_config_dir = $hdp-templeton::params::conf_dir

  if ($service_state == 'uninstalled') {
      hdp::package { 'webhcat' :
      size => $size,
      ensure => 'uninstalled'
    }
      hdp::directory { $templeton_config_dir:
        service_state => $service_state,
        force => true
      }

     anchor { 'hdp-templeton::begin': } -> Hdp::Package['webhcat'] -> Hdp::Directory[$templeton_config_dir] ->  anchor { 'hdp-templeton::end': }

  } else {
    hdp::package { 'webhcat' :
      size => $size
    }
    class { hdp-templeton::download-hive-tar: }
    class { hdp-templeton::download-pig-tar: }

    hdp::user{ $templeton_user:}

    hdp::directory { $templeton_config_dir: 
      service_state => $service_state,
      force => true
    }

    hdp-templeton::configfile { ['webhcat-env.sh']: }

    anchor { 'hdp-templeton::begin': } -> Hdp::Package['webhcat'] -> Hdp::User[$templeton_user] -> Hdp::Directory[$templeton_config_dir] -> Hdp-templeton::Configfile<||> ->  anchor { 'hdp-templeton::end': }

     if ($server == true ) { 
      Hdp::Package['webhcat'] -> Hdp::User[$templeton_user] ->   Class['hdp-templeton::download-hive-tar'] -> Class['hdp-templeton::download-pig-tar'] -> Anchor['hdp-templeton::end']
     }
  }
}

### config files
define hdp-templeton::configfile(
  $mode = undef
) 
{
  hdp::configfile { "${hdp-templeton::params::conf_dir}/${name}":
    component       => 'templeton',
    owner           => $hdp-templeton::params::templeton_user,
    mode            => $mode
  }
}

