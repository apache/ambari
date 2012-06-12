class hdp-hive::client(
  $service_state = $hdp::params::cluster_client_state,
  $hive_server_host = undef
) inherits hdp::params
{ 
  if ($service_state == 'no_op') {
   } elsif ($service_state in ['installed_and_configured','uninstalled']) {
    if ($hdp::params::service_exists['hdp-hive::server'] != true) {
      #installs package, creates user, sets configuration
      class { 'hdp-hive':
        service_state => $service_state
      } 
      if ($hive_server_host != undef) {
        Hdp-Hive::Configfile<||>{hive_server_host => $hive_server_host}
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
