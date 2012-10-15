class hdp2-ganglia(
  $service_state
)
{
  if (($service_state != 'no_op') or ($service_state != 'uninstalled')) {
    include hdp2-ganglia::params
    $gmetad_user = $hdp2-ganglia::params::gmetad_user
    $gmond_user = $hdp2-ganglia::params::gmond_user
  
    user { $gmond_user : shell => '/bin/bash'} #provision for nobody user
    if ( $gmetad_user != $gmond_user) {
      user { $gmetad_user : shell => '/bin/bash'} #provision for nobody user
    }
    anchor{'hdp2-ganglia::begin':} -> User<|title == $gmond_user or title == $gmetad_user|> ->  anchor{'hdp2-ganglia::end':}
  }
}

