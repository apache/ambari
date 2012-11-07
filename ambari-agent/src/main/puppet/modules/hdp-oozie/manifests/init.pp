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
class hdp-oozie(
  $service_state = undef,
  $server = false,
  $setup = false
)
{
  include hdp-oozie::params 

# Configs generation  

  if has_key($configuration, 'oozie-site') {
    configgenerator::configfile{'oozie-site':
      modulespath => $hdp-oozie::params::conf_dir, 
      filename => 'oozie-site.xml',
      module => 'hdp-oozie',
      configuration => $configuration['oozie-site']
    }
  }

  $oozie_user = $hdp-oozie::params::oozie_user
  $oozie_config_dir = $hdp-oozie::params::conf_dir
  
  if ($service_state == 'uninstalled') {
    hdp::package { 'oozie-client' : 
      ensure => 'uninstalled'
    }
    if ($server == true ) {
      hdp::package { 'oozie-server' :
        ensure => 'uninstalled'
      }
    }
    hdp::directory { $oozie_config_dir:
      service_state => $service_state,
      force => true
    }

    anchor { 'hdp-oozie::begin': } -> Hdp::Package['oozie-client'] -> Hdp::Directory[$oozie_config_dir] ->  anchor { 'hdp-oozie::end': }

    if ($server == true ) {
       Hdp::Package['oozie-server'] -> Hdp::Package['oozie-client'] ->  Anchor['hdp-oozie::end']
     }
  } else {
    hdp::package { 'oozie-client' : }
    if ($server == true ) {
      hdp::package { 'oozie-server':}
      class { 'hdp-oozie::download-ext-zip': }
    }

     hdp::user{ $oozie_user:}

     hdp::directory { $oozie_config_dir: 
      service_state => $service_state,
      force => true
    }

     hdp-oozie::configfile { ['oozie-env.sh','oozie-log4j.properties']: }

    anchor { 'hdp-oozie::begin': } -> Hdp::Package['oozie-client'] -> Hdp::User[$oozie_user] -> Hdp::Directory[$oozie_config_dir] -> Hdp-oozie::Configfile<||> -> anchor { 'hdp-oozie::end': }

     if ($server == true ) { 
       Hdp::Package['oozie-server'] -> Hdp::Package['oozie-client'] -> Hdp::User[$oozie_user] ->   Class['hdp-oozie::download-ext-zip'] ->  Anchor['hdp-oozie::end']
     }
 }
}

### config files
define hdp-oozie::configfile(
  $mode = undef,
  $oozie_server = undef
) 
{
  hdp::configfile { "${hdp-oozie::params::conf_dir}/${name}":
    component       => 'oozie',
    owner           => $hdp-oozie::params::oozie_user,
    mode            => $mode,
    oozie_server    => $oozie_server
  }
}
