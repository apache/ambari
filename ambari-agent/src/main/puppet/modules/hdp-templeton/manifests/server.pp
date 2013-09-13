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
class hdp-templeton::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-templeton::params
{  

  $templeton_user = $hdp-templeton::params::templeton_user
  if ($service_state == 'no_op') { 
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
  $hdp::params::service_exists['hdp-templeton::server'] = true

  if ( ($service_state == 'installed_and_configured') and
       ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
     $masterHost = $kerberos_adminclient_host[0]
     hdp::download_keytab { 'templeton_headless_keytab' :
       masterhost => $masterHost,
       keytabdst => "${$keytab_path}/templeton.headless.keytab",
       keytabfile => 'templeton.headless.keytab',
       owner => $hdp::params::templeton_user,
       hostnameInPrincipals => 'no' 
     }

     if ( ($hdp::params::service_exists['hdp-hadoop::namenode'] != true) and
          ($hdp::params::service_exists['hdp-hadoop::snamenode'] != true) and
          ($hdp::params::service_exists['hdp-oozie::server'] != true) ) {
       hdp::download_keytab { 'templeton_spnego_service_keytab' :
         masterhost => $masterHost,
         keytabdst => "${$keytab_path}/spnego.service.keytab",
         keytabfile => 'spnego.service.keytab',
         owner => $hdp::params::templeton_user,
         group => $hdp::params::user_group,
         mode => '0440'
       }
     }
  }

  class{ 'hdp-templeton' :
    service_state => $service_state,
    server        => true
  }

  class { 'hdp-templeton::copy-hdfs-directories' :
    service_state => $service_state
  }

  class { 'hdp-templeton::service' :
    ensure       => $service_state,
  }
  
  #top level does not need anchors
  Class['hdp-templeton'] -> Class['hdp-templeton::copy-hdfs-directories'] -> Class['hdp-templeton::service']
  } else { 
  hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-templeton::copy-hdfs-directories($service_state)
{
 $webhcat_apps_dir = $hdp::params::webhcat_apps_dir
 $webhcat_user = $hdp::params::webhcat_user
 $smoke_test_user = $hdp::params::smokeuser
 $smokeuser_keytab = $hdp::params::smokeuser_keytab
 if ($hdp::params::security_enabled == true) {
     $kinit_if_needed = "${hdp::params::kinit_path_local} -kt ${smokeuser_keytab} ${smoke_test_user};"
   } else {
     $kinit_if_needed = "echo 0;"
   }

  anchor{ "hdp::hdp-templeton::copy-hdfs-directories::begin" : }
  anchor{ "hdp::hdp-templeton::copy-hdfs-directories::end" : }

  $kinit_cmd = "su - ${webhcat_user} -c '${kinit_if_needed}'"
  exec { $kinit_cmd:
    command => $kinit_cmd,
    path => ['/bin']
  }

  if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
    hdp-hadoop::hdfs::copyfromlocal { '/usr/lib/hadoop-mapreduce/hadoop-streaming*.jar':
      service_state => $service_state,
      owner => $webhcat_user,
      mode  => '755',
      dest_dir => "$webhcat_apps_dir/hadoop-streaming.jar",
      kinit_if_needed => $kinit_if_needed
    }
  }
  else {
    hdp-hadoop::hdfs::copyfromlocal { '/usr/lib/hadoop/contrib/streaming/hadoop-streaming*.jar':
      service_state => $service_state,
      owner => $webhcat_user,
      mode  => '755',
      dest_dir => "$webhcat_apps_dir/hadoop-streaming.jar",
      kinit_if_needed => $kinit_if_needed
    }
  }
  hdp-hadoop::hdfs::copyfromlocal { '/usr/share/HDP-webhcat/pig.tar.gz' :
    service_state => $service_state,
    owner => $webhcat_user,
    mode  => '755',
    dest_dir => "$webhcat_apps_dir/pig.tar.gz",
  }
  hdp-hadoop::hdfs::copyfromlocal { '/usr/share/HDP-webhcat/hive.tar.gz' :
    service_state => $service_state,
    owner => $webhcat_user,
    mode  => '755',
    dest_dir => "$webhcat_apps_dir/hive.tar.gz",
  }
  Anchor["hdp::hdp-templeton::copy-hdfs-directories::begin"] ->
  Exec[$kinit_cmd] ->
  Hdp-hadoop::Hdfs::Copyfromlocal<||>  ->
  Anchor["hdp::hdp-templeton::copy-hdfs-directories::end"]
}
