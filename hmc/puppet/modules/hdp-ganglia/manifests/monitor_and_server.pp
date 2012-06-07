class hdp-ganglia::monitor_and_server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-ganglia::params
{
  $ganglia_shell_cmds_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir
  $ganglia_conf_dir = $hdp-ganglia::params::ganglia_conf_dir
  $ganglia_runtime_dir = $hdp-ganglia::params::ganglia_runtime_dir

  #note: includes the common package ganglia-monitor
  class { 'hdp-ganglia':
    service_state => $service_state
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
    class { 'hdp-ganglia::server::packages':
      ensure => 'uninstalled'
      }

    hdp::directory { [$ganglia_conf_dir,$ganglia_runtime_dir]:
      service_state => $service_state,
      force => true
    }
    
    class { 'hdp-ganglia::config':
      service_state => $service_state
    }

    Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> 
      Hdp::Directory[$ganglia_conf_dir] -> Hdp::Directory[$ganglia_runtime_dir] ->
      Class['hdp-ganglia::config']
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    class { 'hdp-ganglia::server::packages': }

    class { 'hdp-ganglia::config': 
     ganglia_server_host => $hdp::params::host_address,
     service_state       => $service_state
     }

    class {'hdp-ganglia::monitor::config-gen': }      

    class {'hdp-ganglia::server::config-gen': }      
    
    hdp-ganglia::config::generate_server { 'gmetad':
      ganglia_service => 'gmetad'
    }

    class { 'hdp-ganglia::service::gmond': 
      ensure => $service_state
    }

    class { 'hdp-ganglia::server::services' : 
      service_state => $service_state,
      monitor_and_server_single_node => true
    }

    class { 'hdp-ganglia::service::change_permission':
      ensure => $service_state
    }

    #top level no anchors needed
    Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] -> 
      Class['hdp-ganglia::monitor::config-gen'] -> Class['hdp-ganglia::server::config-gen'] -> Hdp-ganglia::Config::Generate_server['gmetad'] ->
      Class['hdp-ganglia::service::gmond'] -> Class['hdp-ganglia::server::services'] ->
      Class['hdp-ganglia::service::change_permission']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
