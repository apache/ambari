class hdp-zookeeper::zookeeper::service_check()
{
  include hdp-zookeeper::params
  $conf_dir = $hdp-zookeeper::params::conf_dir
  $smoke_script = $hdp::params::zk_smoke_test_script

  $smoke_test_user = $hdp::params::smokeuser
  $zookeeper_smoke_shell_files = ['zkService.sh']

  anchor { 'hdp-zookeeper::zookeeper::service_check::begin':}

  hdp-zookeeper::zookeeper_smoke_shell_file { $zookeeper_smoke_shell_files: }

  anchor{ 'hdp-zookeeper::zookeeper::service_check::end':}
}

define hdp-zookeeper::zookeeper_smoke_shell_file()
{
  file { '/tmp/zkService.sh':
    ensure => present,
    source => "puppet:///modules/hdp-zookeeper/zkService.sh",
    mode => '0755'
  }

  exec { '/tmp/zkService.sh':
    command   => "sh /tmp/zkService.sh ${smoke_script} ${smoke_test_user} ${conf_dir}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/zkService.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
