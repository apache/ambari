class hdp2-ganglia::config(
  $ganglia_server_host = undef,
  $service_state = $hdp2::params::cluster_service_state
)
{
 if ($service_state in ['running','installed_and_configured','stopped']) {
    #TODO: divide into what is needed on server vs what is needed on monitored nodes
    $shell_cmds_dir = $hdp2-ganglia::params::ganglia_shell_cmds_dir
    $shell_files = ['checkGmond.sh','checkRrdcached.sh','gmetadLib.sh','gmondLib.sh','rrdcachedLib.sh' ,'setupGanglia.sh','startGmetad.sh','startGmond.sh','startRrdcached.sh','stopGmetad.sh','stopGmond.sh','stopRrdcached.sh','teardownGanglia.sh']

    hdp2::directory_recursive_create { $shell_cmds_dir :
      owner => root,
      group => root
    } 

     hdp2-ganglia::config::init_file { ['gmetad','gmond']: }

     hdp2-ganglia::config::shell_file { $shell_files: }                       

     hdp2-ganglia::config::file { ['gangliaClusters.conf','gangliaEnv.sh','gangliaLib.sh']: 
       ganglia_server_host => $ganglia_server_host
     }
 
     anchor{'hdp2-ganglia::config::begin':} -> Hdp2::Directory_recursive_create[$shell_cmds_dir] -> Hdp2-ganglia::Config::Shell_file<||> -> anchor{'hdp2-ganglia::config::end':}
     Anchor['hdp2-ganglia::config::begin'] -> Hdp2-ganglia::Config::Init_file<||> -> Anchor['hdp2-ganglia::config::end']
     Anchor['hdp2-ganglia::config::begin'] -> Hdp2-ganglia::Config::File<||> -> Anchor['hdp2-ganglia::config::end']
  }
}

define hdp2-ganglia::config::shell_file()
{
  file { "${hdp2-ganglia::params::ganglia_shell_cmds_dir}/${name}":
    source => "puppet:///modules/hdp2-ganglia/${name}", 
    mode => '0755'
  }
}

define hdp2-ganglia::config::init_file()
{
  file { "/etc/init.d/hdp-${name}":
    source => "puppet:///modules/hdp2-ganglia/${name}.init", 
    mode => '0755'
  }
}

### config files
define hdp2-ganglia::config::file(
  $ganglia_server_host = undef
)
{
  hdp2::configfile { "${hdp2-ganglia::params::ganglia_shell_cmds_dir}/${name}":
    component           => 'ganglia',
    owner               => root,
    group               => root
  }
  if ($ganglia_server_host != undef) {
    Hdp2::Configfile<||>{ganglia_server_host => $ganglia_server_host}
  }
}
