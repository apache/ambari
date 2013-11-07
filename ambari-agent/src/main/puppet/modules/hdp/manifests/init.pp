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
class hdp(
  $service_state = undef,
  $pre_installed_pkgs = undef
)
{

  import 'params.pp'
  include hdp::params

  Exec { logoutput => 'on_failure' }
  
  hdp::group { 'hdp_user_group':
    group_name => $hdp::params::user_group,
  }

 ## Port settings
  if has_key($configuration, 'hdfs-site') {
    $hdfs-site = $configuration['hdfs-site']

    if (hdp_get_major_stack_version($stack_version) >= 2) {
      $namenode_port = hdp_get_port_from_url($hdfs-site["dfs.namenode.http-address"])
      $snamenode_port = hdp_get_port_from_url($hdfs-site["dfs.namenode.secondary.http-address"])
    } else {
      $namenode_port = hdp_get_port_from_url($hdfs-site["dfs.http.address"])
      $snamenode_port = hdp_get_port_from_url($hdfs-site["dfs.secondary.http.address"])
    }

    $datanode_port = hdp_get_port_from_url($hdfs-site["dfs.datanode.http.address"])
    $journalnode_port = hdp_get_port_from_url($hdfs-site["dfs.journalnode.http-address"])
  } else {
    $namenode_port = "50070"
    $snamenode_port = "50090"
    $datanode_port = "50075"
    $journalnode_port = "8480"
  }

  if has_key($configuration, 'mapred-site') {
    $mapred-site = $configuration['mapred-site']

    if (hdp_get_major_stack_version($stack_version) >= 2) {
      $jtnode_port = hdp_get_port_from_url($mapred-site["mapreduce.jobtracker.http.address"],"50030")
      $tasktracker_port = hdp_get_port_from_url($mapred-site["mapreduce.tasktracker.http.address"],"50060")
    } else {
      $jtnode_port = hdp_get_port_from_url($mapred-site["mapred.job.tracker.http.address"],"50030")
      $tasktracker_port = hdp_get_port_from_url($mapred-site["mapred.task.tracker.http.address"],"50060")
    }
    $jobhistory_port = hdp_get_port_from_url($mapred-site["mapreduce.history.server.http.address"],"51111")

    $hs_port = hdp_get_port_from_url($mapred-site["mapreduce.jobhistory.webapp.address"],"19888")
  }

  if has_key($configuration, 'yarn-site') {
    $yarn-site = $configuration['yarn-site']
    $rm_port = hdp_get_port_from_url($yarn-site["yarn.resourcemanager.webapp.address"],"8088")
    $rm_https_port = hdp_get_port_from_url($yarn-site["yarn.resourcemanager.webapp.https.address"],"8090")
    $nm_port = hdp_get_port_from_url($yarn-site["yarn.nodemanager.webapp.address"],"8042")
  }

  $hbase_master_port = hdp_default("hbase-site/hbase.master.info.port","60010")
  $hbase_rs_port = hdp_default("hbase-site/hbase.regionserver.info.port","60030")
  
  $ganglia_port = hdp_default("ganglia_port","8651")
  $ganglia_collector_slaves_port = hdp_default("ganglia_collector_slaves_port","8660")
  $ganglia_collector_namenode_port = hdp_default("ganglia_collector_namenode_port","8661")
  $ganglia_collector_jobtracker_port = hdp_default("ganglia_collector_jobtracker_port","8662")
  $ganglia_collector_hbase_port = hdp_default("ganglia_collector_hbase_port","8663")
  $ganglia_collector_rm_port = hdp_default("ganglia_collector_rm_port","8664")
  $ganglia_collector_nm_port = hdp_default("ganglia_collector_nm_port","8660")
  $ganglia_collector_hs_port = hdp_default("ganglia_collector_hs_port","8666")

  $oozie_server_port = hdp_default("oozie_server_port","11000")

  $templeton_port = hdp_default("webhcat-site/templeton.port","50111")

  $namenode_metadata_port = hdp_default("namenode_metadata_port","8020")

  $changeUid_path = "/tmp/changeUid.sh"

  file { $changeUid_path :
    ensure => present,
    source => "puppet:///modules/hdp/changeToSecureUid.sh",
    mode => '0755'
  }
  
  #TODO: think not needed and also there seems to be a puppet bug around this and ldap
  class { 'hdp::snmp': service_state => 'running'}

  class { 'hdp::create_smoke_user': }

  if ($pre_installed_pkgs != undef) {
    class { 'hdp::pre_install_pkgs': }
  }

  #turns off selinux
  class { 'hdp::set_selinux': }

  if ($service_state != 'uninstalled') {
    if ($hdp::params::lzo_enabled == true) {
      @hdp::lzo::package{ 32:}
      @hdp::lzo::package{ 64:}
    }
    if ($hdp::params::security_enabled) {
      hdp::package{ 'unzip':
        ensure       => 'present',
        size         => $size,
        java_needed  => false,
        lzo_needed   => false
      }
    }
  }

  #TODO: treat consistently 
  if ($service_state != 'uninstalled') {
    if ($hdp::params::snappy_enabled == true) {
      include hdp::snappy::package
    }
  }

  Hdp::Package<|title == 'hadoop 32'|> ->   Hdp::Package<|title == 'hbase'|>
  Hdp::Package<|title == 'hadoop 64'|> ->   Hdp::Package<|title == 'hbase'|>

  hdp::package{ 'glibc':
    ensure       => 'present',
    size         => $size,
    java_needed  => false,
    lzo_needed   => false
  }

    anchor{'hdp::begin':}
    anchor{'hdp::end':}

    ##Create all users for all components presents in cluster
    if ($hdp::params::hbase_master_hosts != "") {
      class { 'hdp::create_hbase_user': }
    }
    
    if ($hdp::params::nagios_server_host != "") {
	    hdp::group { 'nagios_group':
	      group_name => $hdp::params::nagios_group,
	    }

      hdp::user{ 'nagios_user':
        user_name => $hdp::params::nagios_user,
        gid => $hdp::params::nagios_group
      }

      Anchor['hdp::begin'] -> Hdp::Group['nagios_group'] -> Hdp::User['nagios_user'] -> Anchor['hdp::end']
    }

    if ($hdp::params::oozie_server != "") {
      hdp::user{ 'oozie_user':
        user_name => $hdp::params::oozie_user
      }

      Anchor['hdp::begin'] -> Hdp::Group['hdp_user_group'] -> Hdp::User['oozie_user'] -> Anchor['hdp::end']  
    }

    if ($hdp::params::hcat_server_host != "") {
      hdp::user{ 'webhcat_user':
        user_name => $hdp::params::webhcat_user
      }

      if ($hdp::params::webhcat_user != $hdp::params::hcat_user) {
        hdp::user { 'hcat_user':
          user_name => $hdp::params::hcat_user
        }
      }

      Anchor['hdp::begin'] -> Hdp::Group['hdp_user_group'] -> Hdp::User<|title == 'webhcat_user' or title == 'hcat_user'|> -> Anchor['hdp::end'] 
    }

    if ($hdp::params::hive_server_host != "") {
      hdp::user{ 'hive_user':
        user_name => $hdp::params::hive_user
      }

      Anchor['hdp::begin'] -> Hdp::Group['hdp_user_group'] -> Hdp::User['hive_user'] -> Anchor['hdp::end']  
    }

    if ($hdp::params::rm_host != "") {
      hdp::user { 'yarn_user':
        user_name => $hdp::params::yarn_user
      }
      
      Anchor['hdp::begin'] -> Hdp::Group['hdp_user_group'] -> Hdp::User['yarn_user'] -> Anchor['hdp::end']
    }

}

class hdp::pre_install_pkgs
{

  if ($service_state == 'installed_and_configured') {
    hdp::exec{ 'yum install $pre_installed_pkgs':
       command => "yum install -y $pre_installed_pkgs"
    }
  } elsif ($service_state == 'uninstalled') {
    hdp::exec{ 'yum erase $pre_installed_pkgs':
       command => "yum erase -y $pre_installed_pkgs"
    }
  }
}

class hdp::create_hbase_user()
{
  $hbase_user = $hdp::params::hbase_user

  hdp::user{ 'hbase_user':
    user_name => $hbase_user,
    groups => [$hdp::params::user_group]
  }

  ## Set hbase user uid to > 1000
  $cmd_set_hbase_uid_check = "test $(id -u ${hbase_user}) -gt 1000"
  $hbase_user_dirs = "/home/${hbase_user},/tmp/${hbase_user},/usr/bin/${hbase_user}"

  hdp::set_uid { 'set_hbase_user_uid':
    user      => $hbase_user,
    user_dirs => $hbase_user_dirs,
    unless    => $cmd_set_hbase_uid_check
  }

  Group['hdp_user_group'] -> Hdp::User['hbase_user'] -> Hdp::Set_uid['set_hbase_user_uid']
}

class hdp::create_smoke_user()
{

  $smoke_group = $hdp::params::smoke_user_group
  $smoke_user = $hdp::params::smokeuser
  $security_enabled = $hdp::params::security_enabled

  hdp::group { 'smoke_group':
    group_name => $smoke_group,
  }
  
	hdp::group { 'proxyuser_group':
	  group_name => $proxyuser_group,
	}
  
  hdp::user { 'smoke_user':
    user_name => $smoke_user,
    gid    => $hdp::params::user_group,
    groups => ["$proxyuser_group"]
  }

  ## Set smoke user uid to > 1000 for enable security feature
  $smoke_user_dirs = "/tmp/hadoop-${smoke_user},/tmp/hsperfdata_${smoke_user},/home/${smoke_user},/tmp/${smoke_user},/tmp/sqoop-${smoke_user}"
  $cmd_set_uid_check = "test $(id -u ${smoke_user}) -gt 1000"

  hdp::set_uid { 'set_smoke_user_uid':
    user      => $smoke_user,
    user_dirs => $smoke_user_dirs,
    unless    => $cmd_set_uid_check
  }

  Hdp::Group<|title == 'smoke_group' or title == 'proxyuser_group'|> ->
  Hdp::User['smoke_user'] -> Hdp::Set_uid['set_smoke_user_uid']
}


class hdp::set_selinux()
{
 $cmd = "/bin/echo 0 > /selinux/enforce"
 hdp::exec{ $cmd:
    command => $cmd,
    unless => "head -n 1 /selinux/enforce | grep ^0$",
    onlyif => "test -f /selinux/enforce"
 }
}

define hdp::group(
  $group_name = undef
)
{
  if($hdp::params::defined_groups[$group_name]!="defined"){
    group { $name:
      name => $group_name,
      ensure => present   
    }
    
    $hdp::params::defined_groups[$group_name] = "defined"
  }
}

define hdp::user(
  $user_name = undef,
  $gid = $hdp::params::user_group,
  $just_validate = undef,
  $groups = undef,
  $uid = undef
)
{
  $user_info = $hdp::params::user_info[$user_name]
  
  if ($just_validate != undef) {
    $just_val  = $just_validate
  } elsif (($user_info == undef) or ("|${user_info}|" == '||')){ #tests for different versions of Puppet
    $just_val = false
  } else {
    $just_val = $user_info[just_validate]
  }
  
  if ($just_val == true) {
    exec { "user ${name} exists":
      command => "su - ${user_name} -c 'ls /dev/null' >/dev/null 2>&1",
      path    => ['/bin']
    }
  } else {
      if(!defined(User[$user_name])){
        user { $user_name:
          ensure     => present,
          managehome => true,
          gid        => $gid, #TODO either remove this to support LDAP env or fix it
          shell      => '/bin/bash',
          groups     => $groups,
          uid        => $uid
        }
      } else {
        User <| $name == $user_name |> {
          groups +> $groups
        }
      }
  }
}

     
define hdp::directory(
  $owner = undef,
  $group = $hdp::params::user_group,
  $mode  = undef,
  $ensure = directory,
  $force = undef,
  $links = 'follow',
  $service_state = 'running',
  $override_owner = false
  )
{
 if (($service_state == 'uninstalled') and ($wipeoff_data == true)) {
  file { $name :
    ensure => absent,
    owner  => $owner,
    group  => $group,
    mode   => $mode,
    links  => $links,
    force  => $force
   }
  } elsif ($service_state != 'uninstalled') {
    if $override_owner == true {
      file { $name :
      ensure => $ensure,
      owner  => $owner,
      group  => $group,
      links  => $links,
      mode   => $mode,
      force  => $force
     }
    } else {
      file { $name :
      ensure => $ensure,
      links  => $links,
      mode   => $mode,
      force  => $force
     }
    }
  }
}
#TODO: check on -R flag and use of recurse
define hdp::directory_recursive_create(
  $owner = undef,
  $group = $hdp::params::user_group,
  $mode = undef,
  $context_tag = undef,
  $ensure = directory,
  $force = undef,
  $service_state = 'running',
  $override_owner = true
  )
{

  hdp::exec {"mkdir -p ${name}" :
    command => "mkdir -p ${name}",
    creates => $name
  }
  #to take care of setting ownership and mode
  hdp::directory { $name :
    owner => $owner,
    group => $group,
    mode  => $mode,
    ensure => $ensure,
    force => $force,
    service_state => $service_state,
    override_owner => $override_owner
  }
  Hdp::Exec["mkdir -p ${name}"] -> Hdp::Directory[$name]
}

define hdp::directory_recursive_create_ignore_failure(
  $owner = undef,
  $group = $hdp::params::user_group,
  $mode = undef,
  $context_tag = undef,
  $ensure = directory,
  $force = undef,
  $service_state = 'running'
  )
{
  hdp::exec {"mkdir -p ${name} ; exit 0" :
    command => "mkdir -p ${name} ; exit 0",
    creates => $name
  }
    hdp::exec {"chown ${owner}:${group} ${name}; exit 0" :
    command => "chown ${owner}:${group} ${name}; exit 0"
  }
    hdp::exec {"chmod ${mode} ${name} ; exit 0" :
    command => "chmod ${mode} ${name} ; exit 0"
  }
  Hdp::Exec["mkdir -p ${name} ; exit 0"] -> Hdp::Exec["chown ${owner}:${group} ${name}; exit 0"] -> Hdp::Exec["chmod ${mode} ${name} ; exit 0"]
}

### helper to do exec
define hdp::exec(
  $command,
  $refreshonly = undef,
  $unless = undef,
  $onlyif = undef,
  $path = $hdp::params::exec_path,
  $user = undef,
  $creates = undef,
  $tries = 1,
  $timeout = 300,
  $try_sleep = undef,
  $initial_wait = undef,
  $logoutput = 'on_failure',
  $cwd = undef
)
{
     


  if (($initial_wait != undef) and ($initial_wait != "undef")) {
    #passing in creates and unless so dont have to wait if condition has been acheived already
    hdp::wait { "service ${name}" : 
      wait_time => $initial_wait,
      creates   => $creates,
      unless    => $unless,
      onlyif    => $onlyif,
      path      => $path
    }
  }
  
  exec { $name :
    command     => $command,
    refreshonly => $refreshonly,
    path        => $path,
    user        => $user,
    creates     => $creates,
    unless      => $unless,
    onlyif      => $onlyif,
    tries       => $tries,
    timeout     => $timeout,
    try_sleep   => $try_sleep,
    logoutput   => $logoutput,
    cwd         => $cwd
  }
  
  anchor{ "hdp::exec::${name}::begin":} -> Exec[$name] -> anchor{ "hdp::exec::${name}::end":} 
  if (($initial_wait != undef) and ($initial_wait != "undef")) {
    Anchor["hdp::exec::${name}::begin"] -> Hdp::Wait["service ${name}"] -> Exec[$name]
  }
}

#### utilities for waits
define hdp::wait(
  $wait_time,
  $creates = undef,
  $unless = undef,
  $onlyif = undef,
  $path = undef #used for unless
)   
{
  exec { "wait ${name} ${wait_time}" :
    command => "/bin/sleep ${wait_time}",
    creates => $creates,
    unless  => $unless,
    onlyif  => $onlyif,
    path    => $path
  } 
}

define hdp::set_uid(
  $user = undef,
  $user_dirs = undef,
  $unless = undef
)
{
  $cmd_set_uid = "/tmp/changeUid.sh ${user} ${user_dirs} 2>/dev/null"

  hdp::exec{ $cmd_set_uid:
    command => $cmd_set_uid,
    unless  => $unless
  }
}

