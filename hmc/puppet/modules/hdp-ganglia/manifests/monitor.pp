class hdp-ganglia::monitor(
  $service_state = $hdp::params::cluster_service_state,
  $ganglia_server_host = undef,
  $opts = {}
) inherits hdp-ganglia::params
{
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['uninstalled']) {
    #note: includes the common package ganglia-monitor
    include hdp-ganglia
    class { 'hdp-ganglia::config':
      ganglia_server_host => $ganglia_server_host,
      service_state       => $service_state
    }
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    #note: includes the common package ganglia-monitor
    include hdp-ganglia
    class { 'hdp-ganglia::config': 
      ganglia_server_host => $ganglia_server_host,
      service_state       => $service_state
    }

    class { 'hdp-ganglia::monitor::config-gen':}

    class { 'hdp-ganglia::service::gmond': 
      ensure => $service_state
    }

    Class['hdp-ganglia'] -> Class['hdp-ganglia::config'] -> Class['hdp-ganglia::monitor::config-gen'] -> Class['hdp-ganglia::service::gmond']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

class hdp-ganglia::monitor::config-gen()
{

  anchor{'hdp-ganglia::monitor::config-gen::begin':}

  #TODO: to get around anchor problems
  Class['hdp-ganglia'] -> Class['hdp-ganglia::monitor::config-gen']

  if ($hdp-ganglia::params::nomit_namenode == true) {
    hdp-ganglia::config::generate_monitor { 'HDPNameNode':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::monitor::config-gen::begin'],
      before  => Anchor['hdp-ganglia::monitor::config-gen::end']
    }
  }
  if ($hdp-ganglia::params::nomit_jobtracker == true) {
    hdp-ganglia::config::generate_monitor { 'HDPJobTracker':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::monitor::config-gen::begin'],
      before  => Anchor['hdp-ganglia::monitor::config-gen::end']
    }
  }
  if ($hdp-ganglia::params::nomit_hbase_master == true) {
    hdp-ganglia::config::generate_monitor { 'HDPHBaseMaster':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::monitor::config-gen::begin'],
      before  => Anchor['hdp-ganglia::monitor::config-gen::end']
    }
  }
  if ($hdp-ganglia::params::nomit_slaves == true) {
    hdp-ganglia::config::generate_monitor { 'HDPSlaves':
      ganglia_service => 'gmond',
      require => Anchor['hdp-ganglia::monitor::config-gen::begin'],
      before  => Anchor['hdp-ganglia::monitor::config-gen::end']
    }
  }  
  anchor{'hdp-ganglia::monitor::config-gen::end':}
}
