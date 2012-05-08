class hdp-templeton::params() inherits hdp::params
{
  $templeton_user = hdp_default("templeton_user","templeton")

  ###pig and hive tar url connector
  $download_url = $hdp::params::apache_artifacts_download_url

  $pig_tar_url = "${download_url}/pig-0.9.2.tar.gz"
  $pig_tar_name = hdp_default("pig_tar_name","pig.tar.gz")
  $hive_tar_url = "${download_url}/hive-0.8.1.tar.gz"
  $hive_tar_name = hdp_default("hive_tar_name","hive.tar.gz")

  ### templeton-env
  $conf_dir = hdp_default("hadoop/templeton-env/conf_dir","/etc/templeton")

  ### templeton-env
  $templeton_log_dir = hdp_default("hadoop/templeton-env/templeton_log_dir","/var/log/templeton")

  $templeton_pid_dir = hdp_default("hadoop/templeton-env/templeton_pid_dir","/var/run/templeton")

  $templeton_jar_name= hdp_default("hadoop/templeton-env/templeton_jar_name","templeton-0.1.3.jar")
 
  $hadoop_prefix = hdp_default("hadoop/templeton-env/hadoop_prefix","/usr")
  $hive_prefix = hdp_default("hadoop/templeton-env/hive_prefix","/usr")
  
  ### templeton-site
  $hadoop_conf_dir = hdp_default("hadoop/templeton-site/hadoop_conf_dir")
  $templeton_jar = hdp_default("hadoop/templeton-site/templeton_jar","/usr/share/templeton/templeton-0.1.3.jar")
  $zookeeper_jar = hdp_default("hadoop/templeton-site/zookeeper_jar","/usr/share/zookeeper/zookeeper-*.jar")
  $pig_tar_gz = hdp_default("hadoop/templeton-site/pig_tar_gz","$pig_tar_name")
  $pig_tar_name_hdfs = hdp_default("hadoop/templeton-site/pig_tar_name_hdfs","pig-0.9.2")

  $hive_tar_gz = hdp_default("hadoop/templeton-site/hive_tar_gz","$hive_tar_name")
  $hive_metastore_sasl_enabled = hdp_default("hadoop/templeton-site/hive_metastore_sasl_enabled","no")

  $templeton_metastore_principal = hdp_default("hadoop/templeton-site/templeton_metastore_principal")

  $keytab_path = hdp_default("hadoop/templeton-site/keytab_path")
  
}
