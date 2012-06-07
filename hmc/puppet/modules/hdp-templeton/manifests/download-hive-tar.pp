class hdp-templeton::download-hive-tar()
{
  include hdp-templeton::params

  $src_tar_name = $hdp-templeton::params::src_hive_tar_name
  $dest_tar_name = $hdp-templeton::params::dest_hive_tar_name
  $target = "${hdp::params::artifact_dir}/${dest_tar_name}"
 
  anchor { 'hdp-templeton::download-hive-tar::begin':}         

   hdp::package { 'templeton-tar-hive-0.0.1-1' :
     require   => Anchor['hdp-templeton::download-hive-tar::begin']                                                              
   }
  
   hdp::exec { 'mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"],
       require => Hdp::Package['templeton-tar-hive-0.0.1-1'],
       notify  =>  Anchor['hdp-templeton::download-hive-tar::end'],
   }

   anchor { 'hdp-templeton::download-hive-tar::end':}       

}
