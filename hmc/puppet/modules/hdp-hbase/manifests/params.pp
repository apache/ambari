class hdp-hbase::params() inherits hdp::params 
{
  
  ####### users
  $hbase_user = $hdp::params::hbase_user
  
  ### hbase-env
  $hadoop_conf_dir = hdp_default("hadoop/hbase-env/hadoop_conf_dir")
  $conf_dir = $hdp::params::hbase_conf_dir

  $hbase_log_dir = hdp_default("hadoop/hbase-env/hbase_log_dir","/var/log/hbase")

  $hbase_master_heapsize = hdp_default("hadoop/hbase-env/hbase_master_heapsize","1000m")

  $hbase_pid_dir = hdp_default("hadoop/hbase-env/hbase_pid_dir","/var/run/hbase")

  $hbase_regionserver_heapsize = hdp_default("hadoop/hbase-env/hbase_regionserver_heapsize","1000m")


  ### hbase-site.xml
  $hbase_hdfs_root_dir = hdp_default("hadoop/hbase-site/hbase_hdfs_root_dir","/apps/hbase/data")

  $hbase_tmp_dir = hdp_default("hadoop/hbase-site/hbase_tmp_dir","$hbase_log_dir")


  #TODO: check if any of these 'hdfs' vars need to be euated with vars in hdp-hadoop
  $hdfs_enable_shortcircuit_read = hdp_default("hadoop/hbase-site/hdfs_enable_shortcircuit_read",true)

  $hdfs_enable_shortcircuit_skipchecksum = hdp_default("hadoop/hbase-site/hdfs_enable_shortcircuit_skipchecksum","false")

  $hdfs_support_append = hdp_default("hadoop/hbase-site/hdfs_support_append",true)

  $hfile_blockcache_size = hdp_default("hadoop/hbase-site/hfile_blockcache_size","0.25")

  $hstore_blockingstorefiles = hdp_default("hadoop/hbase-site/hstore_blockingstorefiles",7)

  $hstore_compactionthreshold = hdp_default("hadoop/hbase-site/hstore_compactionthreshold",3)

  $hstorefile_maxsize = hdp_default("hadoop/hbase-site/hstorefile_maxsize",268435456)

  $preloaded_mastercoprocessor_classes = hdp_default("hadoop/hbase-site/preloaded_mastercoprocessor_classes")

  $preloaded_regioncoprocessor_classes = hdp_default("hadoop/hbase-site/preloaded_regioncoprocessor_classes")

  $regionserver_memstore_lab = hdp_default("hadoop/hbase-site/regionserver_memstore_lab",true)

  $regionserver_memstore_lowerlimit = hdp_default("hadoop/hbase-site/regionserver_memstore_lowerlimit","0.35")

  $regionserver_memstore_upperlimit = hdp_default("hadoop/hbase-site/regionserver_memstore_upperlimit","0.4")

}
