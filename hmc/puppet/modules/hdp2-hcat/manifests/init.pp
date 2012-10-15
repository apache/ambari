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
class hdp2-hcat(
  $service_state = $hdp2::params::cluster_client_state
) inherits hdp2-hcat::params
{
  $hcat_config_dir = $hdp2-hcat::params::hcat_conf_dir
   

  if ($hdp2::params::use_32_bits_on_slaves == false) {
    $size = 64
  } else {
    $size = 32
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp2::package { 'hcat' :
      ensure => 'uninstalled', 
      size   => $size 
    }

    hdp2::directory { $hcat_config_dir:
      service_state => $service_state,
      force => true
    }

    Hdp2::Package['hcat'] -> Hdp2::Directory[$hcat_config_dir]

  } elsif ($service_state == 'installed_and_configured') {
    hdp2::package { 'hcat' : 
      size => $size 
    }

    hdp2::directory { $hcat_config_dir:
      service_state => $service_state,
      force => true
    }

    hdp2-hcat::configfile { 'hcat-env.sh':}
  
    Hdp2::Package['hcat'] -> Hdp2::Directory[$hcat_config_dir] -> Hdp-hcat::Configfile<||> 
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp2-hcat::configfile()
{
  hdp2::configfile { "${hdp2::params::hcat_conf_dir}/${name}":
    component => 'hcat'
  }
}
