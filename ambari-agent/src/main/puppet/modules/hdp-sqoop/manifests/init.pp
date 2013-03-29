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
class hdp-sqoop(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp-sqoop::params
{
  if ($hdp::params::use_32_bits_on_slaves == false) {
    $size = 64
  } else {
    $size = 32
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp::package { 'sqoop' :
      ensure => 'uninstalled',
      size   => $size
    }
  } elsif ($service_state == 'installed_and_configured') {

    hdp::package { 'sqoop' :
      size => $size
    }
    class { 'hdp-sqoop::mysql-connector': }
    if ($package_type == 'hdp') {
      hdp-sqoop::createsymlinks { ['/usr/lib/sqoop/conf']:}
    }

    hdp::directory { $conf_dir:
      service_state => $service_state,
      force => true,
      owner => $sqoop_user,
      group => $hdp::params::user_group,
      override_owner => true
    }

    hdp-sqoop::configfile { ['sqoop-env.sh']:}

    hdp-sqoop::ownership { 'ownership': }

    anchor { 'hdp-sqoop::begin': } -> Hdp::Package['sqoop'] -> Class['hdp-sqoop::mysql-connector'] -> Hdp::Directory[$conf_dir] -> Hdp-sqoop::Configfile<||> -> Hdp-sqoop::Ownership['ownership'] -> anchor { 'hdp-sqoop::end': }
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}


define hdp-sqoop::createsymlinks()
{
  file { '/usr/lib/sqoop/conf' :
    #ensure => directory,
    ensure => link,
    target => "/etc/sqoop"
  }

  file { '/etc/default/hadoop' :
    ensure => link,
    target => "/usr/bin/hadoop"
  }
}

### config files
define hdp-sqoop::configfile()
{
  hdp::configfile { "${hdp::params::sqoop_conf_dir}/${name}":
    component => 'sqoop',
    owner     => $hdp::params::sqoop_user
  }
}

define hdp-sqoop::ownership {
  file { "${hdp::params::sqoop_conf_dir}/sqoop-env-template.sh":
    owner => $hdp::params::sqoop_user,
    group => $hdp::params::user_group
  }

  file { "${hdp::params::sqoop_conf_dir}/sqoop-site-template.xml":
    owner => $hdp::params::sqoop_user,
    group => $hdp::params::user_group
  }

  file { "${hdp::params::sqoop_conf_dir}/sqoop-site.xml":
    owner => $hdp::params::sqoop_user,
    group => $hdp::params::user_group
  }
}
