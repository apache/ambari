class hdp::snmp(
  $service_state = 'running'
)
{
  include hdp::params

  hdp::snmp-configfile {'snmpd.conf': 
    notify => Service['snmpd']    
  }

  service { 'snmpd' :
    ensure => $service_state
  }
  
  anchor{'hdp::snmp::begin':} -> Hdp::Snmp-configfile<||> -> Service['snmpd'] -> anchor{'hdp::snmp::end':}
}

define hdp::snmp-configfile()
{ 
  hdp::configfile { "${hdp::params::snmp_conf_dir}/${name}":
    component     => 'base',
    owner         => root,
    group         => root
  }
}

