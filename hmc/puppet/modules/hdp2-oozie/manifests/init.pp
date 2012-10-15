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
class hdp2-oozie(
  $service_state = undef,
  $server = false,
  $setup = false
)
{
  include hdp2-oozie::params 
 
  $oozie_user = $hdp2-oozie::params::oozie_user
  $oozie_config_dir = $hdp2-oozie::params::conf_dir
  
  if ($service_state == 'uninstalled') {
    hdp2::package { 'oozie-client' : 
      ensure => 'uninstalled'
    }
    if ($server == true ) {
      hdp2::package { 'oozie-server' :
        ensure => 'uninstalled'
      }
    }
    hdp2::directory { $oozie_config_dir:
      service_state => $service_state,
      force => true
    }

    anchor { 'hdp2-oozie::begin': } -> Hdp2::Package['oozie-client'] -> Hdp2::Directory[$oozie_config_dir] ->  anchor { 'hdp2-oozie::end': }

    if ($server == true ) {
       Hdp2::Package['oozie-server'] -> Hdp2::Package['oozie-client'] ->  Anchor['hdp2-oozie::end']
     }
  } else {
    hdp2::package { 'oozie-client' : }
    if ($server == true ) {
      hdp2::package { 'oozie-server':}
      class { 'hdp2-oozie::download-ext-zip': }
    }

     hdp2::user{ $oozie_user:}

     hdp2::directory { $oozie_config_dir: 
      service_state => $service_state,
      force => true
    }

     hdp2-oozie::configfile { ['oozie-site.xml','oozie-env.sh','oozie-log4j.properties']: }

    anchor { 'hdp2-oozie::begin': } -> Hdp2::Package['oozie-client'] -> Hdp2::User[$oozie_user] -> Hdp2::Directory[$oozie_config_dir] -> Hdp2-oozie::Configfile<||> -> anchor { 'hdp2-oozie::end': }

     if ($server == true ) { 
       Hdp2::Package['oozie-server'] -> Hdp2::Package['oozie-client'] -> Hdp2::User[$oozie_user] ->   Class['hdp2-oozie::download-ext-zip'] ->  Anchor['hdp2-oozie::end']
     }
 }
}

### config files
define hdp2-oozie::configfile(
  $mode = undef,
  $oozie_server = undef
) 
{
  hdp2::configfile { "${hdp2-oozie::params::conf_dir}/${name}":
    component       => 'oozie',
    owner           => $hdp2-oozie::params::oozie_user,
    mode            => $mode,
    oozie_server    => $oozie_server
  }
}
