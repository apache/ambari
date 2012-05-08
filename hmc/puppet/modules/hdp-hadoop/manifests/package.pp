#singleton, but using define so can use collections to override params
define hdp-hadoop::package(
  $ensure = 'present',
  $include_32_bit = false,
  $include_64_bit = false
)
{
  #just use 32 if its specifically requested and no 64 bit requests
  if ($include_32_bit == true) and ($include_64_bit != true) {
    $size = 32
  } else  {
    $size = 64
  }
  $package = "hadoop ${size}"
  $lzo_enabled = $hdp::params::lzo_enabled

  hdp::package{ $package:
    ensure       => $ensure,
    package_type => 'hadoop',
    size         => $size,
    lzo_needed   => $lzo_enabled
  }
  anchor{ 'hdp-hadoop::package::helper::begin': } -> Hdp::Package[$package] -> anchor{ 'hdp-hadoop::package::helper::end': }
}