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
  if ($server == true) {
    $hive_config_dir = $hdp-hive::params::hive_server_conf_dir
    $config_file_mode = '0600'
  } else {
    $hive_config_dir = $hdp-hive::params::hive_conf_dir
    $config_file_mode = '0644'
  }

  # Configs generation
  if has_key($configuration, 'hive-site') {
    configgenerator::configfile{'hive-site':
      modulespath => $hive_config_dir, 
      filename => 'hive-site.xml',
      module => 'hdp-hive',
      configuration => $configuration['hive-site'],
      owner => $hive_user,
      group => $hdp::params::user_group,
      mode => $config_file_mode
    }
  } else {
    file { "${hive_config_dir}/hive-site.xml":
      owner => $hive_user,
      group => $hdp::params::user_group,
      mode => $config_file_mode
    }
  }

  anchor { 'hdp-hive::begin': }
  anchor { 'hdp-hive::end': }

  if ($service_state == 'installed_and_configured' and ($hive_jdbc_driver == "com.mysql.jdbc.Driver" or $hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver")) {
    hdp::exec { "download DBConnectorVerification.jar" :
      command => "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf --retry 5 ${hdp::params::jdk_location}${hdp::params::check_db_connection_jar_name} -o ${hdp::params::check_db_connection_jar_name}'",
      unless  => "[ -f ${check_db_connection_jar} ]"
    }
  }

  if ($service_state == 'uninstalled') {
    hdp::package { 'hive' : 
      ensure => 'uninstalled'
    }

    hdp::directory_recursive_create { $hive_config_dir:
      service_state => $service_state,
      ensure => "directory",
      force => true
    }

    Anchor['hdp-hive::begin'] -> Hdp::Package['hive'] -> Hdp::Directory_recursive_create[$hive_config_dir] ->  Anchor['hdp-hive::end']

  } else {
    hdp::package { 'hive' : }
    if ($server == true ) {
      class { 'hdp-hive::jdbc-connector': }
    }

    hdp::directory_recursive_create { $hive_config_dir:
      service_state => $service_state,
      force => true,
      owner => $hive_user,
      group => $hdp::params::user_group,
      ensure => "directory",
      override_owner => true
    }

    hdp-hive::configfile { 'hive-env.sh': config_dir => $hive_config_dir }

    hdp-hive::ownership { 'ownership': config_dir => $hive_config_dir }
  
    Anchor['hdp-hive::begin'] -> Hdp::Package['hive'] -> 
     Hdp::Directory_recursive_create[$hive_config_dir] -> Hdp-hive::Configfile<||> -> Hdp-hive::Ownership['ownership'] -> Anchor['hdp-hive::end']

     if ($server == true ) {
       Hdp::Package['hive'] -> Class['hdp-hive::jdbc-connector'] -> Anchor['hdp-hive::end']
    }
  }
}

### config files
define hdp-hive::configfile(
  $mode = undef,
  $hive_server_host = undef,
  $config_dir = $hdp-hive::params::hive_conf_dir
) 
{
  hdp::configfile { "${config_dir}/${name}":
    component        => 'hive',
    owner            => $hdp-hive::params::hive_user,
    mode             => $mode,
    hive_server_host => $hive_server_host 
  }
}

define hdp-hive::ownership(
  $config_dir = $hdp-hive::params::hive_conf_dir
)
{
  file { "${config_dir}/hive-default.xml.template":
    owner => $hdp-hive::params::hive_user,
    group => $hdp::params::user_group
  }

  file { "${config_dir}/hive-env.sh.template":
    owner => $hdp-hive::params::hive_user,
    group => $hdp::params::user_group
  }

  file { "${config_dir}/hive-exec-log4j.properties.template":
    owner => $hdp-hive::params::hive_user,
    group => $hdp::params::user_group
  }

  file { "${config_dir}/hive-log4j.properties.template":
    owner => $hdp-hive::params::hive_user,
    group => $hdp::params::user_group
  }
}
