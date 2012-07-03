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
class hdp-hadoop::namenode(
  $service_state = $hdp::params::cluster_service_state,
  $slave_hosts = [],
  $format = true,
  $opts = {}
) inherits hdp-hadoop::params
{
  $hdp::params::service_exists['hdp-hadoop::namenode'] = true

  Hdp-hadoop::Common<||>{service_states +> $service_state}
  Hdp-hadoop::Package<||>{include_64_bit => true}
  Hdp-hadoop::Configfile<||>{sizes +> 64}

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 
    $dfs_name_dir = $hdp-hadoop::params::dfs_name_dir
  
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize
 
    hdp-hadoop::namenode::create_name_dirs { $dfs_name_dir: 
      service_state => $service_state
    }
   
    Hdp-Hadoop::Configfile<||>{namenode_host => $hdp::params::host_address}
    Hdp::Configfile<||>{namenode_host => $hdp::params::host_address} #for components other than hadoop (e.g., hbase) 
  
    if ($service_state == 'running' and $format == true) {
      class {'hdp-hadoop::namenode::format' : }
    }

    hdp-hadoop::service{ 'namenode':
      ensure       => $service_state,
      user         => $hdp-hadoop::params::hdfs_user,
      initial_wait => hdp_option_value($opts,'wait')
    }
    #top level does not need anchors
    Class['hdp-hadoop'] ->  Hdp-hadoop::Service['namenode']
    Hdp-hadoop::Namenode::Create_name_dirs<||> -> Hdp-hadoop::Service['namenode']
    if ($service_state == 'running' and $format == true) {
      Class['hdp-hadoop'] -> Class['hdp-hadoop::namenode::format'] -> Hdp-hadoop::Service['namenode']
      Hdp-hadoop::Namenode::Create_name_dirs<||> -> Class['hdp-hadoop::namenode::format']
    } 
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

define hdp-hadoop::namenode::create_name_dirs($service_state)
{
  $dirs = hdp_array_from_comma_list($name)
  hdp::directory_recursive_create { $dirs :
    owner => $hdp-hadoop::params::hdfs_user,
    mode => '0755',
    service_state => $service_state,
    force => true
  }
}
