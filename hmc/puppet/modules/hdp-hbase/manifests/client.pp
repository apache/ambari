class hdp-hbase::client(
  $service_state = $hdp::params::cluster_client_state,
  $opts = {}
)
{
  #assumption is there are no other hbase components on node
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['installed_and_configured','uninstalled']) {
    if (($hdp::params::service_exists['hdp-hbase::master'] != true) and ($hdp::params::service_exists['hdp-hbase::regionserver'] != true)) {
      #adds package, users, directories, and common configs
      class { 'hdp-hbase': 
        type          => 'client',
        service_state => $service_state
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}