class hdp-hive::hive::service_check() 
{
  $unique = hdp_unique_id_and_date()
  $smoke_test_user = $hdp::params::smokeuser
  $output_file = "/apps/hive/warehouse/hivesmoke${unique}"

  $test_cmd = "fs -test -e ${output_file}" 
  
  anchor { 'hdp-hive::hive::service_check::begin':}

  file { '/tmp/hiveSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-hive/hiveSmoke.sh",
    mode => '0755',
  }

  exec { '/tmp/hiveSmoke.sh':
    command   => "su - ${smoke_test_user} -c 'sh /tmp/hiveSmoke.sh hivesmoke${unique}'",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/hiveSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    notify    => Hdp-hadoop::Exec-hadoop['hive::service_check::test'],
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'hive::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Exec['/tmp/hiveSmoke.sh'],
    before      => Anchor['hdp-hive::hive::service_check::end'] 
  }
  
  anchor{ 'hdp-hive::hive::service_check::end':}
}
