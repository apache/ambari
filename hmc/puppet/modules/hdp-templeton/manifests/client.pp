class hdp-templeton::client(
  $service_state = $hdp::params::cluster_client_state,
  $templeton_server = undef
) inherits hdp::params
{ 
  if ($service_state == 'no_op') {
   } elsif ($service_state in ['installed_and_configured','uninstalled']) {
     if ($hdp::params::service_exists['hdp-templeton::server'] != true) {
       #installs package, creates user, sets configuration
       class { 'hdp-templeton' :
         service_state => $service_state
       }
      if ($templeton_server != undef) {
        Hdp-Templeton::Configfile<||>{templeton_server => $templeton_server}
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
