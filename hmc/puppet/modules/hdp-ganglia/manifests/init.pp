class hdp-ganglia(
  $service_state
)
{
  if (($service_state != 'no_op') or ($service_state != 'uninstalled')) {
    include hdp-ganglia::params
    $gmetad_user = $hdp-ganglia::params::gmetad_user
    $gmond_user = $hdp-ganglia::params::gmond_user
  
    user { $gmond_user : shell => '/bin/bash'} #provision for nobody user
    if ( $gmetad_user != $gmond_user) {
      user { $gmetad_user : shell => '/bin/bash'} #provision for nobody user
    }
    anchor{'hdp-ganglia::begin':} -> User<|title == $gmond_user or title == $gmetad_user|> ->  anchor{'hdp-ganglia::end':}
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
