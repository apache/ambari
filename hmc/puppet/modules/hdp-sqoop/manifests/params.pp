class hdp-sqoop::params() inherits hdp::params
{
  $conf_dir = $hdp::params::sqoop_conf_dir

  $hbase_home = hdp_default("hbase_home","/usr")
  $hive_home = hdp_default("hive_home","/usr")
  $zoo_conf_dir = $hdp::params::zk_conf_dir 
  $sqoop_lib = hdp_default("sqoop_lib","/usr/lib/sqoop/lib/") #TODO: should I remove and just use sqoop_dbroot
}
