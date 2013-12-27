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
class hdp-hbase::params() inherits hdp::params 
{
  
  ####### users
  $hbase_user = $hdp::params::hbase_user
  
  ### hbase-env
  $hadoop_conf_dir = hdp_default("hadoop_conf_dir")
  $conf_dir = $hdp::params::hbase_conf_dir

  $hbase_log_dir = hdp_default("hbase_log_dir","/var/log/hbase")

  $hbase_master_heapsize = hdp_default("hbase_master_heapsize","1000m")

  $hbase_pid_dir = hdp_default("hbase_pid_dir","/var/run/hbase")

  $hbase_regionserver_heapsize = hdp_default("hbase_regionserver_heapsize","1000m")

  $hbase_regionserver_xmn_size = hdp_calc_xmn_from_xms("$hbase_regionserver_heapsize","0.2","512")

  ### hbase-site.xml
  $hbase_tmp_dir = hdp_default("hbase-site/hbase.tmp.dir","$hbase_log_dir")


  #TODO: check if any of these 'hdfs' vars need to be euated with vars in hdp-hadoop
  $hdfs_enable_shortcircuit_read = hdp_default("hbase-site/hdfs.enable.shortcircuit.read",true)

  $hdfs_enable_shortcircuit_skipchecksum = hdp_default("hbase-site/hdfs.enable.shortcircuit.skipchecksum",false)

  $hdfs_support_append = hdp_default("hbase-site/hdfs.support.append",true)

  $hfile_blockcache_size = hdp_default("hbase-site/hfile.blockcache.size","0.25")

  $hfile_max_keyvalue_size = hdp_default("hbase-site/hfile.max.keyvalue.size",10485760)

  $zookeeper_sessiontimeout = hdp_default("hbase-site/zookeeper.sessiontimeout",60000)

  $client_scannercaching = hdp_default("hbase-site/client.scannercaching",100)

  $hstore_blockingstorefiles = hdp_default("hbase-site/hstore.blockingstorefiles",7)

  $hstore_compactionthreshold = hdp_default("hbase-site/hstore.compactionthreshold",3)

  $hstorefile_maxsize = hdp_default("hbase-site/hstorefile.maxsize",1073741824)

  $hregion_blockmultiplier = hdp_default("hbase-site/hregion.blockmultiplier",2)

  $hregion_memstoreflushsize = hdp_default("hbase-site/hregion.memstoreflushsize",134217728)

  $regionserver_handlers = hdp_default("hbase-site/regionserver.handlers", 30)

  $hregion_majorcompaction = hdp_default("hbase-site/hregion.majorcompaction", 86400000)

  $preloaded_mastercoprocessor_classes = hdp_default("hbase-site/preloaded.mastercoprocessor.classes")

  $preloaded_regioncoprocessor_classes = hdp_default("hbase-site/preloaded.regioncoprocessor.classes")

  $regionserver_memstore_lab = hdp_default("hbase-site/regionserver.memstore.lab",true)

  $regionserver_memstore_lowerlimit = hdp_default("hbase-site/regionserver.memstore.lowerlimit","0.35")

  $regionserver_memstore_upperlimit = hdp_default("hbase-site/regionserver.memstore.upperlimit","0.4")

  $keytab_path = hdp_default("keytab_path","/etc/security/keytabs")
  $hbase_client_jaas_config_file = hdp_default("hbase_client_jaas_config_file", "${conf_dir}/hbase_client_jaas.conf")
  $hbase_master_jaas_config_file = hdp_default("hbase_master_jaas_config_file", "${conf_dir}/hbase_master_jaas.conf")
  $hbase_regionserver_jaas_config_file = hdp_default("hbase_regionserver_jaas_config_file", "${conf_dir}/hbase_regionserver_jaas.conf")

  $hbase_master_keytab_path = hdp_default("hbase-site/hbase.master.keytab.file", "${keytab_path}/hbase.service.keytab")
  $hbase_master_principal = hdp_default("hbase-site/hbase.master.kerberos.principal", "hbase/_HOST@${kerberos_domain}")
  $hbase_regionserver_keytab_path = hdp_default("hbase-site/hbase.regionserver.keytab.file", "${keytab_path}/hbase.service.keytab")
  $hbase_regionserver_principal = hdp_default("hbase-site/hbase.regionserver.kerberos.principal", "hbase/_HOST@${kerberos_domain}")

  $hbase_primary_name = hdp_default("hbase_primary_name", "hbase")
  $hostname = $hdp::params::hostname
  if ($use_hostname_in_principal) {
    $hbase_master_jaas_princ = "${hbase_master_primary_name}/${hostname}@${kerberos_domain}"
    $hbase_regionserver_jaas_princ = "${hbase_regionserver_primary_name}/${hostname}@${kerberos_domain}"
  } else {
    $hbase_master_jaas_princ = "${hbase_master_principal_name}@${kerberos_domain}"
    $hbase_regionserver_jaas_princ = "${hbase_regionserver_primary_name}@${kerberos_domain}"
  }

  if (hdp_get_major_stack_version($hdp::params::stack_version) >= 2) {
    $metric-prop-file-name = "hadoop-metrics2-hbase.properties"
  } else {
    $metric-prop-file-name = "hadoop-metrics.properties"
  }
  $smokeuser_permissions = hdp_default("smokeuser_permissions", "RWXCA")
}
