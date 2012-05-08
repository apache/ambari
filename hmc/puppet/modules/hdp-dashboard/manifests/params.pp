class hdp-dashboard::params()
{
  
  $conf_dir = "/usr/share/hdp/dashboard/dataServices/conf/" #cannot change since hard coded in rpm

  $hdp_cluster_name = hdp_default("hadoop/cluster_configuration/hdp_cluster_name")
  $scheduler_name = hdp_default("hadoop/cluster_configuration/scheduler_name")
}
