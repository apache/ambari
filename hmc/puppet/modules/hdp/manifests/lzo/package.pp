define hdp::lzo::package()
{
  $size = $name
  hdp::package {"lzo ${size}":
    package_type  => 'lzo',
    size          => $size,
    java_needed   => false
  }

  hdp::lzo::package::tar { $size:}  
  
  $anchor_beg = "hdp::lzo::package::${size}::begin"
  $anchor_end = "hdp::lzo::package::${size}::end"
  anchor{$anchor_beg:} ->  Hdp::Package["lzo ${size}"] -> anchor{$anchor_end:}
  Anchor[$anchor_beg] ->  Hdp::Lzo::Package::Tar[$size] -> Anchor[$anchor_end]
}

define hdp::lzo::package::tar()
{
  $size = $name
  
  $hadoop_home = $hdp::params::hadoop_home
  $lzo_tar_name = $hdp::params::lzo_tar_name
  $lzo_tar_gz = "${lzo_tar_name}.tar.gz"
  $artifact_dir = $hdp::params::artifact_dir
  $artifacts_download_url = $hdp::params::artifacts_download_url
  $lzo_curl_target = "${artifact_dir}/${lzo_tar_gz}"
  $lzo_dir = "${artifact_dir}/${lzo_tar_name}"
  $lzo_jar = "${lzo_tar_name}.jar"
   
  $curl_cmd = "mkdir -p ${artifact_dir} ; curl -f --retry 10 ${artifacts_download_url}/${lzo_tar_gz} -o ${lzo_curl_target}"
  $untar_cmd = "tar xzf ${artifact_dir}/${lzo_tar_gz}"
  
  ### compute $install_cmd 
  $install_dir = $hdp::params::lzo_compression_so_dirs[$size]
   if ($size == 32) {
    $linux = "Linux-i386-32"
  } else {
    $linux = "Linux-amd64-64"
  }
  $jar_target_dir = "${hadoop_home}/lib/hadoop/lib"
  $install_cmd_cp1 = "mkdir -p ${jar_target_dir};cp -f ${lzo_dir}/${lzo_jar} ${jar_target_dir}"
  $install_cmd_rm = "rm -f ${install_dir}/libgplcompression*"
  $install_mkdir = "mkdir -p ${install_dir}"
  $install_cmd_cp2 = "cp ${lzo_dir}/lib/native/${linux}/libgplcompression* ${install_dir}"
  $install_cmd = "${install_cmd_cp1}; ${install_cmd_rm}; ${install_mkdir}; ${install_cmd_cp2}"
  ### end: compute $install_cmd 

  hdp::exec{ "hdp::lzo::package::tar curl_cmd ${name}":
    command => $curl_cmd,
    creates => $lzo_curl_target
  }

  hdp::exec{ "hdp::lzo::package::tar untar_cmd ${name}":
    command     => $untar_cmd,
    cwd         => $artifact_dir,
    creates => "${lzo_dir}/${lzo_jar}"
  }

   hdp::exec{ "hdp::lzo::package::tar install_cmd ${name}":
    command => $install_cmd
#    unless  => "test -e ${jar_target_dir}/${lzo_jar}"
  }
 
  $anchor_beg = "hdp::lzo::package::tar::${name}::begin"
  $anchor_end = "hdp::lzo::package::tar::${name}::end"
  anchor{ $anchor_beg:} -> Exec["hdp::lzo::package::tar curl_cmd ${name}"] -> Exec["hdp::lzo::package::tar untar_cmd ${name}"] -> 
    Exec["hdp::lzo::package::tar install_cmd ${name}"] -> anchor{ $anchor_end:}
}
