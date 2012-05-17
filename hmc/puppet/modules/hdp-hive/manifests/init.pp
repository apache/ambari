class hdp-hive(
  $service_state,
  $server = false
) 
{
  include hdp-hive::params

  $hive_user = $hdp-hive::params::hive_user
  $hive_config_dir = $hdp-hive::params::hive_conf_dir

  anchor { 'hdp-hive::begin': }
  anchor { 'hdp-hive::end': } 

  if ($service_state == 'uninstalled') {
    hdp::package { 'hive' : 
      ensure => 'uninstalled'
    }

    hdp::directory { $hive_config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp-hive::begin'] -> Hdp::Package['hive'] -> Hdp::Directory[$hive_config_dir] ->  Anchor['hdp-hive::end']

  } else {
    hdp::package { 'hive' : }
    if ($server == true ) {
      class { 'hdp-hive::mysql-connector': }
    }
  
    hdp::user{ $hive_user:}
  
    hdp::directory { $hive_config_dir: 
      service_state => $service_state,
      force => true
    }

    hdp-hive::configfile { ['hive-env.sh','hive-site.xml']: }
  
    Anchor['hdp-hive::begin'] -> Hdp::Package['hive'] -> Hdp::User[$hive_user] ->  
     Hdp::Directory[$hive_config_dir] -> Hdp-hive::Configfile<||> ->  Anchor['hdp-hive::end']

     if ($server == true ) {
       Hdp::Package['hive'] -> Hdp::User[$hive_user] -> Class['hdp-hive::mysql-connector'] -> Anchor['hdp-hive::end']
    }
  }
}

### config files
define hdp-hive::configfile(
  $mode = undef,
  $hive_server_host = undef
) 
{
  hdp::configfile { "${hdp-hive::params::hive_conf_dir}/${name}":
    component        => 'hive',
    owner            => $hdp-hive::params::hive_user,
    mode             => $mode,
    hive_server_host => $hive_server_host 
  }
}
