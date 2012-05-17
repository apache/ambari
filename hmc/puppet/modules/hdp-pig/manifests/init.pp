class hdp-pig(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp-pig::params
{  
  $pig_config_dir = $hdp-pig::params::pig_conf_dir
 
  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp::package { 'pig' :
      ensure => 'uninstalled',
      size   => 32
    }
    hdp::directory_recursive_create { $pig_config_dir:
      service_state => $service_state,
      force => true
    }
   anchor { 'hdp-pig::begin': } -> Hdp::Package['pig'] -> Hdp::Directory_recursive_create[$pig_conf_dir] -> anchor { 'hdp-pig::end': }

  } elsif ($service_state == 'installed_and_configured') {
    hdp::package { 'pig' : 
      size => 32
    }

    hdp::directory { $pig_config_dir:
      service_state => $service_state,
      force => true
    }

    hdp-pig::configfile { ['pig-env.sh','pig.properties','log4j.properties']:}
  
    anchor { 'hdp-pig::begin': } -> Hdp::Package['pig'] -> Hdp::Directory[$pig_conf_dir] -> Hdp-pig::Configfile<||> -> anchor { 'hdp-pig::end': }
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp-pig::configfile()
{
  hdp::configfile { "${hdp::params::pig_conf_dir}/${name}":
    component => 'pig'
  }
}



