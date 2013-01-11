/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports = [
  {
    "name": "fs.default.name",
    "templateName": ["namenode_host"],
    "foreignKey": null,
    "value": "hdfs://<templateName[0]>:8020",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.checkpoint.dir",
    "templateName": ["fs_checkpoint_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.checkpoint.period",
    "templateName": ["fs_checkpoint_period"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.checkpoint.size",
    "templateName": ["fs_checkpoint_size"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "fs.checkpoint.edits.dir",
    "templateName": [],
    "foreignKey": ["fs.checkpoint.dir"],
    "value": "<foreignKey[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hivemetastore_host"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["oozie_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["oozieserver_host"],
    "foreignKey": ["oozie_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hivemetastore_host"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "dfs.name.dir",
    "templateName": ["dfs_name_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.support.append",
    "templateName": ["dfs_support_append"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.webhdfs.enabled",
    "templateName": ["dfs_webhdfs_enabled"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.failed.volumes.tolerated",
    "templateName": ["dfs_datanode_failed_volume_tolerated"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.block.local-path-access.user",
    "templateName": ["dfs_block_local_path_access_user"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.data.dir",
    "templateName": ["dfs_data_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.hosts.exclude",
    "templateName": ["hadoop_conf_dir", "dfs_exclude"],
    "foreignKey": null,
    "value": "<templateName[0]>\/<templateName[1]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.hosts",
    "templateName": ["hadoop_conf_dir", "dfs_include"],
    "foreignKey": null,
    "value": "<templateName[0]>\/<templateName[1]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.replication",
    "templateName": ["dfs_replication"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.address",
    "templateName": ["dfs_datanode_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.http.address",
    "templateName": ["dfs_datanode_http_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.http.address",
    "templateName": ["namenode_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50070",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.du.reserved",
    "templateName": ["datanode_du_reserved"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.namenode.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "nn\/_HOST@<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "nn\/_HOST@<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.namenode.kerberos.https.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "host\/_HOST@<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.https.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "host\/_HOST@<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.secondary.http.address",
    "templateName": ["snamenode_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50090",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.web.authentication.kerberos.keytab",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/spnego.service.keytab",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "dn\/_HOST@<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.namenode.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/nn.service.keytab",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.secondary.namenode.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/nn.service.keytab",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/dn.service.keytab",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.https.address",
    "templateName": ["namenode_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50470",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.data.dir.perm",
    "templateName": ["dfs_datanode_data_dir_perm"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  /*
   {
   "name": "dfs.namenode.kerberos.internal.spnego.principal",
   "templateName": [],
   "foreignKey": ["dfs.web.authentication.kerberos.principal"],
   "value": "<foreignKey[0]>",
   "filename": "hdfs-site.xml"
   },
   {
   "name": "dfs.secondary.namenode.kerberos.internal.spnego.principal",
   "templateName": [],
   "foreignKey": ["dfs.web.authentication.kerberos.principal"],
   "value": "<foreignKey[0]>",
   "filename": "hdfs-site.xml"
   },
   */
  {
    "name": "mapred.local.dir",
    "templateName": ["mapred_local_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  /*
   {
   "name": "oozie.service.StoreService.jdbc.url",
   "templateName": ["oozie_data_dir"],
   "foreignKey": ["oozie.db.schema.name"],
   "value": "<templateName[0]>\/<foreignKey[0]>",
   "filename": "oozie-site.xml"
   },
   */
  {
    "name": "oozie.base.url",
    "templateName": ["oozieserver_host"],
    "foreignKey": null,
    "value": "http://<templateName[0]>:11000/oozie",
    "filename": "oozie-site.xml"
  },
  /*
   {
   "name": "oozie.service.JPAService.jdbc.password",
   "templateName": [],
   "foreignKey": null,
   "value": " ",
   "filename": "oozie-site.xml"
   },
   {
   "name": "oozie.db.schema.name",
   "templateName": [],
   "foreignKey": null,
   "value": "oozie",
   "filename": "oozie-site.xml"
   },
   {
   "name": "oozie.service.JPAService.jdbc.url",
   "templateName": [],
   "foreignKey": null,
   "value": "jdbc:derby:/var/data/oozie/oozie-db;create=true",
   "filename": "oozie-site.xml"
   },
   {
   "name": "oozie.action.ssh.http.command.post.options",
   "templateName": [],
   "foreignKey": null,
   "value": " ",
   "filename": "oozie-site.xml"
   },
   */
  {
    "name": "javax.jdo.option.ConnectionURL",
    "templateName": ["hive_mysql_hostname", "hive_database_name"],
    "foreignKey": null,
    "value": "jdbc:mysql://<templateName[0]>\/<templateName[1]>?createDatabaseIfNotExist=true",
    "filename": "hive-site.xml"
  },
  {
    "name": "javax.jdo.option.ConnectionUserName",
    "templateName": ["hive_metastore_user_name"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml"
  },
  {
    "name": "javax.jdo.option.ConnectionPassword",
    "templateName": ["hive_metastore_user_passwd"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.metastore.uris",
    "templateName": ["hivemetastore_host"],
    "foreignKey": null,
    "value": "thrift://<templateName[0]>:9083",
    "filename": "hive-site.xml"
  },
  {
    "name": "mapred.jobtracker.taskScheduler",
    "templateName": ["scheduler_name"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.tasktracker.map.tasks.maximum",
    "templateName": ["mapred_map_tasks_max"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.tasktracker.red.tasks.maximum",
    "templateName": ["mapred_red_tasks_max"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.cluster.reduce.memory.mb",
    "templateName": ["mapred_cluster_red_mem_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.job.map.memory.mb",
    "templateName": ["mapred_job_map_mem_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.cluster.max.map.memory.mb",
    "templateName": ["mapred_cluster_max_map_mem_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.cluster.max.reduce.memory.mb",
    "templateName": ["mapred_cluster_max_red_mem_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.job.reduce.memory.mb",
    "templateName": ["mapred_job_red_mem_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.hosts",
    "templateName": ["hadoop_conf_dir", "mapred_hosts_include"],
    "foreignKey": null,
    "value": "<templateName[0]>\/<templateName[1]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.hosts.exclude",
    "templateName": ["hadoop_conf_dir", "mapred_hosts_exclude"],
    "foreignKey": null,
    "value": "<templateName[0]>\/<templateName[1]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.healthChecker.script.path",
    "templateName": ["mapred_jobstatus_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.job.tracker.persist.jobstatus.dir",
    "templateName": ["hadoop_conf_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>\/health_check",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.child.java.opts",
    "templateName": ["mapred_child_java_opts_sz"],
    "foreignKey": null,
    "value": "-server -Xmx<templateName[0]>m -Djava.net.preferIPv4Stack=true",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.cluster.map.memory.mb",
    "templateName": ["mapred_cluster_map_mem_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "io.sort.mb",
    "templateName": ["io_sort_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "io.sort.spill.percent",
    "templateName": ["io_sort_spill_percent"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.system.dir",
    "templateName": ["mapred_system_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.job.tracker",
    "templateName": ["jobtracker_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50300",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.job.tracker.http.address",
    "templateName": ["jobtracker_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50030",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.userlog.retain.hours",
    "templateName": ["mapreduce_userlog_retainhours"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.jobtracker.maxtasks.per.job",
    "templateName": ["maxtasks_per_job"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.task.tracker.task-controller",
    "templateName": ["task_controller"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.jobtracker.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "jt\/_HOST@<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.tasktracker.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "tt\/_HOST@<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.jobtracker.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/jt.service.keytab",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.tasktracker.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/tt.service.keytab",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.history.server.embedded",
    "templateName": [],
    "foreignKey": null,
    "value": "false",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.history.server.http.address",
    "templateName": ["jobtracker_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:51111",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.jobhistory.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "jt\/_HOST@<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.jobhistory.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/jt.service.keytab",
    "filename": "mapred-site.xml"
  },
  {
    "name": "hbase.rootdir",
    "templateName": ["namenode_host", "hbase_hdfs_root_dir"],
    "foreignKey": null,
    "value": "hdfs:\/\/<templateName[0]>:8020<templateName[1]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.tmp.dir",
    "templateName": ["hbase_tmp_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  /*
   {
   "name": "hbase.master.info.bindAddress",
   "templateName": ["hbasemaster.host"],
   "foreignKey": null,
   "value": "<templateName[0]>",
   "filename": "hbase-site.xml"
   },
   */
  {
    "name": "hbase.regionserver.global.memstore.upperLimit",
    "templateName": ["regionserver_memstore_upperlimit"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hstore.blockingStoreFiles",
    "templateName": ["hstore_blockingstorefiles"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hstore.compactionThreshold",
    "templateName": ["hstore_compactionthreshold"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hfile.block.cache.size",
    "templateName": ["hfile_blockcache_size"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hregion.max.filesize",
    "templateName": ["hstorefile_maxsize"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.regionserver.handler.count",
    "templateName": ["regionserver_handlers"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hregion.majorcompaction",
    "templateName": ["hregion_majorcompaction"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.regionserver.global.memstore.lowerLimit",
    "templateName": ["regionserver_memstore_lowerlimit"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hregion.memstore.block.multiplier",
    "templateName": ["hregion_blockmultiplier"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hregion.memstore.mslab.enabled",
    "templateName": ["regionserver_memstore_lab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hregion.memstore.flush.size",
    "templateName": ["hregion_memstoreflushsize"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.client.scanner.caching",
    "templateName": ["client_scannercaching"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.client.scanner.caching",
    "templateName": ["client_scannercaching"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.cluster.distributed",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.zookeeper.property.clientPort",
    "templateName": [],
    "foreignKey": null,
    "value": "2181",
    "filename": "hbase-site.xml"
  },
  {
    "name": "zookeeper.session.timeout",
    "templateName": ["zookeeper_sessiontimeout"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.client.keyvalue.maxsize",
    "templateName": ["hfile_max_keyvalue_size"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.master.keytab.file",
    "templateName": ["keytab_path"],
    "foreignKey": null,
    "value": "<templateName[0]>\/hm.service.keytab",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.master.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "hm\/_HOST@<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.regionserver.kerberos.principal",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "rs\/_HOST@<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  /*
  {
    "name": "hbase.coprocessor.region.classes",
    "templateName": ["preloaded_regioncoprocessor_classes"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  */
  {
    "name": "dfs.support.append",
    "templateName": ["hdfs_support_append"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "dfs.client.read.shortcircuit",
    "templateName": ["hdfs_enable_shortcircuit_read"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "dfs.client.read.shortcircuit.skip.checksum",
    "templateName": ["hdfs_enable_shortcircuit_skipchecksum"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "templeton.hive.properties",
    "templateName": ["hivemetastore_host"],
    "foreignKey": null,
    "value": "hive.metastore.local=false,hive.metastore.uris=thrift://<templateName[0]>:9083,hive.metastore.sasl.enabled=yes,hive.metastore.execute.setugi=true",
    "filename": "webhcat-site.xml"
  },
  {
    "name": "templeton.zookeeper.hosts",
    "templateName": ["zookeeperserver_hosts"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "webhcat-site.xml"
  },
  {
    "name": "hbase.zookeeper.quorum",
    "templateName": ["zookeeperserver_hosts"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  }
];