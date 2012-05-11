class hdp-ganglia::server(
  $service_state = $hdp::params::cluster_service_state,
  $monitor_and_server_single_node = false,
  $opts = {}
) inherits  hdp-ganglia::params
{
  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    class { 'hdp-ganglia::server::packages':
      ensure => 'uninstalled'
      }
   } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    if ($monitor_and_server_single_node == false) {
      include hdp-ganglia #note: includes the common package ganglia-monitor
    }

    class { 'hdp-ganglia::server::packages': }

    class { 'hdp-ganglia::config': ganglia_server_host => $hdp::params::host_address }

    hdp-ganglia::config::generate_server { ['HDPHBaseMaster','HDPJobTracker','HDPNameNode','HDPSlaves']:
      ganglia_service => 'gmond'
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

    Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] -> 
      Hdp-ganglia::Config::Generate_server<||> -> Class['hdp-ganglia::server::services']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-ganglia::server::packages(
  $ensure = present
)
{
  hdp::package { ['ganglia-server','ganglia-gweb','ganglia-hdp-gweb-addons']:
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
