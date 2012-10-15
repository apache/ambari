class hdp2-ganglia::params() inherits hdp2::params
{
  $ganglia_conf_dir = "/etc/ganglia/hdp"
  $ganglia_runtime_dir = "/var/run/ganglia/hdp"

  $ganglia_shell_cmds_dir = hdp_default("ganglia_shell_cmd_dir","/usr/libexec/hdp/ganglia")
  
  $gmetad_user = $hdp2::params::gmetad_user
  $gmond_user = $hdp2::params::gmond_user

  $webserver_group = hdp_default("hadoop/gangliaEnv/webserver_group","apache")
}
