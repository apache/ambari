class hdp-hbase(
  $type,
  $service_state) 
{
  include hdp-hbase::params
 
  $hbase_user = $hdp-hbase::params::hbase_user
  $config_dir = $hdp-hbase::params::conf_dir
  
  $hdp::params::component_exists['hdp-hbase'] = true

  anchor{'hdp-hbase::begin':}
  anchor{'hdp-hbase::end':}

  if ($service_state == 'uninstalled') {
    hdp::package { 'hbase':
      ensure => 'uninstalled'
    }
    hdp::directory { $config_dir:
      service_state => $service_state,
      force => true
    }

    Anchor['hdp-hbase::begin'] -> Hdp::Package['hbase'] -> Hdp::Directory[$config_dir] -> Anchor['hdp-hbase::end']

  } else {  
    hdp::package { 'hbase': }
  
    hdp::user{ $hbase_user:}
 
    hdp::directory { $config_dir: 
      service_state => $service_state,
      force => true
    }

    hdp-hbase::configfile { ['hbase-env.sh','hbase-site.xml','hbase-policy.xml','log4j.properties','hadoop-metrics.properties']: }
    if ($type == 'master') {
      hdp-hbase::configfile { 'regionservers':}
    }
    Anchor['hdp-hbase::begin'] -> Hdp::Package['hbase'] -> Hdp::User[$hbase_user] -> Hdp::Directory[$config_dir] -> 
    Hdp-hbase::Configfile<||> ->  Anchor['hdp-hbase::end']
  }
}

### config files
define hdp-hbase::configfile(
  $mode = undef,
  $hbase_master_host = undef,
  $template_tag = undef
) 
{
  hdp::configfile { "${hdp-hbase::params::conf_dir}/${name}":
    component         => 'hbase',
    owner             => $hdp-hbase::params::hbase_user,
    mode              => $mode,
    hbase_master_host => $hbase_master_host,
    template_tag      => $template_tag
  }
}
