class hdp-ganglia::params() inherits hdp::params
{
  $ganglia_conf_dir = "/etc/ganglia/hdp"
  $ganglia_runtime_dir = "/var/run/ganglia/hdp"

  $ganglia_shell_cmds_dir = hdp_default("ganglia_shell_cmd_dir","/usr/libexec/hdp/ganglia")
  
  $gmetad_user = "nobody" #TODO: cannot be changed yet
  
  $gmond_user = hdp_default("gmond_user","nobody")

  $webserver_group = hdp_default("hadoop/gangliaEnv/webserver_group","apache")


  $omit_hbase_master = hdp_default("omit_hbase_master",hdp_is_empty($hdp::params::hbase_master_host))
  $omit_jobtracker = hdp_default("omit_jobtracker",hdp_is_empty($hdp::params::jtnode_host))
  $omit_namenode = hdp_default("omit_namenode",hdp_is_empty($hdp::params::namenode_host))
  $omit_slaves = hdp_default("omit_slaves",hdp_is_empty($hdp::params::slave_hosts))
}