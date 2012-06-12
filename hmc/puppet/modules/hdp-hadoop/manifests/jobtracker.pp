class hdp-hadoop::jobtracker(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hadoop::params
{
  $hdp::params::service_exists['hdp-hadoop::jobtracker'] = true
  Hdp-hadoop::Common<||>{service_states +> $service_state}
  Hdp-hadoop::Package<||>{include_64_bit => true}
  Hdp-hadoop::Configfile<||>{sizes +> 64}

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $mapred_user = $hdp-hadoop::params::mapred_user
    $mapred_local_dir = $hdp-hadoop::params::mapred_local_dir 
  
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize
     
    hdp-hadoop::jobtracker::create_local_dirs { $mapred_local_dir: 
      service_state => $service_state
    }

    #TODO: cleanup 
    Hdp-Hadoop::Configfile<||>{jtnode_host => $hdp::params::host_address}

    class { 'hdp-hadoop::jobtracker::hdfs-directory' : 
      service_state => $service_state 
    }

    #TODO: do we keep precondition here?
    if ($service_state == 'running' and $hdp-hadoop::params::use_preconditions == true) {
      class { 'hdp-hadoop::hdfs::service_check':
        before => Hdp-hadoop::Service['jobtracker'],
        require => Class['hdp-hadoop']
      }
    }

    hdp-hadoop::service{ 'jobtracker':
      ensure       => $service_state,
      user         => $mapred_user
    }
  
    hdp-hadoop::service{ 'historyserver':
      ensure         => $service_state,
      user           => $mapred_user,
      create_pid_dir => false,
      create_log_dir => false
    }

    #top level does not need anchors
    Class['hdp-hadoop'] -> Hdp-hadoop::Service['jobtracker'] -> Hdp-hadoop::Service['historyserver']
    Class['hdp-hadoop::jobtracker::hdfs-directory'] -> Hdp-hadoop::Service['jobtracker']
    Hdp-hadoop::Jobtracker::Create_local_dirs<||> -> Hdp-hadoop::Service['jobtracker']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

define hdp-hadoop::jobtracker::create_local_dirs($service_state)
{
    $dirs = hdp_array_from_comma_list($name)
    hdp::directory_recursive_create { $dirs :
      owner => $hdp-hadoop::params::mapred_user,
      mode => '0755',
      service_state => $service_state,
      force => true
    }
}

class hdp-hadoop::jobtracker::hdfs-directory($service_state)
{
  hdp-hadoop::hdfs::directory{ '/mapred' :
    service_state => $service_state,
    owner         => $hdp-hadoop::params::mapred_user
  }  
   hdp-hadoop::hdfs::directory{ '/mapred/system' :
    service_state => $service_state,
    owner         => $hdp-hadoop::params::mapred_user
  }  
  Hdp-hadoop::Hdfs::Directory['/mapred'] -> Hdp-hadoop::Hdfs::Directory['/mapred/system'] 
}

