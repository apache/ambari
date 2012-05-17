class hdp-sqoop::mysql-connector()
{
  include hdp-sqoop::params
  include hdp-hive::params

  $url = $hdp-hive::params::mysql_connector_url
  notice($url)
  $zip_name = regsubst($url,'^.+/([^/]+$)','\1')
  $jar_name = regsubst($zip_name,'zip$','.jar')
  $target = "${hdp::params::artifact_dir}/${zip_name}"
  $sqoop_lib = $hdp-sqoop::params::sqoop_lib
  
  exec{ "curl sqoop_mysql_url":
    command => "mkdir -p ${artifact_dir} ; curl -f --retry 10 ${url} -o ${target} ",
    creates => $target,
    path    => ["/bin","/usr/bin/"]
  }
  exec{ "unzip sqoop_mysql_url":
    command => "unzip -o -j ${target}",
    cwd     => $sqoop_lib,
    group   => $hdp::params::hadoop_user_group,
    creates => "${sqoop_lib}/${$zip_name}",
    path    => ["/usr/bin/"]
  }

  Exec["curl sqoop_mysql_url"] -> Exec["unzip sqoop_mysql_url"]
}
