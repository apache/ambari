class hdp-hcat::mysql-connector()
{
  include hdp-hcat::params

  $url = $hdp-hcat::params::mysql_connector_url
  $zip_name = regsubst($url,'^.+/([^/]+$)','\1')
  $jar_name = regsubst($zip_name,'zip$','-bin.jar')
  $target = "${hdp::params::artifact_dir}/${zip_name}"
  $hcat_lib = $hdp-hcat::params::hcat_lib
  
  exec{ "curl ${url}":
    command => "mkdir -p ${artifact_dir} ; curl -f --retry 10 ${url} -o ${target} ",
    creates => $target,
    path    => ["/bin","/usr/bin/"]
  }
  exec{ "unzip ${target}":
    command => "unzip -o -j ${target} '*.jar' -x */lib/*",
    cwd     => $hcat_lib,
    user    => $hdp::params::hcat_user,
    group   => $hdp::params::hadoop_user_group,
    creates => "${hcat_lib}/${$jar_name}",
    path    => ["/bin","/usr/bin/"]
  }

  Exec["curl ${url}"] -> Exec["unzip ${target}"]
}
