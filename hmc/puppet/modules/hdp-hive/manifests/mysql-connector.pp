class hdp-hive::mysql-connector()
{
  include hdp-hive::params

  $url = $hdp-hive::params::mysql_connector_url
  $zip_name = regsubst($url,'^.+/([^/]+$)','\1')
  $jar_name = regsubst($zip_name,'zip$','.jar')
  $target = "${hdp::params::artifact_dir}/${zip_name}"
  $hive_lib = $hdp-hive::params::hive_lib
  
  exec{ "curl hive_mysql_url":
    command => "mkdir -p ${artifact_dir} ; curl -f --retry 10 ${url} -o ${target} ",
    creates => $target,
    path    => ["/bin","/usr/bin/"]
  }
  exec{ "unzip hive_mysql_url":
    command => "unzip -o -j ${target}",
    cwd     => $hive_lib,
    #user    => $hdp::params::hive_user,
    group   => $hdp::params::hadoop_user_group,
    creates => "${hive_lib}/${$zip_name}",
    path    => ["/usr/bin/"]
  }

  Exec["curl hive_mysql_url"] -> Exec["unzip hive_mysql_url"]
}
