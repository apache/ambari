class hdp-nagios::server::packages(
  $service_state = $hdp::params::cluster_service_state
)
{
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
     hdp-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons']:
      ensure => 'uninstalled'
    }
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    case $hdp::params::hdp_os_type {
      centos6, rhel6: {
        hdp-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons']:
          ensure => 'present'
        }
      }
      default: {
        hdp-nagios::server::package { ['nagios-server','nagios-fping','nagios-plugins','nagios-addons','nagios-php-pecl-json']:
          ensure => 'present'
        }
      }
    }
  } 
  Hdp-nagios::Server::Package['nagios-plugins'] -> Hdp::Package['nagios-addons'] #other order produces package conflict

  anchor{'hdp-nagios::server::packages::begin':} -> Hdp-nagios::Server::Package<||> -> anchor{'hdp-nagios::server::packages::end':}
  Anchor['hdp-nagios::server::packages::begin'] -> Hdp::Package['nagios-addons'] -> Anchor['hdp-nagios::server::packages::end']
  Hdp-nagios::Server::Package['nagios-fping'] -> Hdp-nagios::Server::Package['nagios-plugins']
}


define hdp-nagios::server::package(
  $ensure = present
)
{
  hdp::package { $name: 
    ensure      => $ensure,
    java_needed => false
  }
}
