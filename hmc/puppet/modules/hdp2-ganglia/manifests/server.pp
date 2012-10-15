class hdp2-ganglia::server(
  $service_state = $hdp2::params::cluster_service_state,
  $opts = {}
) inherits  hdp2-ganglia::params
{
  $hdp2::params::service_exists['hdp2-ganglia::server'] = true

  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {

   class { 'hdp2-ganglia::server::packages':
      ensure => 'uninstalled'
   }

  } else {
  class { 'hdp2-ganglia':
    service_state => $service_state
  }

  class { 'hdp2-ganglia::server::packages': }

  class { 'hdp2-ganglia::config': 
    ganglia_server_host => $hdp2::params::host_address,
    service_state       => $service_state 
  }

  hdp2-ganglia::config::generate_server { ['HDPNameNode','HDPResourceManager','HDPHBaseMaster','HDPSlaves']:
    ganglia_service => 'gmond',
    role => 'server'
  }
  hdp2-ganglia::config::generate_server { 'gmetad':
    ganglia_service => 'gmetad',
    role => 'server'
  }

  class { 'hdp2-ganglia::server::gmetad': ensure => $service_state}

  class { 'hdp2-ganglia::service::change_permission': ensure => $service_state }

  #top level does not need anchors
  Class['hdp2-ganglia'] -> Class['hdp2-ganglia::server::packages'] -> Class['hdp2-ganglia::config'] -> 
    Hdp2-ganglia::Config::Generate_server<||> -> Class['hdp2-ganglia::server::gmetad'] -> Class['hdp2-ganglia::service::change_permission']
 }
}

class hdp2-ganglia::server::packages(
  $ensure = present 
)
{
  hdp2::package { ['ganglia-server','ganglia-gweb','ganglia-hdp-gweb-addons']: 
    ensure      => $ensure,
    java_needed => false  
  } 
}


class hdp2-ganglia::service::change_permission(
  $ensure
)
{
  if ($ensure == 'running' or $ensure == 'installed_and_configured') {
    hdp2::directory_recursive_create { '/var/lib/ganglia/dwoo' :
      mode => '0777'
      }
  }
}

class hdp2-ganglia::server::gmetad(
  $ensure
)
{
  if ($ensure == 'running') {
    $command = "service hdp-gmetad start >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1"
   } elsif  ($ensure == 'stopped') {
    $command = "service hdp-gmetad stop >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1"
  }
  if ($ensure == 'running' or $ensure == 'stopped') {
    hdp::exec { "hdp-gmetad service" :
      command => "$command",
      unless => "/bin/ps auwx | /bin/grep [g]metad",
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    }
  }
}
