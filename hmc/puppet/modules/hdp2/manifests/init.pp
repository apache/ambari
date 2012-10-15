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
class hdp2(
  $service_state = undef,
  $pre_installed_pkgs = undef
)
{
  import 'params.pp'
  include hdp2::params
  
  Exec { logoutput => 'on_failure' }
 
  group { $hdp2::params::hadoop_user_group :
    ensure => present
  }

  #TODO: think not needed and also there seems to be a puppet bug around this and ldap
  hdp2::user { $hdp2::params::hadoop_user:
    gid => $hdp2::params::hadoop_user_group
  }
  Group[$hdp2::params::hadoop_user_group] -> Hdp2::User[$hdp2::params::hadoop_user] 
  class { 'hdp2::snmp': service_state => 'running'}

  class { 'hdp2::create_smoke_user': }

  if ($pre_installed_pkgs != undef) {
    class { 'hdp2::pre_install_pkgs': }
  }

  #turns off selinux
  class { 'hdp2::set_selinux': }

  if ($service_state != 'uninstalled') {
    if ($hdp2::params::mapreduce_lzo_enabled == true) {
      @hdp2::lzo::package{ 32:}
      @hdp2::lzo::package{ 64:}
    }
  }

  #TODO: treat consistently 
  if ($service_state != 'uninstalled') {
    if ($hdp2::params::mapreduce_snappy_enabled == true) {
      include hdp2::snappy::package
    }
  }

  Hdp2::Package<|title == 'hadoop 32'|> ->   Hdp2::Package<|title == 'hbase'|>
  Hdp2::Package<|title == 'hadoop 64'|> ->   Hdp2::Package<|title == 'hbase'|>

  #TODO: just for testing
  class{ 'hdp2::iptables': 
    ensure => stopped,
  }

  case $hdp2::params::hdp_os_type {
    centos6, rhel6: {
      hdp2::package{ 'glibc-rhel6':
        ensure       => 'present',
        size         => $size,
        java_needed  => false,
        lzo_needed   => false
      }
    }
  }

}

class hdp2::pre_install_pkgs
{
  if ($service_state == 'installed_and_configured') {
    hdp2::exec{ 'yum install $pre_installed_pkgs':
       command => "yum install -y $pre_installed_pkgs"
    }
  } elsif ($service_state == 'uninstalled') {
    hdp2::exec{ 'yum erase $pre_installed_pkgs':
       command => "yum erase -y $pre_installed_pkgs"
    }
  }
}

class hdp2::create_smoke_user()
{
  $smoke_group = $hdp2::params::smoke_user_group
  $smoke_user = $hdp2::params::smokeuser

  group { $smoke_group :
    ensure => present
  }

  hdp2::user { $smoke_user:}

  $cmd = "usermod -g  $smoke_group  $smoke_user"
  $check_group_cmd = "id -gn $smoke_user | grep $smoke_group"
  hdp2::exec{ $cmd:
     command => $cmd,
     unless => $check_group_cmd
  }
 
  Group[$smoke_group] -> Hdp2::User[$smoke_user] -> Hdp2::Exec[$cmd] 
}


class hdp2::set_selinux()
{
 $cmd = "/bin/echo 0 > /selinux/enforce"
 hdp2::exec{ $cmd:
    command => $cmd,
    unless => "head -n 1 /selinux/enforce | grep ^0$"
  }
}

define hdp2::user(
  $gid = $hdp2::params::hadoop_user_group,
  $just_validate = undef
)
{
  $user_info = $hdp2::params::user_info[$name]
  if ($just_validate != undef) {
    $just_val  = $just_validate
  } elsif (($user_info == undef) or ("|${user_info}|" == '||')){ #tests for different versions of Puppet
    $just_val = false
  } else {
    $just_val = $user_info[just_validate]
  }
  
  if ($just_val == true) {
    exec { "user ${name} exists":
      command => "su - ${name} -c 'ls /dev/null' >/dev/null 2>&1",
      path    => ['/bin']
    }
  } else {
    user { $name:
      ensure     => present,
      managehome => true,
      #gid        => $gid, #TODO either remove this to support LDAP env or fix it
      shell      => '/bin/bash'
    }
  }
}
     
define hdp2::directory(
  $owner = $hdp2::params::hadoop_user,
  $group = $hdp2::params::hadoop_user_group,
  $mode  = undef,
  $ensure = directory,
  $force = undef,
  $service_state = 'running'
  )
{
 if (($service_state == 'uninstalled') and ($wipeoff_data == true)) {
  file { $name :
    ensure => absent,
    owner  => $owner,
    group  => $group,
    mode   => $mode,
    force  => $force
   }
  } elsif ($service_state != 'uninstalled') {
  file { $name :
    ensure => present,
    owner  => $owner,
    group  => $group,
    mode   => $mode,
    force  => $force
   }
  }
}
#TODO: check on -R flag and use of recurse
define hdp2::directory_recursive_create(
  $owner = $hdp2::params::hadoop_user,
  $group = $hdp2::params::hadoop_user_group,
  $mode = undef,
  $context_tag = undef,
  $ensure = directory,
  $force = undef,
  $service_state = 'running'
  )
{
  hdp2::exec {"mkdir -p ${name}" :
    command => "mkdir -p ${name}",
    creates => $name
  }
  #to take care of setting ownership and mode
  hdp2::directory { $name :
    owner => $owner,
    group => $group,
    mode  => $mode,
    ensure => $ensure,
    force => $force,
    service_state => $service_state
  }
  Hdp2::Exec["mkdir -p ${name}"] -> Hdp2::Directory[$name]
}

### helper to do exec
define hdp2::exec(
  $command,
  $refreshonly = undef,
  $unless = undef,
  $onlyif = undef,
  $path = $hdp2::params::exec_path,
  $user = undef,
  $creates = undef,
  $tries = 1,
  $timeout = 900,
  $try_sleep = undef,
  $initial_wait = undef,
  $logoutput = 'on_failure',
  $cwd = undef
)
{
     
  if (($initial_wait != undef) and ($initial_wait != "undef")) {
    #passing in creates and unless so dont have to wait if condition has been acheived already
    hdp2::wait { "service ${name}" : 
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
  
  anchor{ "hdp2::exec::${name}::begin":} -> Exec[$name] -> anchor{ "hdp2::exec::${name}::end":} 
  if (($initial_wait != undef) and ($initial_wait != "undef")) {
    Anchor["hdp2::exec::${name}::begin"] -> Hdp2::Wait["service ${name}"] -> Exec[$name]
  }
}

#### utilities for waits
define hdp2::wait(
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

#### artifact_dir
define hdp2::artifact_dir()
{
  include hdp2_artifact_dir_shared
}
class hdp2_artifact_dir_shared()
{
  file{ $hdp2::params::artifact_dir:
    ensure  => directory
  }
}

##### temp

class hdp2::iptables($ensure)
{
  #TODO: just temp so not considering things like saving firewall rules
  service { 'iptables':
    ensure => $ensure
  }
}
