class hdp-hbase::hbase::service_check() 
{
  $smoke_test_user = $hdp::params::smokeuser

  $output_file = "/apps/hbase/data/usertable"
  $conf_dir = $hdp::params::hbase_conf_dir

  $test_cmd = "fs -test -e ${output_file}" 
  
  anchor { 'hdp-hbase::hbase::service_check::begin':}

  file { '/tmp/hbaseSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-hbase/hbaseSmoke.sh",
    mode => '0755',
  }

  exec { '/tmp/hbaseSmoke.sh':
    command   => "su - ${smoke_test_user} -c 'hbase --config $conf_dir  shell /tmp/hbaseSmoke.sh'",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/hbaseSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    notify    => Hdp-hadoop::Exec-hadoop['hbase::service_check::test'],
    logoutput => "true"
  }

  hdp-hadoop::exec-hadoop { 'hbase::service_check::test':
    command     => $test_cmd,
    refreshonly => true,
    require     => Exec['/tmp/hbaseSmoke.sh'],
    before      => Anchor['hdp-hbase::hbase::service_check::end'] #TODO: remove after testing
  }
  
  anchor{ 'hdp-hbase::hbase::service_check::end':}
}
