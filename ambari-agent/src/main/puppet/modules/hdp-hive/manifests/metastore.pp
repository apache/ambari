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
class hdp-hive::metastore(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-hive::params
{ 

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 

    $hdp::params::service_exists['hdp-hive::server'] = true

    if ( ($service_state == 'installed_and_configured') and
         ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
      $masterHost = $kerberos_adminclient_host[0]
      hdp::download_keytab { 'hive_server_service_keytab' :
        masterhost => $masterHost,
        keytabdst => "${$keytab_path}/hive.service.keytab",
        keytabfile => 'hive.service.keytab',
        owner => $hdp-hive::params::hive_user
      }
    }

    #installs package, creates user, sets configuration
    class{ 'hdp-hive' : 
      service_state => $service_state,
      server        => true
    } 
  
    Hdp-Hive::Configfile<||>{hive_server_host => $hdp::params::host_address}

    class { 'hdp-hive::service' :
      ensure => $service_state,
      service_type => "metastore"
    }
  
    #top level does not need anchors
    Class['hdp-hive'] -> Class['hdp-hive::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
