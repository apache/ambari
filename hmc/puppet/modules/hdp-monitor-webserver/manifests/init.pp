class hdp-monitor-webserver( 
  $service_state = $hdp::params::cluster_service_state,
  $opts = {}
) inherits hdp::params 
{
  #TODO: does not install apache package
  if ($service_state == 'no_op') {
  } elsif ($service_state in ['running','stopped','installed_and_configured']) {
    if ($service_state == 'running') {
      #TODO: refine by using notify/subscribe
      hdp::exec { 'monitor webserver start':
        command => '/etc/init.d/httpd start',
        unless => '/etc/init.d/httpd status'
      } 
    } elsif ($service_state == 'stopped') {
      package { 'httpd':
        ensure => 'stopped'
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}
