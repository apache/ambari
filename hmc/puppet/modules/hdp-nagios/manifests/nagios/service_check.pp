class hdp-nagios::nagios::service_check() 
{
  
  anchor { 'hdp-nagios::nagios::service_check::begin':}

  exec { 'nagios':
    command   => "/etc/init.d/nagios status | grep 'is running'",
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    before      => Anchor['hdp-nagios::nagios::service_check::end'],
    logoutput => "true"
  }

  anchor{ 'hdp-nagios::nagios::service_check::end':}
}
