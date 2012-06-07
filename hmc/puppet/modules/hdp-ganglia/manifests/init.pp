class hdp-ganglia(
  $service_state
)
{
  if ($service_state != 'no_op') {
    include hdp-ganglia::params
    $gmetad_user = $hdp-ganglia::params::gmetad_user
    $gmond_user = $hdp-ganglia::params::gmond_user
    $ganglia_config_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir

    anchor{'hdp-ganglia::begin':} 
    if ($service_state == 'uninstalled') {
      hdp::package { 'ganglia-monitor' :
        ensure => 'uninstalled'
      }
      package { 'libganglia' :
        ensure => 'purged'
      }
      Anchor['hdp-ganglia::begin'] -> Hdp::Package['ganglia-monitor'] -> Package['libganglia'] -> Anchor['hdp-ganglia::end']

    } elsif ($service_state in ['running','installed_and_configured','stopped']) {
      user { $gmond_user : shell => '/bin/bash'} #provision for nobody user
      if ( $gmetad_user != $gmond_user) {
        user { $gmetad_user : shell => '/bin/bash'} #provision for nobody user
      }

      hdp::package { 'ganglia-monitor':
        java_needed => false
      }

      Anchor['hdp-ganglia::begin'] -> User<|title == $gmond_user or title == $gmetad_user|> ->  
        Hdp::Package['ganglia-monitor'] -> Anchor['hdp-ganglia::end']
    }
    anchor{'hdp-ganglia::end':}
  }
}

class hdp-ganglia::service::gmond(
  $ensure
  )
{
  if ($ensure == 'running' or $ensure == 'stopped') {
    service { 'hdp-gmond':
      ensure     => $ensure,
      hasstatus  => false,
      hasrestart => true,
      status    => '/bin/ps auwx | /bin/grep [g]mond'
    }
  }
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


class hdp-ganglia::service::gmetad(
  $ensure
)
{
  if ($ensure == 'running' or $ensure == 'stopped') {
    service { 'hdp-gmetad':
      ensure     => $ensure,
      hasstatus  => false,
      hasrestart => true,
      status    => '/bin/ps auwx | /bin/grep [g]metad'
    }
  }
}
