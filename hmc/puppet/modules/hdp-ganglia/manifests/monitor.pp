class hdp-ganglia::monitor(
  $service_state = $hdp::params::cluster_service_state,
  $ganglia_server_host = undef,
  $opts = {}
) inherits hdp-ganglia::params
{
  if  ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {     

   hdp::package { 'ganglia-monitor':         
       ensure      => 'uninstalled', 
      java_needed => false      
   }

  } else {
    if ($hdp::params::service_exists['hdp-ganglia::server'] != true) {
      class { 'hdp-ganglia':
       service_state => $service_state
      }
    }

    hdp::package { 'ganglia-monitor': }

    if ($hdp::params::service_exists['hdp-ganglia::server'] != true) {
      class { 'hdp-ganglia::config': ganglia_server_host => $ganglia_server_host}
    }

    if ($hdp::params::component_exists['hdp-hadoop'] == true) {
      class { 'hdp-hadoop::enable-ganglia': }
    }

    if ($service_exists['hdp-hbase::master'] == true) {
      class { 'hdp-hbase::master::enable-ganglia': }
    }
  
    if ($service_exists['hdp-hbase::regionserver'] == true) {
      class { 'hdp-hbase::regionserver::enable-ganglia': }
    }

    class { 'hdp-ganglia::monitor::config-gen': }
  
    class { 'hdp-ganglia::service::gmond': ensure => $service_state}

    if ($hdp::params::service_exists['hdp-ganglia::server'] != true) {
      Class['hdp-ganglia'] -> Hdp::Package['ganglia-monitor'] -> Class['hdp-ganglia::config'] -> 
      Class['hdp-ganglia::monitor::config-gen'] -> Class['hdp-ganglia::service::gmond']
    } else {
      Hdp::Package['ganglia-monitor'] ->  Class['hdp-ganglia::monitor::config-gen'] -> Class['hdp-ganglia::service::gmond']
    }
  }
}


class hdp-ganglia::monitor::config-gen()
{

  $service_exists = $hdp::params::service_exists

  if ($service_exists['hdp-hadoop::namenode'] == true) {
    hdp-ganglia::config::generate_monitor { 'HDPNameNode':}
  }
  if ($service_exists['hdp-hadoop::jobtracker'] == true){
    hdp-ganglia::config::generate_monitor { 'HDPJobTracker':}
  }
  if ($service_exists['hdp-hbase::master'] == true) {
    hdp-ganglia::config::generate_monitor { 'HDPHBaseMaster':}
  }
  if ($service_exists['hdp-hadoop::datanode'] == true) {
    hdp-ganglia::config::generate_monitor { 'HDPSlaves':}
  }
  Hdp-ganglia::Config::Generate_monitor<||>{
    ganglia_service => 'gmond',
    role => 'monitor'
  }
   # 
  anchor{'hdp-ganglia::monitor::config-gen::begin':} -> Hdp-ganglia::Config::Generate_monitor<||> -> anchor{'hdp-ganglia::monitor::config-gen::end':}
}
