class hdp-ganglia::monitor(
  $service_state = $hdp::params::cluster_service_state,
  $ganglia_server_host = undef,
  $monitor_and_server_single_node = false,
  $opts = {}
) inherits hdp-ganglia::params
{
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {
    if ($monitor_and_server_single_node == false) {
      include hdp-ganglia #note: includes the common package ganglia-monitor
      class { 'hdp-ganglia::config': 
        ganglia_server_host => $ganglia_server_host,
        require             => Class['hdp-ganglia'],
        before              => Class['hdp-ganglia::monitor::config-gen']
      }
    }

    class { 'hdp-ganglia::monitor::config-gen': }

    if ($monitor_and_server_single_node == false) {
      Class['hdp-ganglia'] -> Class['hdp-ganglia::monitor::config-gen']
      class { 'hdp-ganglia::service::gmond': 
        ensure => $service_state,
        require  => Class['hdp-ganglia::monitor::config-gen']
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-ganglia::monitor::config-gen()
{

  #TODO: to get around anchor problems
  Class['hdp-ganglia'] -> Class['hdp-ganglia::monitor::config-gen']

  $service_exists = $hdp::params::service_exists

  if ($hdp-ganglia::params::omit_namenode != true) {
    hdp-ganglia::config::generate_monitor { 'HDPNameNode':}
  }
  if ($hdp-ganglia::params::omit_job_tracker != true) {
    hdp-ganglia::config::generate_monitor { 'HDPJobTracker':}
  }
  if ($hdp-ganglia::params::omit_hbase_master != true) {
    hdp-ganglia::config::generate_monitor { 'HDPHBaseMaster':}
  }
  if ($hdp-ganglia::params::omit_slaves != true) {
    hdp-ganglia::config::generate_monitor { 'HDPSlaves':}
  }
  Hdp-ganglia::Config::Generate_monitor<||>{
    ganglia_service => 'gmond'
  }

  anchor{'hdp-ganglia::monitor::config-gen::begin':} -> Hdp-ganglia::Config::Generate_monitor<||> -> anchor{'hdp-ganglia::monitor::config-gen::end':}
}
