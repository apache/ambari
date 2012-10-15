class hdp2-ganglia::hdp-gmond::service_check() 
{
  
  anchor { 'hdp2-ganglia::hdp-gmond::service_check::begin':}

  exec { 'hdp-gmond':
    command   => "/etc/init.d/hdp-gmond status | grep -v failed",
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    before      => Anchor['hdp2-ganglia::hdp-gmond::service_check::end'],
    logoutput => "true"
  }

  anchor{ 'hdp2-ganglia::hdp-gmond::service_check::end':}
}
