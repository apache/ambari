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
class hdp-hadoop::client(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp-hadoop::params
{
  $hdp::params::service_exists['hdp-hadoop::client'] = true

  Hdp-hadoop::Common<||>{service_state => $service_state}

  if ($hdp::params::use_32_bits_on_slaves == true) {
    Hdp-hadoop::Package<||>{include_32_bit => true}
    Hdp-hadoop::Configfile<||>{sizes +> 32}
  } else {
    Hdp-hadoop::Package<||>{include_64_bit => true}
    Hdp-hadoop::Configfile<||>{sizes +> 64}
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['installed_and_configured','uninstalled']) {
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize

    if ( ($service_state == 'installed_and_configured') and
         ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
      $masterHost = $kerberos_adminclient_host[0]
      hdp::download_keytab { 'hadoop_client_ambari_qa_keytab' :
        masterhost => $masterHost,
        keytabdst => "${$keytab_path}/${smokeuser}.headless.keytab",
        keytabfile => "${smokeuser}.headless.keytab",
        owner => $smokeuser,
        hostnameInPrincipals => 'no'
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
