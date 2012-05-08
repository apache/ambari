class hdp-zookeeper::client(
  $service_state = $hdp::params::cluster_client_state
) inherits hdp::params
{
  $package_type = $hdp::params::packages

  if ($service_state == 'no_op') {
  } elsif  ($service_state in ['installed_and_configured','uninstalled']) {
      if ($package_type == 'hdp') {
        $cmd = "ln -s /usr/libexec/zkEnv.sh /usr/bin/zkEnv.sh"
        $test = "test -e /usr/bin/zkEnv.sh"
        hdp::exec { $cmd :
           command => $cmd,
           unless  => $test,
           require => Class['hdp-zookeeper']
        }
      } 
      if ($hdp::params::service_exists['hdp-zookeeper'] != true) {
        class { 'hdp-zookeeper' : 
         type => 'client',
         service_state => $service_state
        } 
      }
    } else {
   hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
