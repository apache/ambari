class hdp-oozie(
  $service_state = undef,
  $server = false,
  $setup = false
)
{
  include hdp-oozie::params 
 
  $oozie_user = $hdp-oozie::params::oozie_user
  $oozie_config_dir = $hdp-oozie::params::conf_dir
  
  if ($service_state == 'uninstalled') {
    hdp::package { 'oozie-client' : 
      ensure => 'uninstalled'
    }
  } else {
    hdp::package { 'oozie-client' : }
    if ($server == true ) {
      hdp::package { 'oozie-server':}
      class { 'hdp-oozie::download-ext-zip': }
    }

     hdp::user{ $oozie_user:}

     hdp::directory { $oozie_config_dir: }

     hdp-oozie::configfile { ['oozie-site.xml','oozie-env.sh','oozie-log4j.properties']: }

    anchor { 'hdp-oozie::begin': } -> Hdp::Package['oozie-client'] -> Hdp::User[$oozie_user] -> Hdp::Directory[$oozie_config_dir] -> Hdp-oozie::Configfile<||> -> anchor { 'hdp-oozie::end': }

     if ($server == true ) { 
       Hdp::Package['oozie-server'] -> Hdp::Package['oozie-client'] -> Hdp::User[$oozie_user] ->   Class['hdp-oozie::download-ext-zip'] ->  Anchor['hdp-oozie::end']
     }
 }
}

### config files
define hdp-oozie::configfile(
  $mode = undef,
  $oozie_server = undef
) 
{
  hdp::configfile { "${hdp-oozie::params::conf_dir}/${name}":
    component       => 'oozie',
    owner           => $hdp-oozie::params::oozie_user,
    mode            => $mode,
    oozie_server    => $oozie_server
  }
}
