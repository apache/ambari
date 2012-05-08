class hdp-hcat(
  $server = false
) 
{
  include hdp-hcat::params

  $hcat_user = $hdp::params::hcat_user
  $hcat_config_dir = $hdp-hcat::params::hcat_conf_dir
 
  hdp::package { 'hcat-base' : }
  if ($server == true ) {
    hdp::package { 'hcat-server':} 
    class { 'hdp-hcat::mysql-connector': }
  }
  
  hdp::user{ $hcat_user:}
  
  hdp::directory { $hcat_config_dir: }

  hdp-hcat::configfile { ['hcat-env.sh','hive-env.sh','hive-site.xml']: }
  
  anchor { 'hdp-hcat::begin': } -> Hdp::Package['hcat-base'] -> Hdp::User[$hcat_user] -> 
   Hdp::Directory[$hcat_config_dir] -> Hdp-hcat::Configfile<||> ->  anchor { 'hdp-hcat::end': }

   if ($server == true ) {
     Hdp::Package['hcat-base'] -> Hdp::Package['hcat-server'] ->  Hdp::User[$hcat_user] -> Class['hdp-hcat::mysql-connector'] -> Anchor['hdp-hcat::end']
  }
}

### config files
define hdp-hcat::configfile(
  $mode = undef,
  $hcat_server_host = undef
) 
{
  hdp::configfile { "${hdp-hcat::params::hcat_conf_dir}/${name}":
    component        => 'hcat',
    owner            => $hdp::params::hcat_user,
    mode             => $mode,
    hcat_server_host => $hcat_server_host 
  }
}
