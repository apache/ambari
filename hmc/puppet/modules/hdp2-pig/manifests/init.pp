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
class hdp2-pig(
  $service_state = $hdp2::params::cluster_client_state
) inherits hdp2-pig::params
{  
  $pig_config_dir = $hdp2-pig::params::pig_conf_dir
 
  if ($hdp2::params::use_32_bits_on_slaves == false) {
    $size = 64
  } else {
    $size = 32
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp2::package { 'pig' :
      ensure => 'uninstalled',
      size   => $size 
    }
    hdp2::directory_recursive_create { $pig_config_dir:
      service_state => $service_state,
      force => true
    }
   anchor { 'hdp2-pig::begin': } -> Hdp2::Package['pig'] -> Hdp2::Directory_recursive_create[$pig_conf_dir] -> anchor { 'hdp2-pig::end': }

  } elsif ($service_state == 'installed_and_configured') {
    hdp2::package { 'pig' : 
      size => $size 
    }

    hdp2::directory { $pig_config_dir:
      service_state => $service_state,
      force => true
    }

    hdp2-pig::configfile { ['pig-env.sh','pig.properties','log4j.properties']:}
  
    anchor { 'hdp2-pig::begin': } -> Hdp2::Package['pig'] -> Hdp2::Directory[$pig_conf_dir] -> Hdp2-pig::Configfile<||> -> anchor { 'hdp2-pig::end': }
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp2-pig::configfile()
{
  hdp2::configfile { "${hdp2::params::pig_conf_dir}/${name}":
    component => 'pig'
  }
}



