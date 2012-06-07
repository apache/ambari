class hdp-hive::mysql-connector()
{
  include hdp-hive::params

  $hive_lib = $hdp-hive::params::hive_lib
  $target = "${hive_lib}/mysql-connector-java.jar"
  
  anchor { 'hdp-hive::mysql-connector::begin':}

   hdp::package { 'mysql-connector' :
     require   => Anchor['hdp-hive::mysql-connector::begin']
   }

   hdp::exec { 'hive mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"],
       require => Hdp::Package['mysql-connector'],
       notify  =>  Anchor['hdp-hive::mysql-connector::end'],
   }

   anchor { 'hdp-hive::mysql-connector::end':}

}
