class hdp-sqoop::mysql-connector()
{
  include hdp-sqoop::params
  include hdp-hive::params

  $target = "${hdp::params::artifact_dir}/${zip_name}"
  $sqoop_lib = $hdp-sqoop::params::sqoop_lib

  anchor { 'hdp-sqoop::mysql-connector::begin':}

   hdp::exec { 'yum install -y mysql-connector-java-5.0.8-1':
       command => "yum install -y mysql-connector-java-5.0.8-1",
       unless  => "rpm -qa | grep mysql-connector-java-5.0.8-1",
       path    => ["/bin","/usr/bin/"],
       require   => Anchor['hdp-sqoop::mysql-connector::begin']
   }

   hdp::exec { 'sqoop mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /usr/share/java/mysql-connector-java.jar  ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"],
       require => Hdp::Exec['yum install -y mysql-connector-java-5.0.8-1'],
       notify  =>  Anchor['hdp-sqoop::mysql-connector::end'],
   }

   anchor { 'hdp-sqoop::mysql-connector::end':}
  
}
