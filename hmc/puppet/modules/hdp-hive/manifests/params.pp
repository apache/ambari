class hdp-hive::params() inherits hdp::params
{

  #TODO: will move to globals
  $hive_metastore_user_name = hdp_default("hadoop/hive-site/hive_metastore_user_name","dbusername")
  $hive_metastore_user_passwd = hdp_default("hadoop/hive-site/hive_metastore_user_passwd","dbpassword")
 
 ####### users
 
  
  ### common
  $hive_metastore_port = hdp_default("hive_metastore_port",9083)
  $hive_lib = hdp_default("hive_lib","/usr/lib/hive/lib/") #TODO: should I remove and just use hive_dbroot

  ### hive-env
  $hive_conf_dir = $hdp::params::hive_conf_dir

  $hive_dbroot = hdp_default("hadoop/hive-env/hive_dbroot",$hive_lib)

  $hive_log_dir = hdp_default("hadoop/hive-env/hive_log_dir","/var/log/hive")

  $hive_pid_dir = hdp_default("hadoop/hive-env/hive_pid_dir","/var/run/hive")
#  $hive_pid_dir = "${hive_piddirprefix}/${hdp::params::hive_user}"
  
  ### hive-site
  $hive_database_name = hdp_default("hadoop/hive-site/hive_database_name","hive")

  $hive_metastore_principal = hdp_default("hadoop/hive-site/hive_metastore_principal")

  $hive_metastore_sasl_enabled = hdp_default("hadoop/hive-site/hive_metastore_sasl_enabled","false")

  #TODO: using instead hive_server_host in hdp::params $hive_metastore_server_host = hdp_default("hadoop/hive-site/hive_metastore_server_host")

  $keytab_path = hdp_default("hadoop/hive-site/keytab_path")
  
  ###mysql connector
  $download_url = $hdp::params::gpl_artifacts_download_url
  $mysql_connector_url = "${download_url}/mysql-connector-java-5.1.18.zip"
}
