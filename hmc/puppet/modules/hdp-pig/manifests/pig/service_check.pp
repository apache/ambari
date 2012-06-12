class hdp-pig::pig::service_check() 
{
  $smoke_test_user = $hdp::params::smokeuser
  $input_file = 'passwd'
  $output_file = "pigsmoke.out"

  $cleanup_cmd = "dfs -rmr ${output_file} ${input_file}"
  #cleanup put below to handle retries; if retrying there wil be a stale file that needs cleanup; exit code is fn of second command
  $create_file_cmd = "${cleanup_cmd}; hadoop dfs -put /etc/passwd ${input_file} " #TODO: inconsistent that second comamnd needs hadoop
  $test_cmd = "fs -test -e ${output_file}" 
  
  anchor { 'hdp-pig::pig::service_check::begin':}


  hdp-hadoop::exec-hadoop { 'pig::service_check::create_file':
    command   => $create_file_cmd,
    tries     => 3,
    try_sleep => 5,
    require   => Anchor['hdp-pig::pig::service_check::begin'],
    notify    => File['/tmp/pigSmoke.sh'],
    user      => $smoke_test_user
  }

  file { '/tmp/pigSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-pig/pigSmoke.sh",
    mode => '0755',
    require     => Hdp-hadoop::Exec-hadoop['pig::service_check::create_file']
  }

  exec { '/tmp/pigSmoke.sh':
    command   => "su - ${smoke_test_user} -c 'pig /tmp/pigSmoke.sh'",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/pigSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    notify    => Hdp-hadoop::Exec-hadoop['pig::service_check::test'],
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'pig::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Exec['/tmp/pigSmoke.sh'],
    before      => Anchor['hdp-pig::pig::service_check::end'], #TODO: remove after testing
    user      => $smoke_test_user
  }
  
  anchor{ 'hdp-pig::pig::service_check::end':}
}
