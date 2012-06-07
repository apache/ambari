class hdp-hadoop::snamenode(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hadoop::params  
{
  $hdp::params::service_exists['hdp-hadoop::snamenode'] = true

  Hdp-hadoop::Common<||>{service_states +> $service_state}
  Hdp-hadoop::Package<||>{include_64_bit => true}
  Hdp-hadoop::Configfile<||>{sizes +> 64}

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    $fs_checkpoint_dir = $hdp-hadoop::params::fs_checkpoint_dir
  
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize
 
    Hdp-Hadoop::Configfile<||>{snamenode_host => $hdp::params::host_address}
  
    hdp-hadoop::snamenode::create_name_dirs { $fs_checkpoint_dir: 
      service_state => $service_state
    }
    
    if ($hdp::params::service_exists['hdp-hadoop::namenode'] == true) {
      $create_pid_dir = false
      $create_log_dir = false
    } else {
      $create_pid_dir = true
      $create_log_dir = true
    }
    
    hdp-hadoop::service{ 'secondarynamenode':
      ensure         => $service_state,
      user           => $hdp-hadoop::params::hdfs_user,
      create_pid_dir => $create_pid_dir,
      create_log_dir => $create_log_dir
    }
  
    #top level does not need anchors
    Class['hdp-hadoop'] -> Hdp-hadoop::Service['secondarynamenode']
    Hdp-hadoop::Namenode::Create_name_dirs<||> -> Hdp-hadoop::Service['secondarynamenode']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

define hdp-hadoop::snamenode::create_name_dirs($service_state)
{
 if ($hdp::params::service_exists['hdp-hadoop::namenode'] != true) {
   $dirs = hdp_array_from_comma_list($name)
   hdp::directory_recursive_create { $dirs :
     owner => $hdp-hadoop::params::hdfs_user,
     mode => '0755',
     service_state => $service_state,
     force => true
   }
  }
}
