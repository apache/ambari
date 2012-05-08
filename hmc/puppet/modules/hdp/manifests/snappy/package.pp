class hdp::snappy::package()
{
 hdp::package {'snappy':
    package_type  => 'snappy',
    java_needed   => false
  }
  
  hdp::snappy::package::ln{ 64:} 
  hdp::snappy::package::ln{ 32:} 
  
  anchor{'hdp::snappy::package::begin':} ->  Hdp::Package['snappy'] -> Hdp::Snappy::Package::Ln<||> -> anchor{'hdp::snappy::package::end':}
}

define hdp::snappy::package::ln()
{
  $size = $name
  $hadoop_home = $hdp::params::hadoop_home  
  $snappy_so = $hdp::params::snappy_so
  $so_target_dir = $hdp::params::snappy_compression_so_dirs[$size]
  $so_target = "${so_target_dir}/libsnappy.so"
  $so_src_dir = $hdp::params::snappy_so_src_dir[$size]
  $so_src = "${so_src_dir}/${snappy_so}" 
  
  if ($so_target != $so_src) { 
    $ln_cmd = "mkdir -p $so_target_dir; ln -sf ${so_src} ${so_target}"
    hdp::exec{ "hdp::snappy::package::ln ${name}":
      command => $ln_cmd,
      creates => $so_target
    }
  }
}
