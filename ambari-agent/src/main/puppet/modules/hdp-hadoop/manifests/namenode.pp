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

  Hdp-hadoop::Common<||>{service_state => $service_state}
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
        group => $hdp::params::user_group
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

    hdp-hadoop::namenode::create_user_directories { 'create_user_directories' :
      service_state => $service_state
    }

    Anchor['hdp-hadoop::begin'] ->
    Hdp-hadoop::Namenode::Create_name_dirs<||> ->
    Hdp-hadoop::Service['namenode'] ->
    Hdp-hadoop::Namenode::Create_app_directories<||> ->
    Hdp-hadoop::Namenode::Create_user_directories<||> ->
    Anchor['hdp-hadoop::end']

    if ($service_state == 'running' and $format == true) {
      Anchor['hdp-hadoop::begin'] ->
      Hdp-hadoop::Namenode::Create_name_dirs<||> ->
      Class['hdp-hadoop::namenode::format'] ->
      Hdp-hadoop::Service['namenode'] ->
      Anchor['hdp-hadoop::end']
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

    if ($hdp::params::hbase_master_hosts != "") {

      hdp-hadoop::hdfs::directory { $hdp-hadoop::params::hdfs_root_dir:
        owner         => $hdp::params::hbase_user,
        service_state => $service_state
      }

      $hbase_staging_dir = $hdp::params::hbase_staging_dir
      hdp-hadoop::hdfs::directory { $hbase_staging_dir:
        owner         => $hdp::params::hbase_user,
        service_state => $service_state,
        mode             => '711'
      }
    }

    if ($hdp::params::hive_server_host != "") {
      $hive_user = $hdp::params::hive_user
      $hive_apps_whs_dir = $hdp::params::hive_apps_whs_dir

      hdp-hadoop::hdfs::directory{ $hive_apps_whs_dir:
        service_state   => $service_state,
        owner            => $hive_user,
        mode             => '777',
        recursive_chmod  => true
      }
    }

    if ($hdp::params::webhcat_server_host != "") {
      $webhcat_user = $hdp::params::webhcat_user
      $webhcat_apps_dir = $hdp::params::webhcat_apps_dir

      hdp-hadoop::hdfs::directory{ $webhcat_apps_dir:
        service_state => $service_state,
        owner => $webhcat_user,
        mode  => '755',
        recursive_chmod => true
      }
    }

    if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
      if ($hdp::params::nm_hosts != "") {
        if ($hdp::params::yarn_log_aggregation_enabled == "true") {
          $yarn_user = $hdp::params::yarn_user
          $yarn_nm_app_log_dir = $hdp::params::yarn_nm_app_log_dir

          hdp-hadoop::hdfs::directory{ $yarn_nm_app_log_dir:
            service_state => $service_state,
            owner => $yarn_user,
            group => $hdp::params::user_group,
            mode  => '1777',
            recursive_chmod => true
          }
        }
      }


      if ($hdp::params::hs_host != "") {
        $mapred_user = $hdp::params::mapred_user
        $mapreduce_jobhistory_intermediate_done_dir = $hdp::params::mapreduce_jobhistory_intermediate_done_dir
        $group = $hdp::params::user_group
        $mapreduce_jobhistory_done_dir = $hdp::params::mapreduce_jobhistory_done_dir

        hdp-hadoop::hdfs::directory{ $mapreduce_jobhistory_intermediate_done_dir:
          service_state => $service_state,
          owner => $mapred_user,
          group => $group,
          mode  => '1777'
        }

        hdp-hadoop::hdfs::directory{ $mapreduce_jobhistory_done_dir:
          service_state => $service_state,
          owner => $mapred_user,
          group => $group,
          mode  => '1777'
        }
      }
    }
  }
}


define hdp-hadoop::namenode::create_user_directories($service_state)
{
  if ($service_state == 'running') {
    $smoke_hdfs_user_dir = $hdp::params::smoke_hdfs_user_dir

    $smoke_user_dir_item="$smoke_hdfs_user_dir,"

    if ($hdp::params::hive_server_host != "") {
      $hive_hdfs_user_dir = $hdp::params::hive_hdfs_user_dir
      $hive_dir_item="$hive_hdfs_user_dir,"
    } else {
      $hive_dir_item=""
    }

    if ($hdp::params::oozie_server != "") {
      $oozie_hdfs_user_dir = $hdp::params::oozie_hdfs_user_dir
      $oozie_dir_item="$oozie_hdfs_user_dir,"
    } else {
      $oozie_dir_item=""
    }
    
    if ($hdp::params::webhcat_server_host != "") {
      $hcat_hdfs_user_dir = $hdp::params::hcat_hdfs_user_dir
      $webhcat_hdfs_user_dir = $hdp::params::webhcat_hdfs_user_dir
      $webhcat_dir_item="$webhcat_hdfs_user_dir,"
      if ($hcat_hdfs_user_dir != webhcat_hdfs_user_dir) {
        $hcat_dir_item="$hcat_hdfs_user_dir,"
      } else {
        $hcat_dir_item=""
      }
    } else {
      $webhcat_dir_item=""
    }

    $users_dir_list_comm_sep = "$smoke_user_dir_item $hive_dir_item $oozie_dir_item $hcat_dir_item $webhcat_dir_item"

    #Get unique users directories set
    $users_dirs_set = hdp_set_from_comma_list($users_dir_list_comm_sep)

    hdp-hadoop::namenode::create_user_directory{ $users_dirs_set:
      service_state => $service_state
    }
  }
  
}

define hdp-hadoop::namenode::create_user_directory($service_state)
{
  
  $owner = hdp_hadoop_get_owner($name)
  $mode = hdp_hadoop_get_mode($name)
  debug("## Creating user directory: $name, owner: $owner, mode: $mode")
  hdp-hadoop::hdfs::directory{ $name:
   service_state   => $service_state,
   mode            => $mode,
   owner           => $owner,
   recursive_chmod => true
  }
}

