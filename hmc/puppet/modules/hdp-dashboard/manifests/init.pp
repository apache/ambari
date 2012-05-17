class hdp-dashboard(
  $service_state = $hdp::params::cluster_client_state,
  $opts = {}
) inherits hdp-dashboard::params
{
   if ($service_state == 'no_op') {
   } elsif ($service_state == 'uninstalled') {
    hdp::package { 'dashboard' :
      ensure => 'uninstalled',
      size   => 64
    }
    hdp::directory_recursive_create { $conf_dir :
      service_state => $service_state,
      force => true
    }

    Hdp::Package['dashboard'] -> Hdp::Directory_recursive_create[$conf_dir]

   } elsif ($service_state in ['running','installed_and_configured','stopped']) {
      hdp::package { 'dashboard' :
        size => 64
       }
     $conf_dir =  $hdp-dashboard::params::conf_dir
  
     hdp::directory_recursive_create { $conf_dir :
       service_state => $service_state,
       force => true
     }
 
     hdp-dashboard::configfile { 'cluster_configuration.json' : }
     Hdp-Dashboard::Configfile<||>{dashboard_host => $hdp::params::host_address}
  
     #top level does not need anchors
     Hdp::Package['dashboard'] -> Hdp::Directory_recursive_create[$conf_dir] -> Hdp-Dashboard::Configfile<||> 
    } else {
     hdp_fail("TODO not implemented yet: service_state = ${service_state}")
   }
}

###config file helper
define hdp-dashboard::configfile(
  $dashboard_host = undef
)
{
  
  hdp::configfile { "${hdp-dashboard::params::conf_dir}/${name}":
    component      => 'dashboard',
    owner          => root,
    group          => root,
    dashboard_host => $dashboard_host
  }
}


