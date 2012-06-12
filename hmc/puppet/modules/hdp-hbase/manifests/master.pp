class hdp-hbase::master(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hbase::params 
{

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {    
    $hdp::params::service_exists['hdp-hbase::master'] = true

    $hdfs_root_dir = $hdp-hbase::params::hbase_hdfs_root_dir
  
    #adds package, users, directories, and common configs
    class { 'hdp-hbase': 
      type          => 'master',
      service_state => $service_state
    }

    Hdp-hbase::Configfile<||>{hbase_master_host => $hdp::params::host_address}
  
    hdp-hadoop::hdfs::directory { $hdfs_root_dir:
      owner         => $hdp-hbase::params::hbase_user,
      service_state => $service_state
    }    

    hdp-hbase::service{ 'master':
      ensure => $service_state
    }

    #top level does not need anchors
    Class['hdp-hbase'] -> Hdp-hadoop::Hdfs::Directory[$hdfs_root_dir] -> Hdp-hbase::Service['master'] 
    } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

#assumes that master and regionserver will not be on same machine
class hdp-hbase::master::enable-ganglia()
{
  Hdp-hbase::Configfile<|title  == 'hadoop-metrics.properties'|>{template_tag => 'GANGLIA-MASTER'}
}

