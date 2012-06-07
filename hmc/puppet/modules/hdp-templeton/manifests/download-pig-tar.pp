class hdp-templeton::download-pig-tar()
{
  include hdp-templeton::params

  $src_tar_name = $hdp-templeton::params::src_pig_tar_name
  $dest_tar_name = $hdp-templeton::params::dest_pig_tar_name
  $target = "${hdp::params::artifact_dir}/${dest_tar_name}"

  anchor { 'hdp-templeton::download-pig-tar::begin':}

   hdp::package { 'templeton-tar-pig-0.0.1-1' :
     require   => Anchor['hdp-templeton::download-pig-tar::begin']
   }

   hdp::exec { 'mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /tmp/HDP-templeton/${src_tar_name} ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"]
       require => Hdp::Package['templeton-tar-pig-0.0.1-1'],
       notify  =>  Anchor['hdp-templeton::download-pig-tar::end'],
   }

   anchor { 'hdp-templeton::download-pig-tar::end':}

}
