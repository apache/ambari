class hdp-templeton(
  $service_state = undef,
  $server = false
)
{
  include hdp-templeton::params 
 
  $templeton_user = $hdp-templeton::params::templeton_user
  $templeton_config_dir = $hdp-templeton::params::conf_dir

  if ($service_state == 'uninstalled') {
      hdp::package { 'templeton' :
      size => 32,
      ensure => 'uninstalled'
    }
  } else {
    hdp::package { 'templeton' :
    size => 32
    }
    class { hdp-templeton::download-hive-tar: }
    class { hdp-templeton::download-pig-tar: }

    hdp::user{ $templeton_user:}

    hdp::directory { $templeton_config_dir: }

    hdp-templeton::configfile { ['templeton-site.xml','templeton-env.sh']: }

    anchor { 'hdp-templeton::begin': } -> Hdp::Package['templeton'] -> Hdp::User[$templeton_user] -> Hdp::Directory[$templeton_config_dir] -> Hdp-templeton::Configfile<||> ->  anchor { 'hdp-templeton::end': }

     if ($server == true ) { 
      Hdp::Package['templeton'] -> Hdp::User[$templeton_user] ->   Class['hdp-templeton::download-hive-tar'] -> Class['hdp-templeton::download-pig-tar'] -> Anchor['hdp-templeton::end']
     }
  }
}

### config files
define hdp-templeton::configfile(
  $mode = undef
) 
{
  hdp::configfile { "${hdp-templeton::params::conf_dir}/${name}":
    component       => 'templeton',
    owner           => $hdp-templeton::params::templeton_user,
    mode            => $mode
  }
}

