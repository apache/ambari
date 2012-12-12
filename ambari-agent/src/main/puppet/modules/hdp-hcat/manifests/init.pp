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
class hdp-hcat(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp-hcat::params
{
  $hcat_config_dir = $hdp-hcat::params::hcat_conf_dir

  if ($hdp::params::use_32_bits_on_slaves == false) {
    $size = 64
  } else {
    $size = 32
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp::package { 'hcat' :
      ensure => 'uninstalled', 
      size   => $size
    }

    hdp::directory { $hcat_config_dir:
      service_state => $service_state,
      force => true
    }

    Hdp::Package['hcat'] -> Hdp::Directory[$hcat_config_dir]

  } elsif ($service_state == 'installed_and_configured') {
    hdp::package { 'hcat' : 
      size => $size
    }

    hdp::directory { $hcat_config_dir:
      service_state => $service_state,
      force => true
    }

    hdp-hcat::configfile { 'hcat-env.sh':}
  
    Hdp::Package['hcat'] -> Hdp::Directory[$hcat_config_dir] -> Hdp-hcat::Configfile<||> 
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp-hcat::configfile()
{
  hdp::configfile { "${hdp::params::hcat_conf_dir}/${name}":
    component => 'hcat'
  }
}
