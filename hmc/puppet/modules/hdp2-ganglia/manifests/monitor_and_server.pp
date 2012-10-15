class hdp2-ganglia::monitor_and_server(
  $service_state = $hdp2::params::cluster_service_state,
  $opts = {}
) inherits hdp2-ganglia::params
{
  $ganglia_shell_cmds_dir = $hdp2-ganglia::params::ganglia_shell_cmds_dir
  $ganglia_conf_dir = $hdp2-ganglia::params::ganglia_conf_dir
  $ganglia_runtime_dir = $hdp2-ganglia::params::ganglia_runtime_dir

  #note: includes the common package ganglia-monitor
  class { 'hdp2-ganglia':
    service_state => $service_state
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
    class { 'hdp2-ganglia::server::packages':
      ensure => 'uninstalled'
      }

    hdp::directory { [$ganglia_conf_dir,$ganglia_runtime_dir]:
      service_state => $service_state,
      force => true
    }
    
    class { 'hdp2-ganglia::config':
      service_state => $service_state
    }

    Class['hdp2-ganglia'] -> Class['hdp2-ganglia::server::packages'] -> 
      Hdp2::Directory[$ganglia_conf_dir] -> Hdp2::Directory[$ganglia_runtime_dir] ->
      Class['hdp2-ganglia::config']
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    class { 'hdp2-ganglia::server::packages': }

    class { 'hdp2-ganglia::config': 
     ganglia_server_host => $hdp2::params::host_address,
     service_state       => $service_state
     }

    class {'hdp2-ganglia::monitor::config-gen': }      

    class {'hdp2-ganglia::server::config-gen': }      
    
    hdp2-ganglia::config::generate_server { 'gmetad':
      ganglia_service => 'gmetad'
    }

    class { 'hdp2-ganglia::service::gmond': 
      ensure => $service_state
    }

    class { 'hdp2-ganglia::server::services' : 
      service_state => $service_state,
      monitor_and_server_single_node => true
    }

    class { 'hdp2-ganglia::service::change_permission':
      ensure => $service_state
    }

    #top level no anchors needed
    Class['hdp2-ganglia'] -> Class['hdp2-ganglia::server::packages'] -> Class['hdp2-ganglia::config'] -> 
      Class['hdp2-ganglia::monitor::config-gen'] -> Class['hdp2-ganglia::server::config-gen'] -> Hdp2-ganglia::Config::Generate_server['gmetad'] ->
      Class['hdp2-ganglia::service::gmond'] -> Class['hdp2-ganglia::server::services'] ->
      Class['hdp2-ganglia::service::change_permission']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
