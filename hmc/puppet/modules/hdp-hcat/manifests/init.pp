class hdp-hcat(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp-hcat::params
{   
  if ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {
    hdp::package { 'hcat' :
      ensure => 'uninstalled', 
      size   => 32
    }
  } elsif ($service_state == 'installed_and_configured') {
    hdp::package { 'hcat' : 
      size => 32
    }

    hdp-hcat::configfile { 'hcat-env.sh':}
  
    Hdp::Package['hcat'] -> Hdp-hcat::Configfile<||> 
 } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp-hcat::configfile()
{
  hdp::configfile { "${hdp::params::hcat_conf_dir}/${name}":
    component => 'hcat'
  }
}
