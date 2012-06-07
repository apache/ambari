class hdp-ganglia::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-ganglia::params
{
  $ganglia_shell_cmds_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir
  $ganglia_conf_dir = $hdp-ganglia::params::ganglia_conf_dir
  $ganglia_runtime_dir = $hdp-ganglia::params::ganglia_runtime_dir
  
  #note: includes the common package ganglia-monitor
  class { 'hdp-ganglia':
    service_state => $service_state
  }

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

    #top level no anchors needed
    Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Hdp::Directory[$ganglia_shell_cmds_dir] ->  Hdp::Directory[$ganglia_conf_dir] -> Hdp::Directory[$ganglia_runtime_dir]

  } elsif ($service_state in ['running','stopped','installed_and_configured']) {

    class { 'hdp-ganglia::server::packages': }

    class { 'hdp-ganglia::config': 
      ganglia_server_host => $hdp::params::host_address,
      service_state       => $service_state
    }

    class {'hdp-ganglia::server::config-gen': }   
    
    hdp-ganglia::config::generate_server { 'gmetad':
      ganglia_service => 'gmetad'
    }

    class { 'hdp-ganglia::server::services' : 
      service_state => $service_state,
      monitor_and_server_single_node => false
    }

    #top level no anchors needed
    Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] -> 
      Class['hdp-ganglia::server::config-gen'] -> Hdp-ganglia::Config::Generate_server['gmetad'] -> 
      Class['hdp-ganglia::server::services']
   } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-ganglia::server::packages(
  $ensure = present
)
{
  hdp::package { ['ganglia-server','ganglia-gweb','ganglia-hdp-gweb-addons']:
    ensure      => $ensure,
    java_needed => false
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

class hdp-ganglia::server::config-gen()
{
  anchor{'hdp-ganglia::server::config-gen::begin':} 

  if ($hdp-ganglia::params::nomit_hbase_master == true) {
    hdp-ganglia::config::generate_server { 'HDPHBaseMaster':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::server::config-gen::begin'],
      before  => Anchor['hdp-ganglia::server::config-gen::end']
    }
  }
  if ($hdp-ganglia::params::nomit_jobtracker == true) {
    hdp-ganglia::config::generate_server { 'HDPJobTracker':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::server::config-gen::begin'],
      before  => Anchor['hdp-ganglia::server::config-gen::end']
    }
  }
  if ($hdp-ganglia::params::nomit_namenode == true) {
    hdp-ganglia::config::generate_server { 'HDPNameNode':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::server::config-gen::begin'],
      before  => Anchor['hdp-ganglia::server::config-gen::end']
    }
  }
  if ($hdp-ganglia::params::nomit_slaves == true) {
    hdp-ganglia::config::generate_server { 'HDPSlaves':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::server::config-gen::begin'],
      before  => Anchor['hdp-ganglia::server::config-gen::end']
    }
  }
  anchor{'hdp-ganglia::server::config-gen::end':} 
}
