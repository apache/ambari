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
class hdp-hue::server(
  $service_state = $hdp::params::cluster_service_state,
  $setup = false,
  $opts = {}
) inherits  hdp-hue::params
{
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $hdp::params::service_exists['hdp-hue::server'] = true

    #installs package, creates user, sets configuration
    class{ 'hdp-hue' :
      service_state => $service_state,
    }

    Hdp-hue::Generate_config_file<||>{ config_file_path => $hdp-hue::params::hue_conf_file }

    class { 'hdp-hue::service' :
      ensure => $service_state
    }

    #top level does not need anchors
    Class['hdp-hue'] -> Class['hdp-hue::service']
    } else {
      hdp_fail("TODO not implemented yet: service_state = ${service_state}")
    }
}
