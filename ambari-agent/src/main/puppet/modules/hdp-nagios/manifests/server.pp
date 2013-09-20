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
class hdp-nagios::server(
  $service_state = $hdp::params::cluster_service_state
) inherits hdp-nagios::params
{
  $nagios_var_dir = $hdp-nagios::params::nagios_var_dir
  $nagios_rw_dir = $hdp-nagios::params::nagios_rw_dir
  $nagios_config_dir = $hdp-nagios::params::conf_dir
  $plugins_dir = $hdp-nagios::params::plugins_dir
  $nagios_obj_dir = $hdp-nagios::params::nagios_obj_dir
  $check_result_path = $hdp-nagios::params::check_result_path
  $nagios_httpd_config_file = $hdp-nagios::params::httpd_conf_file
  $pid_file = $hdp-nagios::params::nagios_pid_file

  if hdp_is_empty($hdp::params::pathes[nagios_p1_pl]) {
    hdp_fail("There is no path to p1.pl file for nagios")
  }
  else {
    $nagios_p1_pl_by_os = $hdp::params::pathes[nagios_p1_pl]
  }

  if hdp_is_empty($nagios_p1_pl_by_os[$hdp::params::hdp_os_type]) {
    if hdp_is_empty($nagios_p1_pl_by_os['ALL']) {
      hdp_fail("There is no path to p1.pl file for nagios")
    }
      else {
        $nagios_p1_pl = $nagios_p1_pl_by_os['ALL']
      }
    }
    else {
      $nagios_p1_pl = $nagios_p1_pl_by_os[$hdp::params::hdp_os_type]
    }



  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
    class { 'hdp-nagios::server::packages' : 
      service_state => uninstalled
    }

    hdp::exec { "rm -f /var/nagios/rw/nagios.cmd" :
      command => "rm -f /var/nagios/rw/nagios.cmd",
      unless => "test ! -e  /var/nagios/rw/nagios.cmd"
    }

    hdp::exec { "rm -rf /tmp/hadoop-nagios" :
      command => "rm -rf /tmp/hadoop-nagios",
      unless => "test ! -e  /tmp/hadoop-nagios"
    }

    hdp::directory { $nagios_config_dir:
      service_state => $service_state,
      force => true
    }

    hdp::directory { $plugins_dir:
      service_state => $service_state,
      force => true
    }

    hdp::directory { $nagios_obj_dir:
      service_state => $service_state,
      force => true
    }
	
	hdp::directory_recursive_create { nagios_pid_dir:
      service_state => $service_state,
      force => true
    }

    hdp::directory { $nagios_var_dir:
      service_state => $service_state,
      force => true
    }
    



     Class['hdp-nagios::server::packages'] -> Exec['rm -f /var/nagios/rw/nagios.cmd'] -> Hdp::Directory[$nagios_config_dir] -> Hdp::Directory[$plugins_dir] -> Hdp::Directory[$nagios_obj_dir] ->  Hdp::Directory_recursive_create[$nagios_pid_dir] -> Hdp::Directory[$nagios_var_dir]

  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    class { 'hdp-nagios::server::packages' : service_state => $service_state}
  
    file{ $nagios_httpd_config_file :
      ensure => present,
      owner => $nagios_user,
      group => $nagios_group,
      content => template("hdp-nagios/nagios.conf.erb"),
      mode   => '0644'
    }

    hdp::directory { $nagios_config_dir:
      service_state => $service_state,
      force => true,
      owner => $nagios_user,
      group => $nagios_group
    }

    hdp::directory { $plugins_dir:
      service_state => $service_state,
      force => true
    }

    hdp::directory { $nagios_obj_dir:
      service_state => $service_state,
      force => true
    }

    hdp::directory_recursive_create { $nagios_pid_dir:
      service_state => $service_state,
      owner => $nagios_user,
      group => $nagios_group,
      ensure => "directory",
      mode => '0755',
      force => true
    }


    hdp::directory_recursive_create { $nagios_var_dir:
      service_state => $service_state,
      force => true,
      owner => $nagios_user,
      group => $nagios_group
    }
    
    hdp::directory_recursive_create { $check_result_path:
      service_state => $service_state,
      force => true,
      owner => $nagios_user,
      group => $nagios_group
    }

    hdp::directory_recursive_create { $nagios_rw_dir:
      service_state => $service_state,
      force => true,
      owner => $nagios_user,
      group => $nagios_group
    }
    
    hdp::directory { $nagios_log_dir:
      service_state => $service_state,
      force => true,
      owner => $nagios_user,
      group => $nagios_group,
      mode => '0755',
      override_owner => true
    }
    
    hdp::directory { $nagios_log_archives_dir:
      service_state => $service_state,
      force => true,
      owner => $nagios_user,
      group => $nagios_group,
      mode => '0755',
      override_owner => true
    }

    if ($service_state == 'installed_and_configured') {
      $webserver_state = 'restart'
    } elsif ($service_state == 'running') {
      $webserver_state = 'restart'
    } else {
      # We are never stopping httpd
      #$webserver_state = $service_state
    }

    class { 'hdp-monitor-webserver': service_state => $webserver_state}


    class { 'hdp-nagios::server::config': 
      notify => Class['hdp-nagios::server::services']
    }

    class { 'hdp-nagios::server::enable_snmp': }

    class { 'hdp-nagios::server::web_permisssions': }

    file { "$nagios_config_dir/command.cfg" :
      owner => $nagios_user,
      group => $nagios_group
    }

    class { 'hdp-nagios::server::services': ensure => $service_state}

    anchor{'hdp-nagios::server::begin':}
    anchor{'hdp-nagios::server::end':}

    Anchor['hdp-nagios::server::begin'] -> Class['hdp-nagios::server::packages'] -> File[$nagios_httpd_config_file] -> Class['hdp-nagios::server::enable_snmp']->
    Hdp::Directory[$nagios_config_dir] -> Hdp::Directory[$plugins_dir] -> Hdp::Directory_recursive_create[$nagios_pid_dir] ->
    Hdp::Directory[$nagios_obj_dir] -> Hdp::Directory_Recursive_Create[$nagios_var_dir] ->
    Hdp::Directory_Recursive_Create[$check_result_path] -> Hdp::Directory_Recursive_Create[$nagios_rw_dir] ->
    Hdp::Directory[$nagios_log_dir] -> Hdp::Directory[$nagios_log_archives_dir] ->
    Class['hdp-nagios::server::config'] -> Class['hdp-nagios::server::web_permisssions'] ->
    File["$nagios_config_dir/command.cfg"] -> Class['hdp-nagios::server::services'] -> Class['hdp-monitor-webserver'] -> Anchor['hdp-nagios::server::end']

  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-nagios::server::web_permisssions()
{
  $web_login = $hdp-nagios::params::nagios_web_login
  $htpasswd_cmd_os = $hdp::params::cmds[htpasswd]#[$hdp::params::hdp_os_type]


  if hdp_is_empty($hdp::params::cmds[htpasswd]) {
    hdp_fail("There is no htpasswd command mapping")
  }
  else {
    $htpasswd_cmd_by_os = $hdp::params::cmds[htpasswd]
  }

  if hdp_is_empty($htpasswd_cmd_by_os[$hdp::params::hdp_os_type]) {
    if hdp_is_empty($htpasswd_cmd_by_os['ALL']) {
      hdp_fail("There is no htpasswd command mapping")
    }
    else {
      $htpasswd_cmd = $htpasswd_cmd_by_os['ALL']
    }
  }
  else {
    $htpasswd_cmd = $htpasswd_cmd_by_os[$hdp::params::hdp_os_type]
  }

  $cmd = "$htpasswd_cmd -c -b  /etc/nagios/htpasswd.users ${web_login} ${hdp-nagios::params::nagios_web_password}"
  $test = "grep ${web_user} /etc/nagios/htpasswd.users"
  hdp::exec { $cmd :
    command => $cmd,
    unless => $test
  }

  file { "/etc/nagios/htpasswd.users" :
    owner => $hdp-nagios::params::nagios_user,
    group => $hdp-nagios::params::nagios_group,
    mode  => '0640'
  }

  if ($hdp::params::hdp_os_type == "suse") {
    $command = "usermod -G $hdp-nagios::params::nagios_group wwwrun"
  } else {
    $command = "usermod -a -G $hdp-nagios::params::nagios_group apache"
  }

  hdp::exec { "apache_permissions_htpasswd.users" :
    command => $command  
  }

  Hdp::Exec[$cmd] -> File["/etc/nagios/htpasswd.users"] -> Hdp::Exec["apache_permissions_htpasswd.users"]
}

class hdp-nagios::server::services($ensure)
{
   $pid_file = $hdp-nagios::params::nagios_pid_file
  
   if ($ensure == 'running') {
     $command = "service nagios start"
   } elsif ($ensure == 'stopped') {
     $command = "service nagios stop && rm -f ${pid_file}"
   }

   if ($ensure in ['running','stopped']) {
     exec { "nagios":
       command => $command,
       path    => "/usr/local/bin/:/bin/:/sbin/",      
     }
     anchor{'hdp-nagios::server::services::begin':} ->  Exec['nagios'] ->  anchor{'hdp-nagios::server::services::end':}	
   }
}

class hdp-nagios::server::enable_snmp() {

  exec { "enable_snmp":
    command => "service snmpd start; chkconfig snmpd on",
    path    => "/usr/local/bin/:/bin/:/sbin/",
  }

}
