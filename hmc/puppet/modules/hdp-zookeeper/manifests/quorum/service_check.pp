class hdp-zookeeper::quorum::service_check()
{
  include hdp-zookeeper::params
  $conf_dir = $hdp-zookeeper::params::conf_dir

  $smoke_test_user = $hdp::params::smokeuser

  $smoke_script = $hdp::params::zk_smoke_test_script
  $quorum_smoke_shell_files = ['zkSmoke.sh']

  anchor { 'hdp-zookeeper::quorum::service_check::begin':}

  hdp-zookeeper::quorum_smoke_shell_file { $quorum_smoke_shell_files: }

  anchor{ 'hdp-zookeeper::quorum::service_check::end':}
}

define hdp-zookeeper::quorum_smoke_shell_file()
{
  file { '/tmp/zkSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-zookeeper/zkSmoke.sh",
    mode => '0755'
  }

  exec { '/tmp/zkSmoke.sh':
    command   => "sh /tmp/zkSmoke.sh ${smoke_script} ${smoke_test_user} ${conf_dir}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/zkSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
