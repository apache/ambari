class hdp-hcat::hcat::service_check() 
{
  $smoke_test_user = $hdp::params::smokeuser
  $output_file = "/apps/hive/warehouse/hcatsmoke"

  $test_cmd = "fs -test -e ${output_file}" 
  
  anchor { 'hdp-hcat::hcat::service_check::begin':}

  file { '/tmp/hcatSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-hcat/hcatSmoke.sh",
    mode => '0755',
  }

  exec { '/tmp/hcatSmoke.sh':
    command   => "su - ${smoke_test_user} -c 'sh /tmp/hcatSmoke.sh'",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/hcatSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    notify    => Hdp-hadoop::Exec-hadoop['hcat::service_check::test'],
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'hcat::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Exec['/tmp/hcatSmoke.sh'],
    before      => Anchor['hdp-hcat::hcat::service_check::end'] 
  }
  
  anchor{ 'hdp-hcat::hcat::service_check::end':}
}
