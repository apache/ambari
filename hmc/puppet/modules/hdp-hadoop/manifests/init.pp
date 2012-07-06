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
#singleton for use with <||> form so that namenode, datanode, etc can pass state to hdp-hadoop and still use include
define hdp-hadoop::common(
  $service_states = []
)
{
  class { 'hdp-hadoop':
    service_states => $service_states    
  }
  anchor{'hdp-hadoop::common::begin':} -> Class['hdp-hadoop'] -> anchor{'hdp-hadoop::common::end':} 
}

class hdp-hadoop::initialize()
{
  if ($hdp::params::component_exists['hdp-hadoop'] == true) {
  } else {
    $hdp::params::component_exists['hdp-hadoop'] = true
  }
  hdp-hadoop::common { 'common':}
  anchor{'hdp-hadoop::initialize::begin':} -> Hdp-hadoop::Common['common'] -> anchor{'hdp-hadoop::initialize::end':}
}

class hdp-hadoop(
  $service_states  = []
)
{
  include hdp-hadoop::params
  $hadoop_config_dir = $hdp-hadoop::params::conf_dir
  $mapred_user = $hdp-hadoop::params::mapred_user  
  $hdfs_user = $hdp-hadoop::params::hdfs_user  

  anchor{'hdp-hadoop::begin':} 
  anchor{'hdp-hadoop::end':} 

  if ('uninstalled' in $service_states) {
    hdp-hadoop::package { 'hadoop':
      ensure => 'uninstalled'
    }

    hdp::directory_recursive_create { $hadoop_config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp-hadoop::begin'] -> Hdp-hadoop::Package<||> -> Hdp::Directory_recursive_create[$hadoop_config_dir] -> Anchor['hdp-hadoop::end']
  } else {
    
    hdp-hadoop::package { 'hadoop':}


    hdp::directory_recursive_create { $hadoop_config_dir:
      service_state => $service_state,
      force => true
    }
 
    hdp::user{ $hdfs_user:}
    hdp::user { $mapred_user:}

    $logdirprefix = $hdp-hadoop::params::hadoop_logdirprefix
    hdp::directory_recursive_create { $logdirprefix: 
        owner => 'root'
    }
    $piddirprefix = $hdp-hadoop::params::hadoop_piddirprefix
    hdp::directory_recursive_create { $piddirprefix: 
        owner => 'root'
    }
 
    $template_files = ['hadoop-env.sh','core-site.xml','hdfs-site.xml','hadoop-policy.xml','taskcontroller.cfg','health_check','capacity-scheduler.xml','commons-logging.properties','log4j.properties','mapred-queue-acls.xml','slaves']
    hdp-hadoop::configfile { $template_files:
      tag   => 'common', 
      owner => $hdfs_user
    }
    
    hdp-hadoop::configfile { 'hadoop-metrics2.properties' : 
      tag   => 'common', 
      owner => $hdfs_user,
    }

    hdp-hadoop::configfile { 'mapred-site.xml': 
      tag => 'common', 
      owner => $mapred_user
    }

    Anchor['hdp-hadoop::begin'] -> Hdp-hadoop::Package<||> ->  Hdp::Directory_recursive_create[$hadoop_config_dir] ->  Hdp::User<|title == $hdfs_user or title == $mapred_user|> 
    -> Hdp-hadoop::Configfile<|tag == 'common'|> -> Anchor['hdp-hadoop::end']
    Anchor['hdp-hadoop::begin'] -> Hdp::Directory_recursive_create[$logdirprefix] -> Anchor['hdp-hadoop::end']
    Anchor['hdp-hadoop::begin'] -> Hdp::Directory_recursive_create[$piddirprefix] -> Anchor['hdp-hadoop::end']
  }
}

class hdp-hadoop::enable-ganglia()
{
  Hdp-hadoop::Configfile<|title  == 'hadoop-metrics2.properties'|>{template_tag => 'GANGLIA'}
}

###config file helper
define hdp-hadoop::configfile(
  $owner = undef,
  $hadoop_conf_dir = $hdp-hadoop::params::conf_dir,
  $mode = undef,
  $namenode_host = undef,
  $jtnode_host = undef,
  $snamenode_host = undef,
  $template_tag = undef,
  $size = undef, #TODO: deprecate
  $sizes = []
) 
{
  #TODO: may need to be fixed 
  if ($jtnode_host == undef) {
    $calc_jtnode_host = $namenode_host
  } else {
    $calc_jtnode_host = $jtnode_host 
  }
 
  #only set 32 if theer is a 32 bit component and no 64 bit components
  if (64 in $sizes) {
    $common_size = 64
  } elsif (32 in $sizes) {
    $common_size = 32
  } else {
    $common_size = 6
  }
  
  hdp::configfile { "${hadoop_conf_dir}/${name}":
    component      => 'hadoop',
    owner          => $owner,
    mode           => $mode,
    namenode_host  => $namenode_host,
    snamenode_host => $snamenode_host,
    jtnode_host    => $calc_jtnode_host,
    template_tag   => $template_tag,
    size           => $common_size
  }
}

#####
define hdp-hadoop::exec-hadoop(
  $command,
  $unless = undef,
  $refreshonly = undef,
  $echo_yes = false,
  $tries = 1,
  $timeout = 900,
  $try_sleep = undef,
  $user = undef,
  $logoutput = undef
)
{
  include hdp-hadoop::params
  $conf_dir = $hdp-hadoop::params::conf_dir
  if ($echo_yes == true) {
    $cmd = "yes Y | hadoop --config ${conf_dir} ${command}"
  } else {
    $cmd = "hadoop --config ${conf_dir} ${command}"     
  }
  if ($user == undef) {
   $run_user = $hdp-hadoop::params::hdfs_user
  } else {
    $run_user = $user
  }
  hdp::exec { $cmd:
    command     => $cmd,
    user        => $run_user,
    unless      => $unless,
    refreshonly => $refreshonly,
    tries       => $tries,
    timeout     => $timeout,
    try_sleep   => $try_sleep,
    logoutput   => $logoutput
  }
}
