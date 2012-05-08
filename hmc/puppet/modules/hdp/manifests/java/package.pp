define hdp::java::package(
  $size,
  $include_artifact_dir = true
)
{
    
  include hdp::params
  
  $jdk_bin = $hdp::params::jdk_bins[$size]
  $artifact_dir = $hdp::params::artifact_dir
  $jdk_location = $hdp::params::jdk_location
  $jdk_curl_target = "${artifact_dir}/${jdk_bin}"
 
  
  if ($size == "32") {
    $java_home = $hdp::params::java32_home
  } else {
    $java_home = $hdp::params::java64_home
  }
  $java_exec = "${java_home}/bin/java"
  $java_dir = regsubst($java_home,'/[^/]+$','')
   
  if ($include_artifact_dir == true) {
    hdp::artifact_dir{ "java::package::${name}": }
  }
  
  $curl_cmd = "mkdir -p ${artifact_dir} ; curl -f --retry 10 ${jdk_location}/${jdk_bin} -o ${jdk_curl_target}"
  exec{ "${curl_cmd} ${name}":
    command => $curl_cmd,
    creates => $jdk_curl_target,
    path    => ["/bin","/usr/bin/"],
    unless  => "test -e ${java_exec}"
  }
 
  $install_cmd = "mkdir -p ${java_dir} ; chmod +x ${jdk_curl_target}; cd ${java_dir} ; echo A | ${jdk_curl_target} -noregister > /dev/null 2>&1"
  exec{ "${install_cmd} ${name}":
    command => $install_cmd,
    unless  => "test -e ${java_exec}",
    path    => ["/bin","/usr/bin/"]
  }
  
  anchor{"hdp::java::package::${name}::begin":} -> Exec["${curl_cmd} ${name}"] ->  Exec["${install_cmd} ${name}"] -> anchor{"hdp::java::package::${name}::end":}
  if ($include_artifact_dir == true) {
    Anchor["hdp::java::package::${name}::begin"] -> Hdp::Artifact_dir["java::package::${name}"] -> Exec["${curl_cmd} ${name}"]
  }
}
