class hdp-hadoop::client(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp::params
{
  $hdp::params::service_exists['hdp-hadoop::client'] = true

  Hdp-hadoop::Common<||>{service_states +> $service_state}
  Hdp-hadoop::Package<||>{include_32_bit => true}
  Hdp-hadoop::Configfile<||>{sizes +> 32}

  if ($service_state == 'no_op') {
  } elsif ($service_state in ['installed_and_configured','uninstalled']) {
    #adds package, users and directories, and common hadoop configs
    include hdp-hadoop::initialize
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
