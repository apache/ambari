class hdp-zookeeper(
  $type = server,
  $service_state = $hdp::params::cluster_service_state,
  $myid = 1,
  $opts = {}
) inherits hdp-zookeeper::params 
{

 if ($service_state == 'no_op') {
   if ($type == 'server') {
     $hdp::params::service_exists['hdp-zookeeper'] = true
  }
 } elsif ($service_state in ['running','stopped','installed_and_configured','uninstalled']) { 
   $zk_user = $hdp-zookeeper::params::zk_user
   $zk_config_dir = $hdp-zookeeper::params::conf_dir
 
   anchor{'hdp-zookeeper::begin':}
   anchor{'hdp-zookeeper::end':}

   if ($service_state == 'uninstalled') {
     if ($type == 'server') {
       $hdp::params::service_exists['hdp-zookeeper'] = true
    }
     hdp::package { 'zookeeper':
       ensure => 'uninstalled'
     }
     Anchor['hdp-zookeeper::begin'] -> Hdp::Package['zookeeper'] -> Anchor['hdp-zookeeper::end']
   } else {
     hdp::package { 'zookeeper':}

     hdp::user{ $zk_user:}

     hdp::directory { $zk_config_dir: }

     hdp-zookeeper::configfile { ['zoo.cfg','zookeeper-env.sh','configuration.xsl','log4j.properties']: }
 
     if ($hdp::params::update_zk_shell_files == true) {
       hdp-zookeeper::shell_file{ ['zkServer.sh','zkEnv.sh']: }
     }

     if ($type == 'server') {
       $hdp::params::service_exists['hdp-zookeeper'] = true
       class { 'hdp-zookeeper::service': 
         ensure => $service_state,
         myid   => $myid
       }
      }

      Anchor['hdp-zookeeper::begin'] -> Hdp::Package['zookeeper'] -> Hdp::User[$zk_user] -> 
        Hdp::Directory[$zk_config_dir] -> Hdp-zookeeper::Configfile<||> -> Anchor['hdp-zookeeper::end']
      if ($type == 'server') {
        Hdp::Directory[$zk_config_dir] -> Hdp-zookeeper::Configfile<||> -> Class['hdp-zookeeper::service'] -> Anchor['hdp-zookeeper::end']
      }
      if ($hdp::params::update_zk_shell_files == true) {
        Hdp::Package['zookeeper'] -> Hdp-zookeeper::Shell_file<||> -> Anchor['hdp-zookeeper::end']
      }
    }
  } else {
    hdp_fail("TODO not implemented yet: service_state = ${service_state}")
  }
}

### config files
define hdp-zookeeper::configfile(
  $mode = undef
) 
{
  hdp::configfile { "${hdp-zookeeper::params::conf_dir}/${name}":
    component       => 'zookeeper',
    owner           => $hdp-zookeeper::params::zk_user,
    mode            => $mode
  }
}

### 
define hdp-zookeeper::shell_file()
{
  file { "${hdp::params::zk_bin}/${name}":
    source => "puppet:///modules/hdp-zookeeper/${name}", 
    mode => '0755'
  }
}
