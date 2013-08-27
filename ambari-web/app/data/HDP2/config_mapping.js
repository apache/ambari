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

var configs = [
  /**********************************************core-site***************************************/
  {
    "name": "fs.defaultFS",
    "templateName": ["namenode_host"],
    "foreignKey": null,
    "value": "hdfs://<templateName[0]>:8020",
    "filename": "core-site.xml"
  },
  {
    "name": "dfs.namenode.checkpoint.dir",
    "templateName": ["dfs_namenode_checkpoint_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "dfs.namenode.checkpoint.period",
    "templateName": ["dfs_namenode_checkpoint_period"],
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
    "templateName": ["dfs_namenode_checkpoint_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hivemetastore_host"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["oozie_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["oozieserver_host"],
    "foreignKey": ["oozie_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hivemetastore_host"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable" : true
  },
  /**********************************************hdfs-site***************************************/
  {
    "name": "dfs.namenode.name.dir",
    "templateName": ["dfs_namenode_name_dir"],
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
    "name": "dfs.datanode.data.dir",
    "templateName": ["dfs_datanode_data_dir"],
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
    "name": "dfs.namenode.http-address",
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
    "name": "dfs.namenode.secondary.http-address",
    "templateName": ["snamenode_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:50090",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.https.namenode.https-address",
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
  /**********************************************oozie-site***************************************/
  {
    "name": "oozie.base.url",
    "templateName": ["oozieserver_host"],
    "foreignKey": null,
    "value": "http://<templateName[0]>:11000/oozie",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.JPAService.create.db.schema",
    "templateName": [],
    "foreignKey": null,
    "value": "false",  // this is always false
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.db.schema.name",
    "templateName": ['oozie_database_name'],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.JPAService.jdbc.driver",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.derby.jdbc.EmbeddedDriver",  // this value is overwritten in code
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.JPAService.jdbc.username",
    "templateName": ['oozie_metastore_user_name'],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.JPAService.jdbc.password",
    "templateName": ['oozie_metastore_user_passwd'],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.JPAService.jdbc.url",
    "templateName": ["oozie_jdbc_connection_url"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml"
  },
  /**********************************************hive-site***************************************/
  {
    "name": "javax.jdo.option.ConnectionDriverName",
    "templateName": [],
    "foreignKey": null,
    "value": "com.mysql.jdbc.Driver",  // this value is overwritten in code
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
    "name": "javax.jdo.option.ConnectionURL",
    "templateName": ["hive_jdbc_connection_url"],
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
/**********************************************yarn-site***************************************/
  {
    "name": "yarn.resourcemanager.hostname",
    "templateName": ["rm_host"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.resourcemanager.webapp.address",
    "templateName": ["rm_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:8088",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.resourcemanager.resource-tracker.address",
    "templateName": ["rm_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:8025",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.resourcemanager.scheduler.address",
    "templateName": ["rm_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:8030",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.resourcemanager.address",
    "templateName": ["rm_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:8050",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.resourcemanager.admin.address",
    "templateName": ["rm_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:8141",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.log.server.url",
    "templateName": ["hs_host"],
    "foreignKey": null,
    "value": "http://<templateName[0]>:19888/jobhistory/logs",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.nodemanager.local-dirs",
    "templateName": ["yarn_nodemanager_local-dirs"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.nodemanager.log-dirs",
    "templateName": ["yarn_nodemanager_log-dirs"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml"
  },
/**********************************************mapred-site***************************************/
  {
    "name": "mapreduce.jobhistory.webapp.address",
    "templateName": ["hs_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:19888",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.jobhistory.address",
    "templateName": ["hs_host"],
    "foreignKey": null,
    "value": "<templateName[0]>:10020",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.map.memory.mb",
    "templateName": ["mapreduce_map_memory_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.reduce.memory.mb",
    "templateName": ["mapreduce_reduce_memory_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.task.io.sort.mb",
    "templateName": ["mapreduce_task_io_sort_mb"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.jobtracker.system.dir",
    "templateName": ["mapreduce_jobtracker_system_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.hosts",
    "templateName": ["hadoop_conf_dir", "mapred_hosts_include"],
    "foreignKey": null,
    "value": "<templateName[0]>/<templateName[1]>",
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
    "name": "mapred.userlog.retain.hours",
    "templateName": ["mapreduce_userlog_retainhours"],
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
    "name": "mapreduce.tasktracker.map.tasks.maximum",
    "templateName": ["mapreduce_tasktracker_map_tasks_maximum"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.hosts.exclude",
    "templateName": ["hadoop_conf_dir", "mapred_hosts_exclude"],
    "foreignKey": null,
    "value": "<templateName[0]>/<templateName[1]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapred.jobtracker.taskScheduler",
    "templateName": ["scheduler_name"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.tasktracker.healthchecker.script.path",
    "templateName": ["mapred_jobstatus_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },

  /**********************************************hbase-site***************************************/
  {
    "name": "hbase.rootdir",
    "templateName": ["namenode_host", "hbase_hdfs_root_dir"],
    "foreignKey": null,
    "value": "hdfs://<templateName[0]>:8020<templateName[1]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.tmp.dir",
    "templateName": ["hbase_tmp_dir"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
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
    "filename": "hdfs-site.xml"
  },
  {
    "name": "hbase.security.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "simple",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.rpc.engine",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.ipc.WritableRpcEngine",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.security.authorization",
    "templateName": [],
    "foreignKey": null,
    "value": "false",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.zookeeper.quorum",
    "templateName": ["zookeeperserver_hosts"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "zookeeper.znode.parent",
    "templateName": [],
    "foreignKey": null,
    "value": "/hbase-unsecure",
    "filename": "hbase-site.xml"
  },
  /**********************************************webhcat-site***************************************/
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
  }
];

/**
 * Configs consists of 2 types: Computed values, which cannot be modified by user
 * and overridable values, which user can modify. We provide interface how to get all of this
 * configs separately
 * @type {Object}
 */
module.exports = {
  all : function(){
    return configs.slice(0);
  },
  overridable: function(){
    return configs.filterProperty('foreignKey');
  },
  computed: function(){
    return configs.filterProperty('foreignKey', null);
  }
};
