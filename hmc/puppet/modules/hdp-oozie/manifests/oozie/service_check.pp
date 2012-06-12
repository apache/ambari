class hdp-oozie::oozie::service_check()
{
  include hdp-oozie::params

  $smoke_shell_files = ['oozieSmoke.sh']
  anchor { 'hdp-oozie::oozie::service_check::begin':}

  hdp-oozie::smoke_shell_file { $smoke_shell_files: }

  anchor{ 'hdp-oozie::oozie::service_check::end':}
}

define hdp-oozie::smoke_shell_file()
{
  $smoke_test_user = $hdp::params::smokeuser
  $conf_dir = $hdp::params::oozie_conf_dir
  $hadoopconf_dir = $hdp::params::hadoop_conf_dir 

  file { '/tmp/oozieSmoke.sh':
    ensure => present,
    source => "puppet:///modules/hdp-oozie/oozieSmoke.sh",
    mode => '0755'
  }

  exec { '/tmp/oozieSmoke.sh':
    command   => "sh /tmp/oozieSmoke.sh ${conf_dir} ${hadoopconf_dir} ${smoke_test_user}",
    tries     => 3,
    try_sleep => 5,
    require   => File['/tmp/oozieSmoke.sh'],
    path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    logoutput => "true"
  }
}
