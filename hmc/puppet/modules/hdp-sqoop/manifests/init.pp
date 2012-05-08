class hdp-sqoop(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp-sqoop::params
{ 
  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp::package { 'sqoop' :
      ensure => 'uninstalled',
      size   => 32
    }
  } elsif ($service_state == 'installed_and_configured') {

    hdp::package { 'sqoop' :
      size => 32
    }
    class { 'hdp-sqoop::mysql-connector': }
    if ($package_type == 'hdp') {
      hdp-sqoop::createsymlinks { ['/usr/lib/sqoop/conf']:}
    }

    hdp-sqoop::configfile { ['sqoop-env.sh']:}

    anchor { 'hdp-sqoop::begin': } -> Hdp::Package['sqoop'] -> Class['hdp-sqoop::mysql-connector'] -> Hdp-sqoop::Configfile<||> -> anchor { 'hdp-sqoop::end': }
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}


define hdp-sqoop::createsymlinks()
{
  file { '/usr/lib/sqoop/conf' :
    #ensure => directory,
    ensure => link,
    target => "/etc/sqoop"
  }

  file { '/etc/default/hadoop' :
    ensure => link,
    target => "/usr/bin/hadoop"
  }
}

### config files
define hdp-sqoop::configfile()
{
  hdp::configfile { "${hdp::params::sqoop_conf_dir}/${name}":
    component => 'sqoop'
  }
}



