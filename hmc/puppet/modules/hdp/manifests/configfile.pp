define hdp::configfile(
  $component,
  $conf_dir = undef, #if this is undef then name is of form conf_dir/file_name
  $owner = undef, 
  $group = $hdp::params::hadoop_user_group,
  $mode = undef,
  $size = 64, #32 or 64 bit (used to pick appropriate java_home)
  $template_tag = undef,
  $namenode_host = $hdp::params::namenode_host,
  $jtnode_host = $hdp::params::jtnode_host,
  $snamenode_host = $hdp::params::snamenode_host,
  $slave_hosts = $hdp::params::slave_hosts,
  $rs_hosts = $hdp::params::rs_hosts,
  $zookeeper_hosts = $hdp::params::zookeeper_hosts,
  $hbase_master_host = $hdp::params::hbase_master_host,
  $hcat_server_host = $hdp::params::hcat_server_host,
  $hive_server_host = $hdp::params::hive_server_host,
  $oozie_server = $hdp::params::oozie_server,
  $templeton_server_host = $hdp::params::templeton_server_host,
  $hcat_mysql_host = $hdp::params::hcat_mysql_host,
  $nagios_server_host = $hdp::params::nagios_server_host,
  $ganglia_server_host = $hdp::params::ganglia_server_host,
  $dashboard_host = $hdp::params::dashboard_host,
  $gateway_host = $hdp::params::gateway_host
) 
{

   if ($conf_dir == undef) {
     $qualified_file_name = $name
     $file_name = regsubst($name,'^.+/([^/]+$)','\1')
   } else {
     $qualified_file_name = "${conf_dir}/${name}"
     $file_name = $name
   }
   if ($component == 'base') {
     $module = 'hdp'
   } else {
      $module = "hdp-${component}"   
   }

   if ($template_tag == undef) {  
     $template_name = "${module}/${file_name}.erb"
   } else {
     $template_name = "${module}/${file_name}-${template_tag}.erb"
   }

   file{ $qualified_file_name:
     ensure  => present,
     owner   => $owner,
     group   => $group,
     mode    => $mode,
     content => template($template_name)
  }
}
