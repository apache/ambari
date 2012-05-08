class hdp-hcat::client(
  $service_state = $hdp::params::cluster_client_state,
  $hcat_server_host = undef
) inherits hdp::params
{ 
  if ($service_state == 'no_op') {
   } elsif ($service_state == 'installed_and_configured') {
    include hdp-hcat #installs package, creates user, sets configuration
    if ($hcat_server_host != undef) {
      Hdp-Hcat::Configfile<||>{hcat_server_host => $hcat_server_host}
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
