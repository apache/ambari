class hdp-ganglia::config($ganglia_server_host = undef)
{
  #TODO: divide into what is needed on server vs what is needed on monitored nodes
  $shell_cmds_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir
  $shell_files = ['checkGmond.sh','checkRrdcached.sh','gmetadLib.sh','gmondLib.sh','rrdcachedLib.sh' ,'setupGanglia.sh','startGmetad.sh','startGmond.sh','startRrdcached.sh','stopGmetad.sh','stopGmond.sh','stopRrdcached.sh','teardownGanglia.sh']

  hdp::directory_recursive_create { $shell_cmds_dir :
    owner => root,
    group => root
  } 

   hdp-ganglia::config::init_file { ['gmetad','gmond']: }

   hdp-ganglia::config::shell_file { $shell_files: }                       

   hdp-ganglia::config::file { ['gangliaClusters.conf','gangliaEnv.sh','gangliaLib.sh']: 
     ganglia_server_host => $ganglia_server_host
   }
 
   anchor{'hdp-ganglia::config::begin':} -> Hdp::Directory_recursive_create[$shell_cmds_dir] -> Hdp-ganglia::Config::Shell_file<||> -> anchor{'hdp-ganglia::config::end':}
   Anchor['hdp-ganglia::config::begin'] -> Hdp-ganglia::Config::Init_file<||> -> Anchor['hdp-ganglia::config::end']
   Anchor['hdp-ganglia::config::begin'] -> Hdp-ganglia::Config::File<||> -> Anchor['hdp-ganglia::config::end']
}

define hdp-ganglia::config::shell_file()
{
  file { "${hdp-ganglia::params::ganglia_shell_cmds_dir}/${name}":
    source => "puppet:///modules/hdp-ganglia/${name}", 
    mode => '0755'
  }
}

define hdp-ganglia::config::init_file()
{
  file { "/etc/init.d/hdp-${name}":
    source => "puppet:///modules/hdp-ganglia/${name}.init", 
    mode => '0755'
  }
}

### config files
define hdp-ganglia::config::file(
  $ganglia_server_host = undef
)
{
  hdp::configfile { "${hdp-ganglia::params::ganglia_shell_cmds_dir}/${name}":
    component           => 'ganglia',
    owner               => root,
    group               => root
  }
  if ($ganglia_server_host != undef) {
    Hdp::Configfile<||>{ganglia_server_host => $ganglia_server_host}
  }
}
