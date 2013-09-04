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
class hdp-yarn::nodemanager(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-yarn::params
{
  $yarn_user = $hdp-yarn::params::yarn_user
  $nm_local_dirs = $hdp-yarn::params::nm_local_dirs
  $nm_log_dirs = $hdp-yarn::params::nm_log_dirs
  $yarn_log_dir_prefix = $hdp-yarn::params::yarn_log_dir_prefix

  if ($service_state == 'no_op') {
  } elsif ($service_state in 'installed_and_configured') {
  
    include hdp-yarn::initialize

    ##Process package
    hdp-yarn::package{'yarn-nodemanager':}

    hdp::configfile {"${hdp::params::limits_conf_dir}/mapreduce.conf":
      component => 'yarn',
      owner => 'root',
      group => 'root',
      mode => 644,
      require => Hdp-yarn::Package['yarn-nodemanager']
    }

  } elsif ($service_state in ['running','stopped']) {

    include hdp-yarn::initialize

    # To avoid duplicate resource definitions
    $nm_dirs = hdp_set_from_comma_list("${nm_local_dirs},${nm_log_dirs}", "$yarn_log_dir_prefix")

    hdp-yarn::nodemanager::create_nm_dirs { $nm_dirs:
      service_state => $service_state
    }

    hdp-yarn::service{ 'nodemanager':
      ensure       => $service_state,
      user         => $yarn_user
    }

    anchor{"hdp-yarn::nodemanager::begin" : } ->
    Hdp-yarn::Nodemanager::Create_nm_dirs<||> ->
    Hdp-yarn::Service['nodemanager'] ->
    anchor{"hdp-yarn::nodemanager::end": }

  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}


define hdp-yarn::nodemanager::create_nm_dirs($service_state) {
  $dirs = hdp_array_from_comma_list($name)
  hdp::directory_recursive_create_ignore_failure { $dirs :
    owner => $hdp-yarn::params::yarn_user,
    context_tag => 'yarn_service',
    service_state => $service_state,
    force => true
  }
}
