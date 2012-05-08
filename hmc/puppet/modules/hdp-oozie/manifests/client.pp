class hdp-oozie::client(
  $service_state = $hdp::params::cluster_client_state,
  $oozie_server = undef
) inherits hdp::params
{ 
  if ($service_state == 'no_op') {
   } elsif ($service_state in ['installed_and_configured','uninstalled']) {
     if ($hdp::params::service_exists['hdp-oozie::server'] != true) {
       #installs package, creates user, sets configuration
       class { 'hdp-oozie' :
         service_state => $service_state
       }
      if ($oozie_server != undef) {
        Hdp-Oozie::Configfile<||>{oozie_server => $oozie_server}
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
