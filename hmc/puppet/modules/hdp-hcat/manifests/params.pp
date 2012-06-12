class hdp-hcat::params() inherits hdp::params
{
  $hcat_conf_dir = $hdp::params::hcat_conf_dir

  $hcat_metastore_port = hdp_default("hcat_metastore_port",9933)
  $hcat_lib = hdp_default("hcat_lib","/usr/lib/hcatalog/share/hcatalog") #TODO: should I remove and just use hcat_dbroot

  ### hcat-env
  $hcat_dbroot = hdp_default("hadoop/hcat-env/hcat_dbroot",$hcat_lib)

  $hcat_log_dir = hdp_default("hadoop/hcat-env/hcat_log_dir","/var/log/hcatalog")

  $hcat_pid_dir = hdp_default("hadoop/hcat-env/hcat_pid_dir","/var/run/hcatalog")

}
