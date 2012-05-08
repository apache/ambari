class hdp-ganglia::params() inherits hdp::params
{
  $ganglia_conf_dir = "/etc/ganglia/hdp"
  $ganglia_runtime_dir = "/var/run/ganglia/hdp"

  $ganglia_shell_cmds_dir = hdp_default("ganglia_shell_cmd_dir","/usr/libexec/hdp/ganglia")
  
  $gmetad_user = "nobody" #TODO: cannot be changed yet
  
  $gmond_user = hdp_default("gmond_user","nobody")

  $webserver_group = hdp_default("hadoop/gangliaEnv/webserver_group","apache")
}