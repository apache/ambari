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
class hdp-hbase::regionserver(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hbase::params
{

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {    
    $hdp::params::service_exists['hdp-hbase::regionserver'] = true       

    if ( ($service_state == 'installed_and_configured') and
         ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
       $masterHost = $kerberos_adminclient_host[0]
       hdp::download_keytab { 'hbase_rs_service_keytab' :
         masterhost => $masterHost,
         keytabdst => "${$keytab_path}/rs.service.keytab",
         keytabfile => 'rs.service.keytab',
         owner => $hdp::params::hbase_user
       }
    }

    if ($hdp::params::service_exists['hdp-hbase::master'] != true) {
      #adds package, users, directories, and common configs
      class { 'hdp-hbase': 
        type          => 'regionserver',
        service_state => $service_state
      } 
      $create_pid_dir = true
      $create_conf_dir = true
    } else {
      $create_pid_dir = false
      $create_conf_dir = false
    }


    hdp-hbase::service{ 'regionserver':
      ensure         => $service_state,
      create_pid_dir => $create_pid_dir,
      create_conf_dir => $create_conf_dir
    }

    #top level does not need anchors
    Class['hdp-hbase'] ->  Hdp-hbase::Service['regionserver']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

#assumes that master and regionserver will not be on same machine
class hdp-hbase::regionserver::enable-ganglia()
{
  Hdp-hbase::Configfile<|title  == $metric-prop-file-name |>{template_tag => 'GANGLIA-RS'}
}
