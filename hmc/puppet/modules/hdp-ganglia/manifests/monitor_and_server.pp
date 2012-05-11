class hdp-ganglia::monitor_and_server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-ganglia::params
{

  class { 'hdp-ganglia': }
  
  class { 'hdp-ganglia::server':
    service_state                  => $service_state,
    opts                           => $opts,
    monitor_and_server_single_node => true
  }

  class { 'hdp-ganglia::monitor':
    service_state                  => $service_state,
    opts                           => $opts,
    monitor_and_server_single_node => true
  }

  class { 'hdp-ganglia::service::gmond': 
    ensure => $service_state
  }

  class { 'hdp-ganglia::service::change_permission':
    ensure => $service_state
  }

  anchor{'hdp-ganglia::monitor_and_server::begin':} -> Class['hdp-ganglia'] -> Class['hdp-ganglia::server'] ->
    Class['hdp-ganglia::monitor'] -> Class['hdp-ganglia::service::gmond'] -> Class['hdp-ganglia::service::change_permission'] -> anchor{'hdp-ganglia::monitor_and_server::end':}
}
