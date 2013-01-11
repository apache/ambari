#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
class hdp-hadoop::params(
) inherits hdp::params 
{

  ##TODO: for testing in masterless mode
  $use_preconditions = false
  ####  
  $conf_dir = $hdp::params::hadoop_conf_dir 

  ####### users

  $mapred_user = $hdp::params::mapred_user
  $hdfs_user = $hdp::params::hdfs_user
  
  ##### security related
  $keytab_path = hdp_default("keytab_path","/etc/security/keytabs")
 
  if ($hdp::params::security_enabled == true) {
    $enable_security_authorization = true
    $security_type = "kerberos"
    $task_controller = "org.apache.hadoop.mapred.LinuxTaskController"
    $dfs_datanode_address = 1019
    $dfs_datanode_http_address = 1022
  } else {
    $enable_security_authorization = false
    $security_type = "simple"
    $task_controller = "org.apache.hadoop.mapred.DefaultTaskController"
    $dfs_datanode_address = 50010
    $dfs_datanode_http_address = 50075
  }

  ### hadoop-env
  
  $dtnode_heapsize = hdp_default("hadoop/hadoop-env/dtnode_heapsize","1024m")
  $ttnode_heapsize = hdp_default("hadoop/hadoop-env/ttnode_heapsize","1024m")

  $hadoop_heapsize = hdp_default("hadoop/hadoop-env/hadoop_heapsize","1024m")

  $hadoop_logdirprefix = hdp_default("hadoop/hadoop-env/hadoop_logdirprefix","/var/log/hadoop")

  $hadoop_piddirprefix = hdp_default("hadoop/hadoop-env/hadoop_piddirprefix","/var/run/hadoop")
  $run_dir = $hadoop_piddirprefix

  $namenode_formatted_mark_dir = "${run_dir}/hdfs/namenode/formatted/"

  $jtnode_heapsize = hdp_default("hadoop/hadoop-env/jtnode_heapsize","1024m")

  $jtnode_opt_maxnewsize = hdp_default("hadoop/hadoop-env/jtnode_opt_maxnewsize","200m")

  $jtnode_opt_newsize = hdp_default("hadoop/hadoop-env/jtnode_opt_newsize","200m")

  $namenode_heapsize = hdp_default("hadoop/hadoop-env/namenode_heapsize","1024m")

  $namenode_opt_maxnewsize = hdp_default("hadoop/hadoop-env/namenode_opt_maxnewsize","640m")

  $namenode_opt_newsize = hdp_default("hadoop/hadoop-env/namenode_opt_newsize","640m")
  
  ### compression related
  if (($hdp::params::lzo_enabled == true) and ($hdp::params::snappy_enabled == true)) {
    $mapred_compress_map_output = true
    $compression_codecs =  "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec,org.apache.hadoop.io.compress.SnappyCodec"
    $mapred_map_output_compression_codec = "org.apache.hadoop.io.compress.SnappyCodec"
  } elsif ($hdp::params::snappy_enabled == true) {
    $mapred_compress_map_output = true
    $compression_codecs = "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec" 
    $mapred_map_output_compression_codec = "org.apache.hadoop.io.compress.SnappyCodec"
  } elsif ($hdp::params::lzo_enabled == true) {
    $mapred_compress_map_output = true
    $compression_codecs = "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec"
    $mapred_map_output_compression_codec = "com.hadoop.compression.lzo.LzoCodec"
  } else { 
    $mapred_compress_map_output = false
    $compression_codecs = "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec"
    $mapred_map_output_compression_codec = "org.apache.hadoop.io.compress.DefaultCodec"
  }

  ### core-site
  $fs_checkpoint_dir = hdp_default("hadoop/core-site/fs_checkpoint_dir","/tmp/hadoop-hdfs/dfs/namesecondary")

  $proxyuser_group = hdp_default("hadoop/core-site/proxyuser_group","users")

  ### hdfs-site
  $datanode_du_reserved = hdp_default("hadoop/hdfs-site/datanode_du_reserved",1073741824)

  $dfs_block_local_path_access_user = hdp_default("hadoop/hdfs-site/dfs_block_local_path_access_user","hbase")

  $dfs_data_dir = $hdp::params::dfs_data_dir

  $dfs_datanode_data_dir_perm = hdp_default("hadoop/hdfs-site/dfs_datanode_data_dir_perm",750)

  $dfs_datanode_failed_volume_tolerated = hdp_default("hadoop/hdfs-site/dfs_datanode_failed_volume_tolerated",0)

  $dfs_exclude = hdp_default("hadoop/hdfs-site/dfs_exclude","dfs.exclude")

  $dfs_include = hdp_default("hadoop/hdfs-site/dfs_include","dfs.include")
  
  $dfs_name_dir = hdp_default("hadoop/hdfs-site/dfs_name_dir","/tmp/hadoop-hdfs/dfs/name")
  
  $dfs_replication = hdp_default("hadoop/hdfs-site/dfs_replication",3)

  $dfs_support_append = hdp_default("hadoop/hdfs-site/dfs_support_append",true)

  $dfs_webhdfs_enabled = hdp_default("hadoop/hdfs-site/dfs_webhdfs_enabled",false)


 ######### mapred #######
   ### mapred-site

  $mapred_system_dir = '/mapred/system'

  $io_sort_mb = hdp_default("hadoop/mapred-site/io_sort_mb","200")

  $io_sort_spill_percent = hdp_default("hadoop/mapred-site/io_sort_spill_percent","0.9")

  $mapred_child_java_opts_sz = hdp_default("hadoop/mapred-site/mapred_child_java_opts_sz","-Xmx768m")

  $mapred_cluster_map_mem_mb = hdp_default("hadoop/mapred-site/mapred_cluster_map_mem_mb","-1")

  $mapred_cluster_max_map_mem_mb = hdp_default("hadoop/mapred-site/mapred_cluster_max_map_mem_mb","-1")

  $mapred_cluster_max_red_mem_mb = hdp_default("hadoop/mapred-site/mapred_cluster_max_red_mem_mb","-1")

  $mapred_cluster_red_mem_mb = hdp_default("hadoop/mapred-site/mapred_cluster_red_mem_mb","-1")

  $mapred_hosts_exclude = hdp_default("hadoop/mapred-site/mapred_hosts_exclude","mapred.exclude")

  $mapred_hosts_include = hdp_default("hadoop/mapred-site/mapred_hosts_include","mapred.include")

  $mapred_job_map_mem_mb = hdp_default("hadoop/mapred-site/mapred_job_map_mem_mb","-1")

  $mapred_job_red_mem_mb = hdp_default("hadoop/mapred-site/mapred_job_red_mem_mb","-1")

  $mapred_jobstatus_dir = hdp_default("hadoop/mapred-site/mapred_jobstatus_dir","file:////mapred/jobstatus")

  $mapred_local_dir = hdp_default("hadoop/mapred-site/mapred_local_dir","/tmp/hadoop-mapred/mapred/local")
   
  $mapred_map_tasks_max = hdp_default("hadoop/mapred-site/mapred_map_tasks_max",4)

  $mapred_red_tasks_max = hdp_default("hadoop/mapred-site/mapred_red_tasks_max",4)

  $mapreduce_userlog_retainhours = hdp_default("hadoop/mapred-site/mapreduce_userlog_retainhours",24)

  $maxtasks_per_job = hdp_default("hadoop/mapred-site/maxtasks_per_job","-1")

  $scheduler_name = hdp_default("hadoop/mapred-site/scheduler_name","org.apache.hadoop.mapred.CapacityTaskScheduler")

  #### health_check

  $security_enabled = $hdp::params::security_enabled

  $task_bin_exe = hdp_default("hadoop/health_check/task_bin_exe")

  $rca_enabled = hdp_default("rca_enabled", false)
  if ($rca_enabled == true) {
    $rca_prefix = ""
  } else {
    $rca_prefix = "###"
  }
  $ambari_db_server_host = hdp_default("ambari_db_server_host", "localhost")
}
