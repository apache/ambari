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
class hdp-pig(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp-pig::params
{  
  $pig_config_dir = $hdp-pig::params::pig_conf_dir
 
  if ($hdp::params::use_32_bits_on_slaves == false) {
    $size = 64
  } else {
    $size = 32
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp::package { 'pig' :
      ensure => 'uninstalled',
      size   => $size
    }
    hdp::directory_recursive_create { $pig_config_dir:
      service_state => $service_state,
      force => true
    }
   anchor { 'hdp-pig::begin': } -> Hdp::Package['pig'] -> Hdp::Directory_recursive_create[$pig_conf_dir] -> anchor { 'hdp-pig::end': }

  } elsif ($service_state == 'installed_and_configured') {
    hdp::package { 'pig' : 
      size => $size
    }

    hdp::directory { $pig_config_dir:
      service_state => $service_state,
      force => true,
      owner => $hdp::params::hdfs_user,
      group => $hdp::params::user_group,
      override_owner => true
    }

    hdp-pig::configfile { ['pig-env.sh','pig.properties','log4j.properties']:}
  
    anchor { 'hdp-pig::begin': } -> Hdp::Package['pig'] -> Hdp::Directory[$pig_conf_dir] -> Hdp-pig::Configfile<||> -> anchor { 'hdp-pig::end': }
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp-pig::configfile()
{
  hdp::configfile { "${hdp::params::pig_conf_dir}/${name}":
    component => 'pig',
    owner => $hdp::params::hdfs_user
  }
}



