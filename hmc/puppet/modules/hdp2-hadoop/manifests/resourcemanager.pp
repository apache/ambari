class hdp2-hadoop::resourcemanager(
  $service_state = $hdp2::params::cluster_service_state,
  $opts = {}
) inherits hdp2-hadoop::params
{
  $hdp2::params::service_exists['hdp2-hadoop::resourcemanager'] = true
  Hdp2-hadoop::Common<||>{service_states +> $service_state}
  Hdp2-hadoop::Package<||>{include_64_bit => true}
  Hdp2-hadoop::Configfile<||>{sizes +> 64}

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $yarn_user = $hdp2-hadoop::params::yarn_user
    $mapred_user = $hdp2-hadoop::params::mapred_user
    $yarn_nm_local_dirs = $hdp2-hadoop::params::yarn_nm_local_dirs 
  
    #adds package, users and directories, and common hadoop configs
    include hdp2-hadoop::initialize
     
    hdp2-hadoop::resourcemanager::create_local_dirs { $yarn_nm_local_dirs: 
      service_state => $service_state
    }

    #TODO: cleanup 
    Hdp2-Hadoop::Configfile<||>{yarn_rm_host => $hdp2::params::host_address}

    class { 'hdp2-hadoop::resourcemanager::hdfs-directory' : 
      service_state => $service_state 
    }

    #TODO: do we keep precondition here?
    if ($service_state == 'running' and $hdp2-hadoop::params::use_preconditions == true) {
      class { 'hdp2-hadoop::hdfs::service_check':
        before => Hdp2-hadoop::Service['resourcemanager'],
        require => Class['hdp2-hadoop']
      }
    }

    hdp2-hadoop::service{ 'resourcemanager':
      ensure       => $service_state,
      user         => $yarn_user
    }
  
    hdp2-hadoop::service{ 'historyserver':
      ensure         => $service_state,
      user           => $mapred_user,
      create_pid_dir => true,
      create_log_dir => true 
    }

    #top level does not need anchors
    Class['hdp2-hadoop'] -> Hdp2-hadoop::Service['resourcemanager'] -> Hdp2-hadoop::Service['historyserver']
    Class['hdp2-hadoop::resourcemanager::hdfs-directory'] -> Hdp2-hadoop::Service['resourcemanager']
    Hdp2-hadoop::Resourcemanager::Create_local_dirs<||> -> Hdp2-hadoop::Service['resourcemanager']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

define hdp2-hadoop::resourcemanager::create_local_dirs($service_state)
{
    $dirs = hdp_array_from_comma_list($name)
    hdp2::directory_recursive_create { $dirs :
      owner => $hdp2-hadoop::params::yarn_user,
      mode => '0755',
      service_state => $service_state,
      force => true
    }
}

class hdp2-hadoop::resourcemanager::hdfs-directory($service_state)
{
  hdp2-hadoop::hdfs::directory{ '/app-logs' :
    service_state => $service_state,
    owner         => $hdp2-hadoop::params::yarn_user,
    mode          => 1777
  }  
  hdp2-hadoop::hdfs::directory{ '/mapred' :
    service_state => $service_state,
    owner         => $hdp2-hadoop::params::mapred_user,
    mode          => 755
  }  
  hdp2-hadoop::hdfs::directory{ '/mapred/history' :
    service_state => $service_state,
    owner         => $hdp2-hadoop::params::mapred_user,
    mode          => 755
  }  
  hdp2-hadoop::hdfs::directory{ '/mapred/history/done_intermediate' :
    service_state => $service_state,
    owner         => $hdp2-hadoop::params::mapred_user,
    mode          => 1777
  }  
  hdp2-hadoop::hdfs::directory{ '/mapred/history/done' :
    service_state => $service_state,
    owner         => $hdp2-hadoop::params::mapred_user,
    mode          => 770
  }  
  Hdp2-hadoop::Hdfs::Directory['/app-logs'] -> Hdp2-hadoop::Hdfs::Directory['/mapred'] -> Hdp2-hadoop::Hdfs::Directory['/mapred/history'] -> Hdp2-hadoop::Hdfs::Directory['/mapred/history/done'] -> Hdp2-hadoop::Hdfs::Directory['/mapred/history/done_intermediate']
}

