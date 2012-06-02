class hdp-ganglia::server(
  $service_state = $hdp::params::cluster_service_state,
  $monitor_and_server_single_node = false,
  $opts = {}
) inherits  hdp-ganglia::params
{
  $ganglia_shell_cmds_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir
  $ganglia_conf_dir = $hdp-ganglia::params::ganglia_conf_dir
  $ganglia_runtime_dir = $hdp-ganglia::params::ganglia_runtime_dir

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    class { 'hdp-ganglia::server::packages':
      ensure => 'uninstalled'
      }

    hdp::directory { $ganglia_shell_cmds_dir:
      service_state => $service_state,
      force => true
    }

    hdp::directory { $ganglia_conf_dir:
      service_state => $service_state,
      force => true
    }

    hdp::directory { $ganglia_runtime_dir:
      service_state => $service_state,
      force => true
    }

    anchor { 'hdp-ganglia::server::begin': } -> Class['hdp-ganglia::server::packages'] -> Hdp::Directory[$ganglia_shell_cmds_dir] ->  Hdp::Directory[$ganglia_conf_dir] -> Hdp::Directory[$ganglia_runtime_dir] -> anchor { 'hdp-ganglia::server::end': }

   } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    if ($monitor_and_server_single_node == false) {
      include hdp-ganglia #note: includes the common package ganglia-monitor
    }

    class { 'hdp-ganglia::server::packages': }

    class { 'hdp-ganglia::config': 
     ganglia_server_host => $hdp::params::host_address,
     service_state       => $service_state
     }
   
    if ($hdp-ganglia::params::omit_hbase_master != true) {
      hdp-ganglia::config::generate_server { 'HDPHBaseMaster':
        ganglia_service => 'gmond'
      }
    }
    if ($hdp-ganglia::params::omit_jobtracker != true) {
      hdp-ganglia::config::generate_server { 'HDPJobTracker':
        ganglia_service => 'gmond'
      }
    }
    if ($hdp-ganglia::params::omit_namenode != true) {
      hdp-ganglia::config::generate_server { 'HDPNameNode':
        ganglia_service => 'gmond'
      }
    }
    if ($hdp-ganglia::params::omit_slaves != true) {
      hdp-ganglia::config::generate_server { 'HDPSlaves':
        ganglia_service => 'gmond'
      }
    }
    
    hdp-ganglia::config::generate_server { 'gmetad':
      ganglia_service => 'gmetad'
    }

    class { 'hdp-ganglia::server::services' : 
      service_state => $service_state,
      monitor_and_server_single_node => $monitor_and_server_single_node
    }

    #TODO: to get around anchor problems
    Class['hdp-ganglia'] -> Hdp-ganglia::Config::Generate_server<||>
    
    if ($monitor_and_server_single_node == false) {
      Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages']
    }

    anchor{'hdp-ganglia::server::begin':} -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] -> 
      Hdp-ganglia::Config::Generate_server<||> -> Class['hdp-ganglia::server::services'] -> anchor{'hdp-ganglia::server::end':}
   } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-ganglia::server::packages(
  $ensure = present
)
{
  hdp::package { ['ganglia-server','ganglia-gweb','ganglia-hdp-gweb-addons']:
    java_needed => 'false',
    ensure => $ensure
 } 
}

class hdp-ganglia::server::services(
  $service_state,
  $monitor_and_server_single_node
)
{
  if ($service_state in ['running','stopped']) {
    class { 'hdp-ganglia::service::gmetad': ensure => $service_state}

    anchor{'hdp-ganglia::server::services::begin':} -> Class['hdp-ganglia::service::gmetad'] -> anchor{'hdp-ganglia::server::services::end':} 
  
    if ($monitor_and_server_single_node == false) {
      class { 'hdp-ganglia::service::gmond': ensure => $service_state}
      class { 'hdp-ganglia::service::change_permission': ensure => $service_state }
      
      Anchor['hdp-ganglia::server::services::begin'] -> Class['hdp-ganglia::service::gmond'] -> Class['hdp-ganglia::service::change_permission'] -> Class['hdp-ganglia::service::gmetad'] 
    }
  }
}
