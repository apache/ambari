define hdp::lzo::package()
{
  $size = $name
  hdp::package {"lzo ${size}":
    package_type  => 'lzo',
    size          => $size,
    java_needed   => false
  }

  $anchor_beg = "hdp::lzo::package::${size}::begin"
  $anchor_end = "hdp::lzo::package::${size}::end"
  anchor{$anchor_beg:} ->  Hdp::Package["lzo ${size}"] -> anchor{$anchor_end:}
}

