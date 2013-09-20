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
class hdp-hadoop::tasktracker(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hadoop::params
{
  $hdp::params::service_exists['hdp-hadoop::tasktracker'] = true

  Hdp-hadoop::Common<||>{service_state => $service_state}

  if ($hdp::params::use_32_bits_on_slaves == true) {
    Hdp-hadoop::Package<||>{include_32_bit => true}
    Hdp-hadoop::Configfile<||>{sizes +> 32}
  } else {
    Hdp-hadoop::Package<||>{include_64_bit => true}
    Hdp-hadoop::Configfile<||>{sizes +> 64}
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 
    $mapred_local_dir = $hdp-hadoop::params::mapred_local_dir
  
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize

    if ( ($service_state == 'installed_and_configured') and
         ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
      $masterHost = $kerberos_adminclient_host[0]
      hdp::download_keytab { 'tasktracker_service_keytab' :
        masterhost => $masterHost,
        keytabdst => "${$keytab_path}/tt.service.keytab",
        keytabfile => 'tt.service.keytab',
        owner => $hdp-hadoop::params::mapred_user
      }
    }

    hdp-hadoop::tasktracker::create_local_dirs { $mapred_local_dir:
      service_state => $service_state
    }
    
    if ($hdp::params::service_exists['hdp-hadoop::jobtracker'] == true) {
      $create_pid_dir = false
      $create_log_dir = false
    } else {
      $create_pid_dir = true
      $create_log_dir = true
    }

    hdp-hadoop::service{ 'tasktracker':
      ensure => $service_state,
      user   => $hdp-hadoop::params::mapred_user,
      create_pid_dir => $create_pid_dir,
      create_log_dir => $create_log_dir
    }
  
    #top level does not need anchors
    Anchor['hdp-hadoop::begin'] -> Hdp-hadoop::Tasktracker::Create_local_dirs<||> -> Hdp-hadoop::Service['tasktracker'] ->
    Anchor['hdp-hadoop::end']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

define hdp-hadoop::tasktracker::create_local_dirs($service_state)
{
  if ($hdp::params::service_exists['hdp-hadoop::jobtracker'] != true) {
    $dirs = hdp_array_from_comma_list($name)
    hdp::directory_recursive_create_ignore_failure { $dirs :
      owner => $hdp-hadoop::params::mapred_user,
      mode => '0755',
      service_state => $service_state,
      force => true
    }
  }
}
