class hdp-ganglia::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-ganglia::params
{
  $hdp::params::service_exists['hdp-ganglia::server'] = true

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {

   class { 'hdp-ganglia::server::packages':
      ensure => 'uninstalled'
   }

  } else {
  class { 'hdp-ganglia':
    service_state => $service_state
  }

  class { 'hdp-ganglia::server::packages': }

  class { 'hdp-ganglia::config': 
    ganglia_server_host => $hdp::params::host_address,
    service_state       => $service_state 
  }

  hdp-ganglia::config::generate_server { ['HDPHBaseMaster','HDPJobTracker','HDPNameNode','HDPSlaves']:
    ganglia_service => 'gmond',
    role => 'server'
  }
  hdp-ganglia::config::generate_server { 'gmetad':
    ganglia_service => 'gmetad',
    role => 'server'
  }

  class { 'hdp-ganglia::server::services' : service_state => $service_state}

  class { 'hdp-ganglia::service::change_permission': ensure => $service_state }

  #top level does not need anchors
  Class['hdp-ganglia'] -> Class['hdp-ganglia::server::packages'] -> Class['hdp-ganglia::config'] -> 
    Hdp-ganglia::Config::Generate_server<||> -> Class['hdp-ganglia::server::services'] -> Class['hdp-ganglia::service::change_permission']
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

class hdp-ganglia::server::services($service_state)
{
  class { 'hdp-ganglia::service::gmetad': ensure => $service_state}
  anchor{'hdp-ganglia::server::services::begin':} -> Class['hdp-ganglia::service::gmetad'] -> anchor{'hdp-ganglia::server::services::end':}
}

class hdp-ganglia::service::change_permission(
  $ensure
)
{
  if ($ensure == 'running' or $ensure == 'installed_and_configured') {
    hdp::directory_recursive_create { '/var/lib/ganglia/dwoo' :
      mode => '0777'
      }
  }
}
