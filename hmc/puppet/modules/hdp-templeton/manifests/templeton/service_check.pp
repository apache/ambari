class hdp-templeton::templeton::service_check()
{
  include hdp-templeton::params
  $smoke_test_user = $hdp::params::smokeuser

  $templeton_host = $hdp::params::templeton_server_host

  $smoke_shell_files = ['templetonSmoke.sh']

  anchor { 'hdp-templeton::templeton::service_check::begin':}

  hdp-templeton::smoke_shell_file { $smoke_shell_files: }

  anchor{ 'hdp-templeton::templeton::service_check::end':}
}

define hdp-templeton::smoke_shell_file()
{
  file { '/tmp/templetonSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-templeton/templetonSmoke.sh",
    mode => '0755'
  }

  exec { '/tmp/templetonSmoke.sh':
    command   => "sh /tmp/templetonSmoke.sh ${templeton_host} ${smoke_test_user}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/templetonSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
