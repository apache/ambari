class hdp2-ganglia::monitor(
  $service_state = $hdp2::params::cluster_service_state,
  $ganglia_server_host = undef,
  $opts = {}
) inherits hdp2-ganglia::params
{
  if  ($service_state == 'no_op') {
  } elsif ($service_state == 'uninstalled') {     

   hdp2::package { 'ganglia-monitor':         
       ensure      => 'uninstalled', 
      java_needed => false      
   }

  } else {
    if ($hdp2::params::service_exists['hdp2-ganglia::server'] != true) {
      class { 'hdp2-ganglia':
       service_state => $service_state
      }
    }

    hdp2::package { 'ganglia-monitor': }

    if ($hdp2::params::service_exists['hdp2-ganglia::server'] != true) {
      class { 'hdp2-ganglia::config': ganglia_server_host => $ganglia_server_host}
    }

#    if (($hdp2::params::service_exists['hdp-hadoop::datanode'] == true) or ($hdp2::params::service_exists['hdp-hadoop::namenode'] == true) or ($hdp2::params::service_exists['hdp-hadoop::jobtracker'] == true) or ($hdp2::params::service_exists['hdp-hadoop::tasktracker'] == true) or ($hdp2::params::service_exists['hdp-hadoop::client'] == true) or ($hdp2::params::service_exists['hdp-hadoop::snamenode'] == true)) {
#     class { 'hdp-hadoop::enable-ganglia': }
#   }

    if ($service_exists['hdp-hbase::master'] == true) {
      class { 'hdp-hbase::master::enable-ganglia': }
    }
  
    if ($service_exists['hdp-hbase::regionserver'] == true) {
      class { 'hdp-hbase::regionserver::enable-ganglia': }
    }

    class { 'hdp2-ganglia::monitor::config-gen': }
  
    class { 'hdp2-ganglia::monitor::gmond': ensure => $service_state}

    if ($hdp2::params::service_exists['hdp2-ganglia::server'] != true) {
      Class['hdp2-ganglia'] -> Hdp2::Package['ganglia-monitor'] -> Class['hdp2-ganglia::config'] -> 
      Class['hdp2-ganglia::monitor::config-gen'] -> Class['hdp2-ganglia::monitor::gmond']
    } else {
      Hdp2::Package['ganglia-monitor'] ->  Class['hdp2-ganglia::monitor::config-gen'] -> Class['hdp2-ganglia::monitor::gmond']
    }
  }
}


class hdp2-ganglia::monitor::config-gen()
{

  $service_exists = $hdp2::params::service_exists

  if ($service_exists['hdp2-hadoop::namenode'] == true) {
    hdp2-ganglia::config::generate_monitor { 'HDPNameNode':}
  }
  if ($service_exists['hdp2-hadoop::resourcemanager'] == true){
    hdp2-ganglia::config::generate_monitor { 'HDPResourceManager':}
  }
  if ($service_exists['hdp2-hbase::master'] == true) {
    hdp2-ganglia::config::generate_monitor { 'HDPHBaseMaster':}
  }
  if ($service_exists['hdp2-hadoop::datanode'] == true) {
    hdp2-ganglia::config::generate_monitor { 'HDPSlaves':}
  }
  Hdp2-ganglia::Config::Generate_monitor<||>{
    ganglia_service => 'gmond',
    role => 'monitor'
  }
   # 
  anchor{'hdp2-ganglia::monitor::config-gen::begin':} -> Hdp2-ganglia::Config::Generate_monitor<||> -> anchor{'hdp2-ganglia::monitor::config-gen::end':}
}

class hdp2-ganglia::monitor::gmond(
  $ensure
  )
{
  if ($ensure == 'running') {
    $command = "service hdp-gmond start >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1"
   } elsif  ($ensure == 'stopped') {
    $command = "service hdp-gmond stop >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1"
  }
  if ($ensure == 'running' or $ensure == 'stopped') {
    hdp::exec { "hdp-gmond service" :
      command => "$command",
      unless => "/bin/ps auwx | /bin/grep [g]mond",
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    }
  }
}
