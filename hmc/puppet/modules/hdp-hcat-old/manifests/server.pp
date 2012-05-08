class hdp-hcat::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-hcat::params
{ 
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured']) { 
    class{ 'hdp-hcat' : server => true} #installs package, creates user, sets configuration
  
    Hdp-Hcat::Configfile<||>{hcat_server_host => $hdp::params::host_address}

    class { 'hdp-hcat::hdfs-directories' : 
      service_state => $service_state
    }

    class { 'hdp-hcat::service' :
      ensure => $service_state
    }
  
    #top level does not need anchors
    Class['hdp-hcat'] -> Class['hdp-hcat::hdfs-directories'] -> Class['hdp-hcat::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-hcat::hdfs-directories($service_state)
{
  $hcat_user = $hdp::params::hcat_user
 
  hdp-hadoop::hdfs::directory{ '/apps/hive/warehouse':
    service_state   => $service_state,
    owner            => $hcat_user,
    mode             => '770',
    recursive_chmod  => true
  }  
  hdp-hadoop::hdfs::directory{ "/usr/${hcat_user}":
    service_state => $service_state,
    owner         => $hcat_user
  }
}
