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
class hdp-hadoop::snamenode(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hadoop::params  
{
  $hdp::params::service_exists['hdp-hadoop::snamenode'] = true

  Hdp-hadoop::Common<||>{service_state => $service_state}
  Hdp-hadoop::Package<||>{include_64_bit => true}
  Hdp-hadoop::Configfile<||>{sizes +> 64}

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $fs_checkpoint_dir = $hdp-hadoop::params::fs_checkpoint_dir
  
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize

    if ( ($service_state == 'installed_and_configured') and
         ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
      if ($hdp::params::service_exists['hdp-hadoop::namenode'] != true) {
        $masterHost = $kerberos_adminclient_host[0]
        hdp::download_keytab { 'snamenode_service_keytab' :
          masterhost => $masterHost,
          keytabdst => "${$keytab_path}/nn.service.keytab",
          keytabfile => 'nn.service.keytab',
          owner => $hdp-hadoop::params::hdfs_user
        }
        hdp::download_keytab { 'snamenode_spnego_keytab' :   
          masterhost => $masterHost,
          keytabdst => "${$keytab_path}/spnego.service.keytab",
          keytabfile => 'spnego.service.keytab', 
          owner => $hdp-hadoop::params::hdfs_user,
          mode => '0440',
          group => $hdp::params::user_group
        }
      }
    }
 
    Hdp-Hadoop::Configfile<||>{snamenode_host => $hdp::params::host_address}
  
    hdp-hadoop::snamenode::create_name_dirs { $fs_checkpoint_dir: 
      service_state => $service_state
    }
    
    if ($hdp::params::service_exists['hdp-hadoop::namenode'] == true) {
      $create_pid_dir = false
      $create_log_dir = false
    } else {
      $create_pid_dir = true
      $create_log_dir = true
    }
    
    hdp-hadoop::service{ 'secondarynamenode':
      ensure         => $service_state,
      user           => $hdp-hadoop::params::hdfs_user,
      create_pid_dir => $create_pid_dir,
      create_log_dir => $create_log_dir
    }
  
    #top level does not need anchors
    Anchor['hdp-hadoop::begin'] -> Hdp-hadoop::Namenode::Create_name_dirs<||> ->
      Hdp-hadoop::Service['secondarynamenode'] -> Anchor['hdp-hadoop::end']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

define hdp-hadoop::snamenode::create_name_dirs($service_state)
{
   $dirs = hdp_array_from_comma_list($name)
   hdp::directory_recursive_create { $dirs :
     owner => $hdp-hadoop::params::hdfs_user,
     mode => '0755',
     service_state => $service_state,
     force => true
  }
}
