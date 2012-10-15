class hdp2-hadoop::nodemanager(
  $service_state = $hdp2::params::cluster_service_state,
  $opts = {}
) inherits hdp2-hadoop::params
{
  $hdp2::params::service_exists['hdp2-hadoop::nodemanager'] = true

  Hdp2-hadoop::Common<||>{service_states +> $service_state}

  if ($hdp2::params::use_32_bits_on_slaves == true) {
    Hdp2-hadoop::Package<||>{include_32_bit => true}
    Hdp2-hadoop::Configfile<||>{sizes +> 32}
  } else {
    Hdp2-hadoop::Package<||>{include_64_bit => true}
    Hdp2-hadoop::Configfile<||>{sizes +> 64}
  }

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 
    $yarn_nm_local_dirs = $hdp2-hadoop::params::yarn_nm_local_dirs
  
    #adds package, users and directories, and common hadoop configs
    include hdp2-hadoop::initialize
  
    hdp2-hadoop::nodemanager::create_local_dirs { $yarn_nm_local_dirs: 
      service_state => $service_state
    }
    
    if ($hdp2::params::service_exists['hdp2-hadoop::resourcemanager'] == true) {
      $create_pid_dir = false
      $create_log_dir = false
    } else {
      $create_pid_dir = true
      $create_log_dir = true
    }

    hdp2-hadoop::service{ 'nodemanager':
      ensure => $service_state,
      user   => $hdp2-hadoop::params::yarn_user,
      create_pid_dir => $create_pid_dir,
      create_log_dir => $create_log_dir
    }
  
    #top level does not need anchors
    Class['hdp2-hadoop'] -> Hdp2-hadoop::Service['nodemanager']
    Hdp2-hadoop::Nodemanager::Create_local_dirs<||> -> Hdp2-hadoop::Service['nodemanager']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

define hdp2-hadoop::nodemanager::create_local_dirs($service_state)
{
  if ($hdp2::params::service_exists['hdp2-hadoop::resourcemanager'] != true) {
    $dirs = hdp_array_from_comma_list($name)
    hdp2::directory_recursive_create { $dirs :
      owner => $hdp2-hadoop::params::yarn_user,
      mode => '0755',
      service_state => $service_state,
      force => true
    }
  }
}
