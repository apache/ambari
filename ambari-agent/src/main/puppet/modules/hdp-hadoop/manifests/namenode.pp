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

    if ( ($service_state == 'installed_and_configured') and 
         ($security_enabled == true) and ($kerberos_install_type == "AMBARI_SET_KERBEROS") ) {
      $masterHost = $kerberos_adminclient_host[0]
      hdp::download_keytab { 'namenode_service_keytab' :
        masterhost => $masterHost,
        keytabdst => "${$keytab_path}/nn.service.keytab",
        keytabfile => 'nn.service.keytab',
        owner => $hdp-hadoop::params::hdfs_user
      }
      hdp::download_keytab { 'namenode_hdfs_headless_keytab' :   
        masterhost => $masterHost,
        keytabdst => "${$keytab_path}/hdfs.headless.keytab",
        keytabfile => 'hdfs.headless.keytab', 
        owner => $hdp-hadoop::params::hdfs_user, 
        hostnameInPrincipals => 'no'
      }
      hdp::download_keytab { 'namenode_spnego_keytab' :   
        masterhost => $masterHost,
        keytabdst => "${$keytab_path}/spnego.service.keytab",
        keytabfile => 'spnego.service.keytab', 
        owner => $hdp-hadoop::params::hdfs_user, 
        mode => '0440',
        group => 'hadoop'
      }
    }
 
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

    hdp-hadoop::namenode::create_app_directories { 'create_app_directories' :
       service_state => $service_state
    }

    #top level does not need anchors
    Class['hdp-hadoop'] ->  Hdp-hadoop::Service['namenode']
    Hdp-hadoop::Namenode::Create_name_dirs<||> -> Hdp-hadoop::Service['namenode'] 
    Hdp-hadoop::Service['namenode'] -> Hdp-hadoop::Namenode::Create_app_directories<||>
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

define hdp-hadoop::namenode::create_app_directories($service_state)
{
  if ($service_state == 'running') {
    $smoke_test_user = $hdp::params::smokeuser
    hdp-hadoop::hdfs::directory{ "/user/${smoke_test_user}":
      service_state => $service_state,
      owner => $smoke_test_user,
      mode  => '770',
      recursive_chmod => true
    }
   
    hdp-hadoop::hdfs::directory{ "/tmp" :
      service_state => $service_state,
      owner => $hdp-hadoop::params::hdfs_user,
      mode => '777'
    }

    hdp-hadoop::hdfs::directory{ '/mapred' :
      service_state => $service_state,
      owner         => $hdp-hadoop::params::mapred_user
    }
    hdp-hadoop::hdfs::directory{ '/mapred/system' :
      service_state => $service_state,
      owner         => $hdp-hadoop::params::mapred_user
    }
    Hdp-hadoop::Hdfs::Directory['/mapred'] -> Hdp-hadoop::Hdfs::Directory['/mapred/system']

    if ($hdp::params::hbase_master_host != "") {
      $hdfs_root_dir = $hdp::params::hbase_hdfs_root_dir
      hdp-hadoop::hdfs::directory { $hdfs_root_dir:
        owner         => $hdp::params::hbase_user,
        service_state => $service_state
      }
    }

    if ($hdp::params::hive_server_host != "") {
      $hive_user = $hdp::params::hive_user

      hdp-hadoop::hdfs::directory{ '/apps/hive/warehouse':
        service_state   => $service_state,
        owner            => $hive_user,
        mode             => '777',
        recursive_chmod  => true
      }
      hdp-hadoop::hdfs::directory{ "/user/${hive_user}":
        service_state => $service_state,
        owner         => $hive_user
      }
    }

    if ($hdp::params::oozie_server != "") {
      $oozie_user = $hdp::params::oozie_user
      hdp-hadoop::hdfs::directory{ "/user/${oozie_user}":
        service_state => $service_state,
        owner => $oozie_user,
        mode  => '775',
        recursive_chmod => true
      }
    }
    
    if ($hdp::params::webhcat_server_host != "") {
      $templeton_user = $hdp::params::templeton_user
      hdp-hadoop::hdfs::directory{ '/user/hcat':
        service_state => $service_state,
        owner => $templeton_user,
        mode  => '755',
        recursive_chmod => true
      }

      hdp-hadoop::hdfs::directory{ '/apps/webhcat':
        service_state => $service_state,
        owner => $templeton_user,
        mode  => '755',
        recursive_chmod => true
      }
    }
  }
}
