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
#singleton for use with <||> form so that namenode, datanode, etc can pass state to hdp2-hadoop and still use include
define hdp2-hadoop::common(
  $service_states = []
)
{
  class { 'hdp2-hadoop':
    service_states => $service_states    
  }
  anchor{'hdp2-hadoop::common::begin':} -> Class['hdp2-hadoop'] -> anchor{'hdp2-hadoop::common::end':} 
}

class hdp2-hadoop::initialize()
{
  if ($hdp2::params::component_exists['hdp2-hadoop'] == true) {
  } else {
    $hdp2::params::component_exists['hdp2-hadoop'] = true
  }
  hdp2-hadoop::common { 'common':}
  anchor{'hdp2-hadoop::initialize::begin':} -> Hdp2-hadoop::Common['common'] -> anchor{'hdp2-hadoop::initialize::end':}
}

class hdp2-hadoop(
  $service_states  = []
)
{
  include hdp2-hadoop::params
  $hadoop_config_dir = $hdp2-hadoop::params::conf_dir
  $hdfs_user = $hdp2-hadoop::params::hdfs_user  
  $yarn_user = $hdp2-hadoop::params::yarn_user  
  $mapred_user = $hdp2-hadoop::params::mapred_user  

  anchor{'hdp2-hadoop::begin':} 
  anchor{'hdp2-hadoop::end':} 

  if ('uninstalled' in $service_states) {
    hdp2-hadoop::package { 'hadoop':
      ensure => 'uninstalled'
    }

    hdp2::directory_recursive_create { $hadoop_config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp2-hadoop::begin'] -> Hdp2-hadoop::Package<||> -> Hdp2::Directory_recursive_create[$hadoop_config_dir] -> Anchor['hdp2-hadoop::end']
  } else {
    
    hdp2-hadoop::package { 'hadoop':}


    hdp2::directory_recursive_create { $hadoop_config_dir:
      service_state => $service_state,
      force => true
    }
 
    hdp2::user { $hdfs_user:}
    hdp2::user { $yarn_user:}
    hdp2::user { $mapred_user:}

    $logdirprefix = $hdp2-hadoop::params::hadoop_logdirprefix
    hdp2::directory_recursive_create { $logdirprefix: 
        owner => 'root'
    }
    $piddirprefix = $hdp2-hadoop::params::hadoop_piddirprefix
    hdp2::directory_recursive_create { $piddirprefix: 
        owner => 'root'
    }
 
    $common_hdfs_template_files = ['hadoop-env.sh','core-site.xml','hdfs-site.xml','hadoop-policy.xml','health_check','hadoop-metrics2.properties','commons-logging.properties','log4j.properties','slaves']
    hdp2-hadoop::configfile { $common_hdfs_template_files:
      tag   => 'common', 
      owner => $hdfs_user
    }
    
    $yarn_template_files = ['yarn-site.xml','yarn-env.sh','container-executor.cfg','capacity-scheduler.xml']
    hdp2-hadoop::configfile { $yarn_template_files: 
      tag => 'common', 
      owner => $yarn_user
    }

    hdp2-hadoop::configfile { 'mapred-site.xml': 
      tag => 'common', 
      owner => $mapred_user
    }

    Anchor['hdp2-hadoop::begin'] -> Hdp2-hadoop::Package<||> ->  Hdp2::Directory_recursive_create[$hadoop_config_dir] ->  Hdp2::User<|title == $hdfs_user or title == $yarn_user or title == $mapred_user|> 
    -> Hdp2-hadoop::Configfile<|tag == 'common'|> -> Anchor['hdp2-hadoop::end']
    Anchor['hdp2-hadoop::begin'] -> Hdp2::Directory_recursive_create[$logdirprefix] -> Anchor['hdp2-hadoop::end']
    Anchor['hdp2-hadoop::begin'] -> Hdp2::Directory_recursive_create[$piddirprefix] -> Anchor['hdp2-hadoop::end']
  }
}

class hdp2-hadoop::enable-ganglia()
{
  Hdp2-hadoop::Configfile<|title  == 'hadoop-metrics2.properties'|>{template_tag => 'GANGLIA'}
}

###config file helper
define hdp2-hadoop::configfile(
  $owner = undef,
  $hadoop_conf_dir = $hdp2-hadoop::params::conf_dir,
  $mode = undef,
  $namenode_host = undef,
  $yarn_rm_host = undef,
  $snamenode_host = undef,
  $template_tag = undef,
  $size = undef, #TODO: deprecate
  $sizes = []
) 
{
  #TODO: may need to be fixed 
  if ($yarn_rm_host == undef) {
    $calc_yarn_rm_host = $namenode_host
  } else {
    $calc_yarn_rm_host = $yarn_rm_host 
  }
 
  #only set 32 if theer is a 32 bit component and no 64 bit components
  if (64 in $sizes) {
    $common_size = 64
  } elsif (32 in $sizes) {
    $common_size = 32
  } else {
    $common_size = 6
  }
  
  hdp2::configfile { "${hadoop_conf_dir}/${name}":
    component      => 'hadoop',
    owner          => $owner,
    mode           => $mode,
    namenode_host  => $namenode_host,
    snamenode_host => $snamenode_host,
    yarn_rm_host   => $calc_yarn_rm_host,
    template_tag   => $template_tag,
    size           => $common_size
  }
}

#####
define hdp2-hadoop::exec-hadoop(
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
  include hdp2-hadoop::params
  $conf_dir = $hdp2-hadoop::params::conf_dir
  if ($echo_yes == true) {
    $cmd = "yes Y | hadoop --config ${conf_dir} ${command}"
  } else {
    $cmd = "hadoop --config ${conf_dir} ${command}"     
  }
  if ($user == undef) {
   $run_user = $hdp2-hadoop::params::hdfs_user
  } else {
    $run_user = $user
  }
  hdp2::exec { $cmd:
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
