class hdp-hive::mysql-connector()
{
  include hdp-hive::params

  $hive_lib = $hdp-hive::params::hive_lib
  $target = "${hive_lib}/mysql-connector-java.jar"
  
  anchor { 'hdp-hive::mysql-connector::begin':}

   hdp::package { 'mysql-connector-java-5.0.8-4.jpp5' :
     require   => Anchor['hdp-hive::mysql-connector::begin']
   }

   hdp::exec { 'hive mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"],
       require => Hdp::Package['mysql-connector-java-5.0.8-4.jpp5'],
       notify  =>  Anchor['hdp-hive::mysql-connector::end'],
   }

   anchor { 'hdp-hive::mysql-connector::end':}

}
