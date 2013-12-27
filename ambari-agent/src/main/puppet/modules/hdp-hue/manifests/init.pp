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
class hdp-hue(
  $service_state = undef
)
{
  include $hdp-hue::params

  $hue_user = $hdp-hue::params::hue_server_user
  $hue_conf_dir = $hdp::params::hue_conf_dir

  if ($service_state == 'uninstalled') {

    hdp::package { 'hue-server':
      ensure => 'uninstalled'
    }

    hdp::directory { $hue_config_dir:
      service_state => $service_state,
      force => true
    }

  } else {
    ## Install package
    hdp::package { 'hue-server': }

    ## Create user
    hdp::user{ 'hue_user':
      user_name => $hue_user
    }

    ## Create dir
    hdp::directory_recursive_create { $hue_conf_dir:
      service_state => $service_state,
      force => true,
      owner => $hue_user
    }

    # Configs generation
    if has_key($configuration, 'hue-site') {
      hdp-hue::generate_config_file { 'hue-ini':
        config_file_path => $hdp-hue::params::hue_conf_file
      }
    }

    anchor { 'hdp-hue::begin': } -> Hdp::Package['hue-server'] ->  Hdp::User['hue_user'] -> Hdp::Directory_recursive_create[$hue_conf_dir] -> Hdp-Hue::Generate_config_file<||> -> anchor { 'hdp-hue::end': }

  }
}

define hdp-hue::generate_config_file(
  $config_file_path
)
{
  if (hdp_is_empty($configuration) == false and
    hdp_is_empty($configuration['hue-site']) == false)
  {
    ## Create hue.ini file
    file { $config_file_path :
      ensure => file,
      content => template('hdp-hue/hue-ini.cfg.erb'),
      owner => $hdp-hue::params::hue_server_user
    }
  }
}