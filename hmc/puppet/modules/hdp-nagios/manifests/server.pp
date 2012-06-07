class hdp-nagios::server(
  $service_state = $hdp::params::cluster_service_state
) inherits hdp-nagios::params
{

  $nagios_config_dir = $hdp-nagios::params::conf_dir
  $plugins_dir = $hdp-nagios::params::plugins_dir
  $nagios_obj_dir = $hdp-nagios::params::nagios_obj_dir

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
    class { 'hdp-nagios::server::packages' : 
      service_state => uninstalled
    }

    if ($wipeoff_data == true) {
      hdp::exec { "rm -f /var/nagios/rw/nagios.cmd" :
        command => "rm -f /var/nagios/rw/nagios.cmd",
        unless => "test ! -e  /var/nagios/rw/nagios.cmd"
      }
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
    if ($wipeoff_data == true) {
     Class['hdp-nagios::server::packages'] -> Exec['rm -f /var/nagios/rw/nagios.cmd'] -> Hdp::Directory[$nagios_config_dir] -> Hdp::Directory[$plugins_dir] -> Hdp::Directory[$nagios_obj_dir]
    } else {
     Class['hdp-nagios::server::packages'] -> Hdp::Directory[$nagios_config_dir] -> Hdp::Directory[$plugins_dir] -> Hdp::Directory[$nagios_obj_dir]
    }
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    class { 'hdp-nagios::server::packages' : }

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


    class { 'hdp-nagios::server::config': 
      notify => Class['hdp-nagios::server::services']
    }

    class { 'hdp-nagios::server::web_permisssions': }

    class { 'hdp-nagios::server::services': ensure => $service_state}

    Class['hdp-nagios::server::packages'] -> Hdp::Directory[$nagios_config_dir] -> Hdp::Directory[$plugins_dir] -> Hdp::Directory[$nagios_obj_dir] -> Class['hdp-nagios::server::config'] -> 
    Class['hdp-nagios::server::web_permisssions'] -> Class['hdp-nagios::server::services']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-nagios::server::web_permisssions()
{
  $web_login = $hdp-nagios::params::nagios_web_login
  $cmd = "htpasswd -c -b  /etc/nagios/htpasswd.users ${web_login} ${hdp-nagios::params::nagios_web_password}"
  $test = "grep ${web_user} /etc/nagios/htpasswd.users"
  hdp::exec { $cmd :
    command => $cmd,
    unless => $test
  }
}

class hdp-nagios::server::services($ensure)
{
  if ($ensure in ['running','stopped']) {
    service { 'nagios': ensure => $ensure}
    anchor{'hdp-nagios::server::services::begin':} ->  Service['nagios'] ->  anchor{'hdp-nagios::server::services::end':}
  }
}
