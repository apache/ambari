class hdp-hive::server(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits  hdp-hive::params
{ 

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 

    $hdp::params::service_exists['hdp-hive::server'] = true

    #installs package, creates user, sets configuration
    class{ 'hdp-hive' : 
      service_state => $service_state,
      server        => true
    } 
  
    Hdp-Hive::Configfile<||>{hive_server_host => $hdp::params::host_address}

    class { 'hdp-hive::hdfs-directories' : 
      service_state => $service_state
    }

    class { 'hdp-hive::service' :
      ensure => $service_state
    }
  
    #top level does not need anchors
    Class['hdp-hive'] -> Class['hdp-hive::hdfs-directories'] -> Class['hdp-hive::service']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-hive::hdfs-directories($service_state)
{
  $hive_user = $hdp::params::hive_user
 
  hdp-hadoop::hdfs::directory{ '/apps/hive/warehouse':
    service_state   => $service_state,
    owner            => $hive_user,
    mode             => '777',
    recursive_chmod  => true
  }  
  hdp-hadoop::hdfs::directory{ "/user/${hive_user}":
    service_state => $service_state,
    owner         => $hive_user
  }
}
