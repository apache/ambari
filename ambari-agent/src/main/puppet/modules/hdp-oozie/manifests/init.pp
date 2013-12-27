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

  $oozie_user = $hdp-oozie::params::oozie_user
  $oozie_config_dir = $hdp-oozie::params::conf_dir

  if has_key($configuration, 'oozie-site') {
    configgenerator::configfile{'oozie-site':
      modulespath => $oozie_config_dir, 
      filename => 'oozie-site.xml',
      module => 'hdp-oozie',
      configuration => $configuration['oozie-site'],
      owner => $oozie_user,
      group => $hdp::params::user_group,
      mode => '0664'
    }
  } else {
    file { "${oozie_config_dir}/oozie-site.xml":
      owner => $oozie_user,
      group => $hdp::params::user_group,
      mode => '0664'
    }
  }

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

     

     hdp::directory { $oozie_config_dir: 
       service_state => $service_state,
       force => true,
       owner => $oozie_user,
       group => $hdp::params::user_group,
       override_owner => true
     }

     hdp-oozie::configfile { 'oozie-env.sh': }

     if ($service_state == 'installed_and_configured') {
       hdp-oozie::configfile { 'oozie-log4j.properties': }

       if ($hdp::params::oozie_jdbc_driver == "com.mysql.jdbc.Driver" or $hdp::params::oozie_jdbc_driver == "oracle.jdbc.driver.OracleDriver") {
         hdp::exec { "download DBConnectorVerification.jar" :
           command => "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf --retry 5 ${hdp::params::jdk_location}${hdp::params::check_db_connection_jar_name} -o ${hdp::params::check_db_connection_jar_name}'",
           unless  => "[ -f ${check_db_connection_jar} ]"
         }
       }
     }

     hdp-oozie::ownership { 'ownership': }

    anchor { 'hdp-oozie::begin': } -> Hdp::Package['oozie-client'] -> Hdp::Directory[$oozie_config_dir] -> Hdp-oozie::Configfile<||> -> Hdp-oozie::Ownership['ownership'] -> anchor { 'hdp-oozie::end': }

     if ($server == true ) { 
       Hdp::Package['oozie-server'] -> Hdp::Package['oozie-client'] -> Class['hdp-oozie::download-ext-zip'] ->  Anchor['hdp-oozie::end']
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

define hdp-oozie::ownership {
  file { "${hdp-oozie::params::conf_dir}/adminusers.txt":
    owner => $hdp-oozie::params::oozie_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-oozie::params::conf_dir}/hadoop-config.xml":
    owner => $hdp-oozie::params::oozie_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-oozie::params::conf_dir}/oozie-default.xml":
    owner => $hdp-oozie::params::oozie_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-oozie::params::conf_dir}/action-conf":
    owner => $hdp-oozie::params::oozie_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-oozie::params::conf_dir}/action-conf/hive.xml":
    owner => $hdp-oozie::params::oozie_user,
    group => $hdp::params::user_group
  }
}
