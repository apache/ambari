class hdp-sqoop::sqoop::service_check() 
{
  $smoke_test_user = $hdp::params::smokeuser
  
  anchor { 'hdp-sqoop::sqoop::service_check::begin':}

  exec { 'sqoop_smoke':
    command   => "su - ${smoke_test_user} -c 'sqoop version'",
    tries     => 3,
    try_sleep => 5,
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }

  anchor{ 'hdp-sqoop::sqoop::service_check::end':}
}
