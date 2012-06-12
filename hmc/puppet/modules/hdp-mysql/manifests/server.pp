class hdp-mysql::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-mysql::params
{ 
  if ($service_state in ['no_op','uninstalled']) {
   } elsif ($service_state in ['running','stopped','installed_and_configured']) {
   
    $db_user = $hdp-mysql::params::db_user
    $db_pw = $hdp-mysql::params::db_pw
    $db_name = $hdp-mysql::params::db_name
    $host = $hdp::params::hive_mysql_host 

    anchor { 'hdp-mysql::server::begin':}

    hdp::package { 'mysql' :
      size   => 32,
      require   => Anchor['hdp-mysql::server::begin']
    }

    hdp::exec { 'mysqld start':
        command => '/etc/init.d/mysqld start',
        unless  => '/etc/init.d/mysqld status',
        require => Hdp::Package['mysql'],
        notify  => File['/tmp/startMysql.sh']
    }

    file { '/tmp/startMysql.sh':
      ensure => present,
      source => "puppet:///modules/hdp-mysql/startMysql.sh",
      mode => '0755',
      require => Hdp::Exec['mysqld start'],
      notify => Exec['/tmp/startMysql.sh']
    }

    exec { '/tmp/startMysql.sh':
      command   => "sh /tmp/startMysql.sh ${db_user} ${db_pw} ${host}",
      tries     => 3,
      try_sleep => 5,
      require   => File['/tmp/startMysql.sh'],
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      notify   => Anchor['hdp-mysql::server::end'],
      logoutput => "true"
    }

    anchor { 'hdp-mysql::server::end':}

  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
