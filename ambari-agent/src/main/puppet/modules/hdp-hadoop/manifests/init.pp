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
  $service_state
)
{
  class { 'hdp-hadoop':
    service_state => $service_state
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

  # Configs generation
  debug('##Configs generation for hdp-hadoop')

  if has_key($configuration, 'mapred-queue-acls') {
    configgenerator::configfile{'mapred-queue-acls': 
      modulespath => $hdp-hadoop::params::conf_dir,
      filename => 'mapred-queue-acls.xml',
      module => 'hdp-hadoop',
      configuration => $configuration['mapred-queue-acls'],
      owner => $hdp-hadoop::params::mapred_user,
      group => $hdp::params::user_group
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hadoop::params::conf_dir}/mapred-queue-acls.xml":
      owner => $hdp-hadoop::params::mapred_user,
      group => $hdp::params::user_group
    }
  }
  
  if has_key($configuration, 'hadoop-policy') {
    configgenerator::configfile{'hadoop-policy': 
      modulespath => $hdp-hadoop::params::conf_dir,
      filename => 'hadoop-policy.xml',
      module => 'hdp-hadoop',
      configuration => $configuration['hadoop-policy'],
      owner => $hdp-hadoop::params::hdfs_user,
      group => $hdp::params::user_group
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hadoop::params::conf_dir}/hadoop-policy.xml":
      owner => $hdp-hadoop::params::hdfs_user,
      group => $hdp::params::user_group
    }
  }

  if has_key($configuration, 'core-site') {
      configgenerator::configfile{'core-site': 
        modulespath => $hdp-hadoop::params::conf_dir,
        filename => 'core-site.xml',
        module => 'hdp-hadoop',
        configuration => $configuration['core-site'],
        owner => $hdp-hadoop::params::hdfs_user,
        group => $hdp::params::user_group
      }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hadoop::params::conf_dir}/core-site.xml":
      owner => $hdp-hadoop::params::hdfs_user,
      group => $hdp::params::user_group
    }
  }

  if has_key($configuration, 'mapred-site') {
    configgenerator::configfile{'mapred-site': 
      modulespath => $hdp-hadoop::params::conf_dir,
      filename => 'mapred-site.xml',
      module => 'hdp-hadoop',
      configuration => $configuration['mapred-site'],
      owner => $hdp-hadoop::params::mapred_user,
      group => $hdp::params::user_group
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hadoop::params::conf_dir}/mapred-site.xml":
      owner => $hdp-hadoop::params::mapred_user,
      group => $hdp::params::user_group
    }
  }

  $task_log4j_properties_location = "${hdp-hadoop::params::conf_dir}/task-log4j.properties"
  
  file { $task_log4j_properties_location:
    owner   => $hdp-hadoop::params::mapred_user,
    group   => $hdp::params::user_group,
    mode    => 644,
    ensure  => present,
    source  => "puppet:///modules/hdp-hadoop/task-log4j.properties",
    replace => false
  }

  if has_key($configuration, 'capacity-scheduler') {
    configgenerator::configfile{'capacity-scheduler':
      modulespath => $hdp-hadoop::params::conf_dir,
      filename => 'capacity-scheduler.xml',
      module => 'hdp-hadoop',
      configuration => $configuration['capacity-scheduler'],
      owner => $hdp-hadoop::params::hdfs_user,
      group => $hdp::params::user_group,
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hadoop::params::conf_dir}/capacity-scheduler.xml":
      owner => $hdp-hadoop::params::hdfs_user,
      group => $hdp::params::user_group
    }
  } 


  if has_key($configuration, 'hdfs-site') {
    configgenerator::configfile{'hdfs-site': 
      modulespath => $hdp-hadoop::params::conf_dir,
      filename => 'hdfs-site.xml',
      module => 'hdp-hadoop',
      configuration => $configuration['hdfs-site'],
      owner => $hdp-hadoop::params::hdfs_user,
      group => $hdp::params::user_group
    }
  } else { # Manually overriding ownership of file installed by hadoop package
    file { "${hdp-hadoop::params::conf_dir}/hdfs-site.xml":
      owner => $hdp-hadoop::params::hdfs_user,
      group => $hdp::params::user_group
    }
  }

  if has_key($configuration, 'hdfs-exclude-file') {
    hdp-hadoop::hdfs::generate_exclude_file{'exclude_file':}
  }

  hdp::package {'ambari-log4j':
    package_type  => 'ambari-log4j'
  }

  file { '/usr/lib/hadoop/lib/hadoop-tools.jar':
    ensure => 'link',
    target => '/usr/lib/hadoop/hadoop-tools.jar',
    mode => 755,
  }

  file { "${hdp-hadoop::params::conf_dir}/configuration.xsl":
    owner => $hdp-hadoop::params::hdfs_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-hadoop::params::conf_dir}/fair-scheduler.xml":
    owner => $hdp-hadoop::params::mapred_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-hadoop::params::conf_dir}/masters":
    owner => $hdp-hadoop::params::hdfs_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-hadoop::params::conf_dir}/ssl-client.xml.example":
    owner => $hdp-hadoop::params::mapred_user,
    group => $hdp::params::user_group
  }

  file { "${hdp-hadoop::params::conf_dir}/ssl-server.xml.example":
    owner => $hdp-hadoop::params::mapred_user,
    group => $hdp::params::user_group
  }

  if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
    if (hdp_is_empty($configuration) == false and hdp_is_empty($configuration['hdfs-site']) == false) {
      if (hdp_is_empty($configuration['hdfs-site']['dfs.hosts.exclude']) == false) and
         (hdp_is_empty($configuration['hdfs-exclude-file']) or
          has_key($configuration['hdfs-exclude-file'], 'datanodes') == false) {
        $exlude_file_path = $configuration['hdfs-site']['dfs.hosts.exclude']
        file { $exlude_file_path :
        ensure => present,
        owner => $hdp-hadoop::params::hdfs_user,
        group => $hdp::params::user_group
        }
      }
      if (hdp_is_empty($hdp::params::slave_hosts) == false and hdp_is_empty($configuration['hdfs-site']['dfs.hosts']) == false) {
        $include_file_path = $configuration['hdfs-site']['dfs.hosts']
        $include_hosts_list = $hdp::params::slave_hosts
        file { $include_file_path :
        ensure => present,
        owner => $hdp-hadoop::params::hdfs_user,
        group => $hdp::params::user_group,
        content => template('hdp-hadoop/include_hosts_list.erb')
        }
      }
    }
  }

}

class hdp-hadoop(
  $service_state
)
{
  include hdp-hadoop::params
  $hadoop_config_dir = $hdp-hadoop::params::conf_dir
  $mapred_user = $hdp-hadoop::params::mapred_user  
  $hdfs_user = $hdp-hadoop::params::hdfs_user
  $hadoop_tmp_dir = $hdp-hadoop::params::hadoop_tmp_dir

  anchor{'hdp-hadoop::begin':} 
  anchor{'hdp-hadoop::end':} 

  if ($service_state=='uninstalled') {
    hdp-hadoop::package { 'hadoop':
      ensure => 'uninstalled'
    }

    hdp::directory_recursive_create { $hadoop_config_dir:
      service_state => $::service_state,
      force => true
    }

    Anchor['hdp-hadoop::begin'] -> Hdp-hadoop::Package<||> -> Hdp::Directory_recursive_create[$hadoop_config_dir] -> Anchor['hdp-hadoop::end']
  } else {
    
    hdp-hadoop::package { 'hadoop':}

    #Replace limits config file
    hdp::configfile {"${hdp::params::limits_conf_dir}/hdfs.conf":
      component => 'hadoop',
      owner => 'root',
      group => 'root',
      require => Hdp-hadoop::Package['hadoop'],
      before  => Anchor['hdp-hadoop::end'],
      mode => 644    
    }

    hdp::directory_recursive_create { $hadoop_config_dir:
      service_state => $::service_state,
      force => true,
      owner => 'root',
      group => 'root'
    }
 
    hdp::user{ 'hdfs_user':
      user_name => $hdfs_user,
      groups => [$hdp::params::user_group]
    }
    
    hdp::user { 'mapred_user':
      user_name => $mapred_user,
      groups => [$hdp::params::user_group]
    }

    $logdirprefix = $hdp-hadoop::params::hdfs_log_dir_prefix
    hdp::directory_recursive_create { $logdirprefix: 
        owner => 'root'
    }
    $piddirprefix = $hdp-hadoop::params::hadoop_pid_dir_prefix
    hdp::directory_recursive_create { $piddirprefix: 
        owner => 'root'
    }

    $dfs_domain_socket_path_dir = hdp_get_directory_from_filepath($hdp-hadoop::params::dfs_domain_socket_path)
    hdp::directory_recursive_create { $dfs_domain_socket_path_dir:
      owner => $hdfs_user,
      group => $hdp::params::user_group,
      mode  => '0644'
    }
 
    #taskcontroller.cfg properties conditional on security
    if ($hdp::params::security_enabled == true) {
      file { "${hdp::params::hadoop_bin}/task-controller":
        owner   => 'root',
        group   => $hdp::params::user_group,
        mode    => '6050',
        require => Hdp-hadoop::Package['hadoop'],
        before  => Anchor['hdp-hadoop::end']
      }
      $tc_owner = 'root'
      $tc_mode = '0644'
    } else {
      $tc_owner = $hdfs_user
      $tc_mode = undef
    }
    hdp-hadoop::configfile { 'taskcontroller.cfg' :
      tag   => 'common',
      owner => $tc_owner,
      mode  => $tc_mode
    }

    $template_files = [ 'hadoop-env.sh', 'commons-logging.properties', 'slaves']
    hdp-hadoop::configfile { $template_files:
      tag   => 'common', 
      owner => $hdfs_user
    }

    if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
      hdp-hadoop::configfile { 'health_check' :
        tag   => 'common',
        owner => $hdfs_user,
        template_tag => 'v2'
      }
    } else {
      hdp-hadoop::configfile { 'health_check' :
        tag   => 'common',
        owner => $hdfs_user
      }
    }

    # log4j.properties has to be installed just one time to prevent
    # manual changes overwriting
    if ($service_state=='installed_and_configured') {
      hdp-hadoop::configfile { 'log4j.properties' :
        tag   => 'common',
        owner => $hdfs_user,
      }
    }

    # updating log4j.properties with data which is sent from server
    hdp-hadoop::update-log4j-properties { 'log4j.properties': }
    
    hdp-hadoop::configfile { 'hadoop-metrics2.properties' : 
      tag   => 'common', 
      owner => $hdfs_user,
    }

    # Copy database drivers for rca enablement
    $server_db_name = $hdp::params::server_db_name
    $hadoop_lib_home = $hdp::params::hadoop_lib_home
    $db_driver_filename = $hdp::params::db_driver_file
    $oracle_driver_url = $hdp::params::oracle_jdbc_url
    $mysql_driver_url = $hdp::params::mysql_jdbc_url

    if ($server_db_name == 'oracle' and $oracle_driver_url != "") {
      $db_driver_dload_cmd = "curl -kf --retry 5 $oracle_driver_url -o ${hadoop_lib_home}/${db_driver_filename}"
    } elsif ($server_db_name == 'mysql' and $mysql_driver_url != "") {
      $db_driver_dload_cmd = "curl -kf --retry 5 $mysql_driver_url -o ${hadoop_lib_home}/${db_driver_filename}"
    }
    if ($db_driver_dload_cmd != undef) {
      exec { '${db_driver_dload_cmd}':
        command => $db_driver_dload_cmd,
        unless  => "test -e ${hadoop_lib_home}/${db_driver_filename}",
        creates => "${hadoop_lib_home}/${db_driver_filename}",
        path    => ["/bin","/usr/bin/"],
        require => Hdp-hadoop::Package['hadoop'],
        before  => Anchor['hdp-hadoop::end']
      }
    }

    if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
      hdp::directory_recursive_create { "$hadoop_tmp_dir":
        service_state => $service_state,
        force => true,
        owner => $hdfs_user
      }
    }

    if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
      Anchor['hdp-hadoop::begin'] -> Hdp-hadoop::Package<||> ->  Hdp::User<|title == $hdfs_user or title == $mapred_user|>  ->
      Hdp::Directory_recursive_create[$hadoop_config_dir] -> Hdp-hadoop::Configfile<|tag == 'common'|> -> Hdp-hadoop::Update-log4j-properties['log4j.properties'] ->
      Hdp::Directory_recursive_create[$logdirprefix] -> Hdp::Directory_recursive_create[$piddirprefix] -> Hdp::Directory_recursive_create["$hadoop_tmp_dir"] -> Anchor['hdp-hadoop::end']
    } else {
      Anchor['hdp-hadoop::begin'] -> Hdp-hadoop::Package<||> ->  Hdp::User<|title == $hdfs_user or title == $mapred_user|>  ->
      Hdp::Directory_recursive_create[$hadoop_config_dir] -> Hdp-hadoop::Configfile<|tag == 'common'|> -> Hdp-hadoop::Update-log4j-properties['log4j.properties'] ->
      Hdp::Directory_recursive_create[$logdirprefix] -> Hdp::Directory_recursive_create[$piddirprefix] -> Anchor['hdp-hadoop::end']
    }

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
  $kinit_override = false,
  $tries = 1,
  $timeout = 900,
  $try_sleep = undef,
  $user = undef,
  $logoutput = undef,
  $onlyif = undef,
  $path = undef
)
{
  include hdp-hadoop::params
  $security_enabled = $hdp::params::security_enabled
  $conf_dir = $hdp-hadoop::params::conf_dir
  $hdfs_user = $hdp-hadoop::params::hdfs_user
  $hbase_user = $hdp-hadoop::params::hbase_user

  if ($user == undef) {
    $run_user = $hdfs_user
  } else {
    $run_user = $user
  }

  if (($security_enabled == true) and ($kinit_override == false)) {
    if ($run_user in [$hdfs_user,'root']) {
      $keytab = $hdp::params::hdfs_user_keytab
      $principal = $hdfs_user
    } elsif ($run_user in [$hbase_user]) {
      $keytab = $hdp::params::hbase_user_keytab
      $principal = $hbase_user
    } else {
      $keytab = $hdp::params::smokeuser_keytab
      $principal = $hdp::params::smokeuser
    }
    $kinit_if_needed = "su - ${run_user} -c '${hdp::params::kinit_path_local} -kt ${keytab} ${principal}'"
  } else {
    $kinit_if_needed = ""
  }
  
  if ($path == undef) {
    if ($echo_yes == true) {
      $cmd = "yes Y | hadoop --config ${conf_dir} ${command}"
    } else {
      $cmd = "hadoop --config ${conf_dir} ${command}"
    } 
    } else {
      $cmd = "${path} ${command}"
    }
  
  if ($kinit_if_needed != "") {
    exec { "kinit_before_${cmd}":
      command => $kinit_if_needed,
      path => ['/bin'],
      before => Hdp::Exec[$cmd]
    }
  }

  hdp::exec { $cmd:
    command     => $cmd,
    user        => $run_user,
    unless      => $unless,
    refreshonly => $refreshonly,
    tries       => $tries,
    timeout     => $timeout,
    try_sleep   => $try_sleep,
    logoutput   => $logoutput,
    onlyif      => $onlyif,
  }
}

#####
define hdp-hadoop::update-log4j-properties(
  $hadoop_conf_dir = $hdp-hadoop::params::conf_dir
)
{
  $properties = [
    { name => 'ambari.jobhistory.database', value => $hdp-hadoop::params::ambari_db_rca_url },
    { name => 'ambari.jobhistory.driver', value => $hdp-hadoop::params::ambari_db_rca_driver },
    { name => 'ambari.jobhistory.user', value => $hdp-hadoop::params::ambari_db_rca_username },
    { name => 'ambari.jobhistory.password', value => $hdp-hadoop::params::ambari_db_rca_password },
    { name => 'ambari.jobhistory.logger', value => 'DEBUG,JHA' },

    { name => 'log4j.appender.JHA', value => 'org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.JobHistoryAppender' },
    { name => 'log4j.appender.JHA.database', value => '${ambari.jobhistory.database}' },
    { name => 'log4j.appender.JHA.driver', value => '${ambari.jobhistory.driver}' },
    { name => 'log4j.appender.JHA.user', value => '${ambari.jobhistory.user}' },
    { name => 'log4j.appender.JHA.password', value => '${ambari.jobhistory.password}' },

    { name => 'log4j.logger.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger', value => '${ambari.jobhistory.logger}' },
    { name => 'log4j.additivity.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger', value => 'true' }
  ]
  hdp-hadoop::update-log4j-property { $properties :
    log4j_file      => $name,
    hadoop_conf_dir => $hadoop_conf_dir
  }
}

#####
define hdp-hadoop::update-log4j-property(
  $log4j_file,
  $hadoop_conf_dir = $hdp-hadoop::params::conf_dir
)
{
  hdp::exec{ "sed -i 's~\\(${hdp-hadoop::params::rca_disabled_prefix}\\)\\?${name[name]}=.*~${hdp-hadoop::params::rca_prefix}${name[name]}=${name[value]}~' ${hadoop_conf_dir}/${log4j_file}":
    command => "sed -i 's~\\(${hdp-hadoop::params::rca_disabled_prefix}\\)\\?${name[name]}=.*~${hdp-hadoop::params::rca_prefix}${name[name]}=${name[value]}~' ${hadoop_conf_dir}/${log4j_file}"
  }
}