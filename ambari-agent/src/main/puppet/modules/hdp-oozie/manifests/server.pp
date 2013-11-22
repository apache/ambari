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
class hdp-oozie::server(
  $service_state = $hdp::params::cluster_service_state,
  $setup = false,
  $opts = {}
) inherits  hdp-oozie::params
{   
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $hdp::params::service_exists['hdp-oozie::server'] = true

    if ( ($service_state == 'installed_and_configured') and
         ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
       $masterHost = $kerberos_adminclient_host[0]
       hdp::download_keytab { 'oozie_service_keytab' :
         masterhost => $masterHost,
         keytabdst => "${$keytab_path}/oozie.service.keytab",
         keytabfile => 'oozie.service.keytab',
         owner => $hdp::params::oozie_user
       }

       if ( ($hdp::params::service_exists['hdp-hadoop::namenode'] != true) and
            ($hdp::params::service_exists['hdp-hadoop::snamenode'] != true) ) {
         hdp::download_keytab { 'oozie_spnego_keytab' :
           masterhost => $masterHost,
           keytabdst => "${$keytab_path}/spnego.service.keytab",
           keytabfile => 'spnego.service.keytab',
           owner => $hdp::params::oozie_user,
           group => $hdp::params::user_group,
           mode => '0440'
         }
      }
    }

    #installs package, creates user, sets configuration
    class{ 'hdp-oozie' : 
      service_state => $service_state,
      server        => true
    } 
  
    Hdp-Oozie::Configfile<||>{oozie_server => $hdp::params::oozie_server}

    class { 'hdp-oozie::service' :
      ensure       => $service_state,
      setup         => $setup
    }
  
    #top level does not need anchors
    Class['hdp-oozie'] -> Class['hdp-oozie::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
