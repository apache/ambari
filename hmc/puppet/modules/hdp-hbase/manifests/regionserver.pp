class hdp-hbase::regionserver(
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp-hbase::params
{

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) {    
    $hdp::params::service_exists['hdp-hbase::regionserver'] = true       

    if ($hdp::params::service_exists['hdp-hbase::master'] != true) {
      #adds package, users, directories, and common configs
      class { 'hdp-hbase': 
        type          => 'regionserver',
        service_state => $service_state
      } 
      $create_pid_dir = true
      $create_log_dir = true
    } else {
      $create_pid_dir = false
      $create_log_dir = false
    }


    hdp-hbase::service{ 'regionserver':
      ensure         => $service_state,
      create_pid_dir => $create_pid_dir,
      create_log_dir => $create_log_dir
    }

    #top level does not need anchors
    Class['hdp-hbase'] ->  Hdp-hbase::Service['regionserver']
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

#assumes that master and regionserver will not be on same machine
class hdp-hbase::regionserver::enable-ganglia()
{
  Hdp-hbase::Configfile<|title  == 'hadoop-metrics.properties'|>{template_tag => 'GANGLIA-RS'}
}
