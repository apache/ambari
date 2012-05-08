class hdp-zookeeper::params() inherits hdp::params 
{
  $conf_dir = $hdp::params::zk_conf_dir

  $zk_user = hdp_default("zk_user","zookeeper")
  
  $zk_log_dir = hdp_default("zk_log_dir","/var/log/zookeeper")
  $zk_data_dir = hdp_default("zk_data_dir","/var/lib/zookeeper/data")
  $zk_pid_dir = hdp_default("zk_pid_dir","/var/run/zookeeper")
  $zk_pid_file = "${zk_pid_dir}/zookeeper_server.pid"
}
