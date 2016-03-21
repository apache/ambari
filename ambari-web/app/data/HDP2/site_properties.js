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

var App = require('app');

var hdp2properties = [
  //***************************************** HDP stack **************************************
  /**********************************************HDFS***************************************/
  {
    "name": "dfs.namenode.checkpoint.dir",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "category": "SECONDARY_NAMENODE",
    "index": 1
  },
  {
    "name": "dfs.namenode.checkpoint.period",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 3
  },
  {
    "name": "dfs.namenode.name.dir",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "category": "NAMENODE",
    "index": 1
  },
  {
    "name": "dfs.webhdfs.enabled",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 0
  },
  {
    "name": "dfs.datanode.failed.volumes.tolerated",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 3
  },
  {
    "name": "dfs.datanode.data.dir",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 1
  },
  {
    "name": "dfs.datanode.data.dir.perm",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "nfs.file.dump.dir",
    "category": "NFS_GATEWAY",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 1
  },
  {
    "name": "dfs.namenode.accesstime.precision",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 2
  },
  {
    "name": "nfs.exports.allowed.hosts",
    "category": "NFS_GATEWAY",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 3
  },
  {
    "name": "dfs.replication",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.du.reserved",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 2
  },
  {
    "name": "namenode_heapsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 2
  },
  {
    "name": "namenode_opt_newsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 3
  },
  {
    "name": "namenode_opt_permsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 5
  },
  {
    "name": "namenode_opt_maxpermsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 6
  },
  {
    "name": "namenode_opt_maxnewsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 4
  },
  {
    "name": "dtnode_heapsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "DATANODE",
    "index": 2
  },
  {
    "name": "nfsgateway_heapsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NFS_GATEWAY",
    "index": 1
  },
  {
    "name": "hadoop_heapsize",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "General",
    "index": 1
  },

/**********************************************YARN***************************************/
  {
    "name": "yarn.acl.enable",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "RESOURCEMANAGER"
  },
  {
    "name": "yarn.admin.acl",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "RESOURCEMANAGER"
  },
  {
    "name": "yarn.log-aggregation-enable",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "RESOURCEMANAGER"
  },
  {
    "name": "yarn.resourcemanager.scheduler.class",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "CapacityScheduler"
  },
  {
    "name": "yarn.scheduler.minimum-allocation-mb",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "CapacityScheduler"
  },
  {
    "name": "yarn.scheduler.maximum-allocation-mb",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "CapacityScheduler"
  },
  {
    "name": "yarn.nodemanager.resource.memory-mb",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.vmem-pmem-ratio",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.linux-container-executor.group",
    "serviceName": "YARN",
    "category": "NODEMANAGER",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.nodemanager.log-dirs",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.local-dirs",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.remote-app-log-dir",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.remote-app-log-dir-suffix",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.aux-services",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.log.retain-second",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn_heapsize",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "General",
    "index": 0
  },
  {
    "name": "resourcemanager_heapsize",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "RESOURCEMANAGER",
    "index": 1
  },
  {
    "name": "nodemanager_heapsize",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "NODEMANAGER",
    "index": 1
  },
  {
    "name": "apptimelineserver_heapsize",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "APP_TIMELINE_SERVER",
    "index": 1
  },
/**********************************************MAPREDUCE2***************************************/
  {
    "name": "mapreduce.map.memory.mb",
    "category": "General",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.reduce.memory.mb",
    "category": "General",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.task.io.sort.mb",
    "category": "General",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-site.xml"
  },
  {
    "name": "jobhistory_heapsize",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-env.xml",
    "category": "HISTORYSERVER",
    "index": 1
  },
/**********************************************oozie-site***************************************/
  {
    "name": "oozie.db.schema.name",
    "category": "OOZIE_SERVER",
    "serviceName": "OOZIE",
    "filename": "oozie-site.xml",
    "index": 4
  },
  {
    "name": "oozie.service.JPAService.jdbc.username",
    "category": "OOZIE_SERVER",
    "serviceName": "OOZIE",
    "filename": "oozie-site.xml",
    "index": 5
  },
  {
    "name": "oozie.service.JPAService.jdbc.password",
    "category": "OOZIE_SERVER",
    "serviceName": "OOZIE",
    "filename": "oozie-site.xml",
    "index": 6
  },
  {
    "name": "oozie.service.JPAService.jdbc.driver", // the default value of this property is overriden in code
    "category": "OOZIE_SERVER",
    "serviceName": "OOZIE",
    "filename": "oozie-site.xml",
    "index": 7
  },
  {
    "name": "oozie.service.JPAService.jdbc.url",
    "category": "OOZIE_SERVER",
    "serviceName": "OOZIE",
    "filename": "oozie-site.xml",
    "index": 8
  },
  {
    "name": "oozie_database",
    "options": [
      {
        displayName: 'New Derby Database',
        hidden: false
      },
      {
        displayName: 'Existing MySQL Database',
        hidden: false
      },
      {
        displayName: 'Existing PostgreSQL Database',
        hidden: false
      },
      {
        displayName: 'Existing Oracle Database',
        hidden: false
      },
      {
        displayName: 'Existing SQL Anywhere Database',
        hidden: App.get('currentStackName') !== 'SAPHD' && (App.get('currentStackName') !== 'HDP' || !App.get('isHadoop23Stack'))
      }
    ],
    "displayType": "radio button",
    "radioName": "oozie-database",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "OOZIE_SERVER",
    "index": 2
  },
  {
    "name": "oozie_data_dir",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "OOZIE_SERVER",
    "index": 9
  },
  {
    "name": "oozie_hostname",
    "displayName": "Database Host",
    "displayType": "host",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "OOZIE_SERVER",
    "index": 3
  },

/**********************************************HIVE***************************************/
  {
    "name": "javax.jdo.option.ConnectionDriverName",  // the default value is overwritten in code
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "index": 7
  },
  {
    "name": "hive.metastore.heapsize",  // the default value is overwritten in code
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "index": 11
  },
  {
    "name": "hive.heapsize",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "category": "HIVE_SERVER2",
    "index": 9
  },
  {
    "name": "hive.client.heapsize",
    "category": "HIVE_CLIENT",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "index": 10
  },
  {
    "name": "javax.jdo.option.ConnectionUserName",
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "index": 5
  },
  {
    "name": "javax.jdo.option.ConnectionPassword",
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "index": 6
  },
  {
    "name": "javax.jdo.option.ConnectionURL",
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "index": 8
  },
  {
    "name": "ambari.hive.db.schema.name",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "category": "HIVE_METASTORE",
    "index": 4
  },
  {
    "name": "hive_database",
    "options": [
      {
        displayName: 'New MySQL Database',
        hidden: false
      },
      {
        displayName: 'Existing MySQL Database',
        hidden: false
      },
      {
        displayName: 'Existing PostgreSQL Database',
        hidden: false
      },
      {
        displayName: 'Existing Oracle Database',
        hidden: false
      },
      {
        displayName: 'Existing SQL Anywhere Database',
        hidden: App.get('currentStackName') !== 'SAPHD' && (App.get('currentStackName') !== 'HDP' || !App.get('isHadoop23Stack'))
      }
    ],
    "displayType": "radio button",
    "radioName": "hive-database",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "HIVE_METASTORE",
    "index": 2
  },
  {
    "name": "hive_hostname",
    "displayName": "Database Host",
    "description": "Specify the host on which the database is hosted",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "host",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "isVisible": true,
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "HIVE_METASTORE",
    "index": 3
  },
  {
    "name": "hcat_log_dir",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "Advanced webhcat-env"
  },
  {
    "name": "hcat_pid_dir",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "Advanced webhcat-env"
  },
  {
    "name": "hive_database_type",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "HIVE_METASTORE"
  },
/**********************************************TEZ*****************************************/
  {
    "name": "tez.am.resource.memory.mb",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.java.opts",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.grouping.split-waves",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.grouping.min-size",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.grouping.max-size",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.log.level",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-input.compress.codec",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-input.is-compressed",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-output.compress.codec",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-output.should-compress",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },

/**********************************************HBASE***************************************/
  {
    "name": "hbase.hstore.compactionThreshold",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 0
  },
  {
    "name": "hfile.block.cache.size",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 1
  },
  {
    "name": "hbase.hregion.max.filesize",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 2
  },
  {
    "name": "hbase.regionserver.handler.count",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 2
  },
  {
    "name": "hbase.hregion.majorcompaction",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 3
  },
  {
    "name": "hbase.hregion.memstore.block.multiplier",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 4
  },
  {
    "name": "hbase.hregion.memstore.flush.size",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 5
  },
  {
    "name": "hbase.client.scanner.caching",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 3
  },
  {
    "name": "zookeeper.session.timeout",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 4
  },
  {
    "name": "hbase.client.keyvalue.maxsize",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 5
  },
  {
    "name": "hbase_master_heapsize",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_MASTER",
    "index": 1
  },
  {
    "name": "hbase_regionserver_heapsize",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_REGIONSERVER",
    "index": 1
  },
  {
    "name": "hbase_regionserver_xmn_max",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_REGIONSERVER",
    "index": 6
  },
  {
    "name": "hbase_regionserver_xmn_ratio",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_REGIONSERVER",
    "index": 7
  },

/**********************************************storm-site***************************************/
  {
    "name": "storm.zookeeper.root",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.local.dir",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.servers",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.port",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.session.timeout",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.connection.timeout",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.retry.times",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.retry.interval",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.retry.intervalceiling.millis",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.cluster.mode",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.local.mode.zmq",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.thrift.transport",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.messaging.transport",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.messaging.netty.buffer_size",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "serviceName": "STORM",
    "category": "General",
    "name": "storm.messaging.netty.max_retries",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.max_wait_ms",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.min_wait_ms",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.server_worker_threads",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.client_worker_threads",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "nimbus.host",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.thrift.port",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.thrift.max_buffer_size",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.childopts",
    "serviceName": "STORM",
    "category": "NIMBUS",
    "filename": "storm-site.xml"
  },
  {
    "name": "nimbus.task.timeout.secs",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.supervisor.timeout.secs",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.monitor.freq.secs",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.cleanup.inbox.freq.secs",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.inbox.jar.expiration.secs",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.task.launch.secs",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.reassign",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.file.copy.expiration.secs",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.topology.validator",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "supervisor.slots.ports",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "SUPERVISOR"
  },
  {
    "name": "supervisor.childopts",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.worker.start.timeout.secs",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.worker.timeout.secs",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.monitor.frequency.secs",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.heartbeat.frequency.secs",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.port",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.worker.threads",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.queue.size",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.invocations.port",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.request.timeout.secs",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.childopts",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "ui.port",
    "category": "STORM_UI_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "ui.childopts",
    "category": "STORM_UI_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
/*********************************************oozie-site for Falcon*****************************/
  {
    "name": "oozie.service.ELService.ext.functions.coord-job-submit-instances",
    "category": "Falcon - Oozie integration",
    "serviceName": "FALCON",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-action-create-inst",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-action-create",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-job-submit-data",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-action-start",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-sla-submit",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-sla-create",
    "filename": "oozie-site.xml"
  },

  // Runtime properties
  {
    "name": "*.domain",
    "category": "FalconRuntimeSite",
    "serviceName": "FALCON",
    "filename": "falcon-runtime.properties.xml"

  },
  {
    "name": "*.log.cleanup.frequency.minutes.retention",
    "category": "FalconRuntimeSite",
    "serviceName": "FALCON",
    "filename": "falcon-runtime.properties.xml"
  },
  {
    "name": "*.log.cleanup.frequency.hours.retention",
    "category": "FalconRuntimeSite",
    "serviceName": "FALCON",
    "filename": "falcon-runtime.properties.xml"
  },
  {
    "name": "*.log.cleanup.frequency.days.retention",
    "category": "FalconRuntimeSite",
    "serviceName": "FALCON",
    "filename": "falcon-runtime.properties.xml"
  },
  {
    "name": "*.log.cleanup.frequency.months.retention",
    "category": "FalconRuntimeSite",
    "serviceName": "FALCON",
    "filename": "falcon-runtime.properties.xml"
  },

  //  Startup properties

  {
    "name": "*.domain",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.workflow.engine.impl",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.oozie.process.workflow.builder",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.oozie.feed.workflow.builder",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.SchedulableEntityManager.impl",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.ConfigSyncService.impl",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.ProcessInstanceManager.impl",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.catalog.service.impl",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.application.services",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.configstore.listeners",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.broker.impl.class",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.shared.libs",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.config.store.uri",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.system.lib.location",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.retry.recorder.path",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.cleanup.service.frequency",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.broker.url",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.broker.ttlInMins",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.entity.topic",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.max.retry.failure.count",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.internal.queue.size",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.authentication.type",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.http.authentication.type",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.http.authentication.token.validity",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.http.authentication.signature.secret",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.http.authentication.simple.anonymous.allowed",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.http.authentication.kerberos.name.rules",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.http.authentication.blacklisted.users",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },

  // Falcon Graph and Storage
  {
    "name": "*.falcon.graph.storage.directory",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.graph.serialize.path",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.graph.preserve.history",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },

/**********************************************KNOX*****************************************/
  {
    "name": "knox_master_secret",
    "serviceName": "KNOX",
    "filename": "knox-env.xml",
    "category": "KNOX_GATEWAY"
  },

/********************************************* KAFKA *****************************/
  {
    "name": "log.dirs",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 1
  },
  {
    "name": "port",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 2
  },
    {
    "name": "listeners",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER"
  },
  {
    "name": "log.roll.hours",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "log.retention.hours",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "zookeeper.connect",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "kafka_pid_dir",
    "serviceName": "KAFKA",
    "filename": "kafka-env.xml",
    "index": 0
  },

/********************************************* ACCUMULO *****************************/
  {
    "name": "accumulo_instance_name",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "General"
  },
  {
    "name": "accumulo_root_password",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "General"
  },
  {
    "name": "trace.user",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "General"
  },
  {
    "name": "trace_password",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "General"
  },
  {
    "name": "instance_secret",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "General"
  },
  {
    "name": "instance.volumes",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 0
  },
  {
    "name": "instance.zookeeper.host",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 1
  },
  {
    "name": "instance.zookeeper.timeout",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 2
  },
  {
    "name": "master.port.client",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 3
  },
  {
    "name": "tserver.port.client",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 4
  },
  {
    "name": "monitor.port.client",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 5
  },
  {
    "name": "monitor.port.log4j",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 6
  },
  {
    "name": "gc.port.client",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 7
  },
  {
    "name": "trace.port.client",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 8
  },
  {
    "name": "tserver.memory.maps.native.enabled",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 9
  },
  {
    "name": "general.classpaths",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "index": 10
  },

/*******************************************kerberos***********************************/
  {
    "name": "kdc_type",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "KDC",
    "index": 0
  },
  {
    "name": "kdc_host",
    "displayType": "supportTextConnection",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "KDC",
    "index": 1
  },
  {
    "name": "realm",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "KDC",
    "index": 2
  },
  {
    "name": "ldap_url",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "KDC",
    "index": 3
  },
  {
    "name": "container_dn",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "KDC",
    "index": 4
  },
  {
    "name": "domains",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "KDC",
    "index": 5
  },
  {
    "name": "manage_identities",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 0
  },
  {
    "name": "install_packages",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 1
  },
  {
    "name": "executable_search_paths",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 2
  },
  {
    "name": "encryption_types",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 3
  },
  {
    "name": "password_length",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 4
  },
  {
    "name": "password_min_lowercase_letters",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 5
  },
  {
    "name": "password_min_uppercase_letters",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 6
  },
  {
    "name": "password_min_digits",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 7
  },
  {
    "name": "password_min_punctuation",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 8
  },
  {
    "name": "password_min_whitespace",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 9
  },
  {
    "name": "service_check_principal_name",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 10
  },
  {
    "name": "ad_create_attributes_template",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 11
  },
  {
    "name": "kdc_create_attributes",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 12
  },
  {
    "name": "case_insensitive_username_rules",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 13
  },
  {
    "name": "manage_auth_to_local",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 14
  },
  {
    "name": "admin_server_host",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Kadmin",
    "index": 0
  },
  {
    "name": "admin_principal",
    "displayName": "Admin principal",
    "description": "Admin principal used to create principals and export key tabs (e.g. admin/admin@EXAMPLE.COM).",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Kadmin",
    "index": 1
  },
  {
    "name": "admin_password",
    "displayName": "Admin password",
    "displayType": "password",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Kadmin",
    "index": 2
  },
  {
    "name": "manage_krb5_conf",
    "dependentConfigPattern": "CATEGORY",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Advanced krb5-conf",
    "index": 0
  },
  {
    "name": "conf_dir",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Advanced krb5-conf",
    "index": 1
  },
  {
    "name": "content",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Advanced krb5-conf",
    "index": 2
  },
/********************************************* flume-agent *****************************/
  {
    "name": "content",
    "serviceName": "FLUME",
    "category": "FLUME_HANDLER",
    "filename": "flume-conf.xml"
  },
  {
    "name": "flume_conf_dir",
    "serviceName": "FLUME",
    "filename": "flume-env.xml",
    "index": 0
  },
  {
    "name": "flume_log_dir",
    "serviceName": "FLUME",
    "filename": "flume-env.xml",
    "index": 1
  },

/**********************************************ZOOKEEPER***************************************/
  {
    "name": "dataDir",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 1
  },
  {
    "name": "tickTime",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 2
  },
  {
    "name": "initLimit",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 3
  },
  {
    "name": "syncLimit",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 4
  },
  {
    "name": "clientPort",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 5
  },
  {
    "name": "zk_log_dir",
    "serviceName": "ZOOKEEPER",
    "filename": "zookeeper-env.xml",
    "index": 0
  },
  {
    "name": "zk_pid_dir",
    "serviceName": "ZOOKEEPER",
    "filename": "zookeeper-env.xml",
    "index": 1
  },
/**********************************************FALCON***************************************/
  {
    "name": "falcon_port",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "FALCON_SERVER"
  },
  {
    "name": "falcon_local_dir",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "FALCON_SERVER"
  },
  {
    "name": "falcon_store_uri",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "FALCON_SERVER"
  },
/************************************************AMBARI_METRICS******************************************/
  {
    "name": "timeline.metrics.service.operation.mode",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "General",
    "index": 1
  },
  {
    "name": "metrics_collector_log_dir",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 2
  },
  {
    "name": "metrics_collector_pid_dir",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 3
  },
  {
    "name": "metrics_monitor_log_dir",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 4
  },
  {
    "name": "metrics_monitor_pid_dir",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 5
  },
  {
    "name": "metrics_grafana_username",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-grafana-env.xml",
    "category": "General",
    "index": 6
  },
  {
    "name": "metrics_grafana_password",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-grafana-env.xml",
    "category": "General",
    "index": 7
  },
  {
    "name": "timeline.metrics.aggregator.checkpoint.dir",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 17
  },
  {
    "name": "timeline.metrics.cluster.aggregator.hourly.checkpointCutOffMultiplier",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 16
  },
  {
    "name": "timeline.metrics.cluster.aggregator.hourly.disabled",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 14
  },
  {
    "name": "timeline.metrics.cluster.aggregator.hourly.interval",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 15
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.checkpointCutOffMultiplier",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 13
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.disabled",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 10
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.interval",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 11
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.timeslice.interval",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 12
  },
  {
    "name": "timeline.metrics.host.aggregator.hourly.checkpointCutOffMultiplier",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 9
  },
  {
    "name": "timeline.metrics.host.aggregator.hourly.disabled",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 7
  },
  {
    "name": "timeline.metrics.host.aggregator.hourly.interval",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 8
  },
  {
    "name": "timeline.metrics.host.aggregator.minute.checkpointCutOffMultiplier",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 6
  },
  {
    "name": "timeline.metrics.host.aggregator.minute.disabled",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 4
  },
  {
    "name": "timeline.metrics.host.aggregator.minute.interval",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 5
  },
  {
    "name": "timeline.metrics.service.checkpointDelay",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 1
  },
  {
    "name": "timeline.metrics.service.default.result.limit",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 2
  },
  {
    "name": "timeline.metrics.service.resultset.fetchSize",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 3
  },
/************************************************Kerberos Descriptor******************************************/
  {
    "name": "smokeuser_principal_name",
    "displayName": "Smoke user principal",
    "category": "Ambari Principals",
    "filename": "cluster-env.xml",
    "index": 1
  },
  {
    "name": "smokeuser_keytab",
    "displayName": "Smoke user keytab",
    "category": "Ambari Principals",
    "filename": "cluster-env.xml",
    "index": 2
  },
  {
    "name": "hdfs_principal_name",
    "displayName": "HDFS user principal",
    "category": "Ambari Principals",
    "filename": "hadoop-env.xml",
    "index": 3
  },
  {
    "name": "hdfs_user_keytab",
    "displayName": "HDFS user keytab",
    "category": "Ambari Principals",
    "filename": "hadoop-env.xml",
    "index": 4
  },
  {
    "name": "hbase_principal_name",
    "displayName": "HBase user principal",
    "category": "Ambari Principals",
    "filename": "hbase-env.xml",
    "index": 5
  },
  {
    "name": "hbase_user_keytab",
    "displayName": "HBase user keytab",
    "category": "Ambari Principals",
    "filename": "hbase-env.xml",
    "index": 6
  },
  {
    "name": "accumulo_principal_name",
    "displayName": "Accumulo user principal",
    "category": "Ambari Principals",
    "filename": "accumulo-env.xml",
    "index": 7
  },
  {
    "name": "accumulo_user_keytab",
    "displayName": "Accumulo user keytab",
    "category": "Ambari Principals",
    "filename": "accumulo-env.xml",
    "index": 8
  },
  {
    "name": "spark.history.kerberos.principal",
    "displayName": "Spark user principal",
    "category": "Ambari Principals",
    "filename": "spark-env.xml",
    "index": 9
  },
  {
    "name": "spark.history.kerberos.keytab",
    "displayName": "Spark user keytab",
    "category": "Ambari Principals",
    "filename": "spark-env.xml",
    "index": 10
  },
  {
    "name": "storm_principal_name",
    "displayName": "Storm user principal",
    "category": "Ambari Principals",
    "filename": "storm-env.xml",
    "index": 11
  },
  {
    "name": "storm_keytab",
    "displayName": "Storm user keytab",
    "category": "Ambari Principals",
    "filename": "storm-env.xml",
    "index": 12
  }
].concat(require('data/HDP2/alert_notification')).concat(require('data/HDP2/gluster_fs_properties'));

if (App.get('isHadoopWindowsStack')) {
  var excludedWindowsConfigs = [
    'dfs.client.read.shortcircuit',
    'knox_pid_dir',
    'ignore_groupsusers_create',
    'hive_database',
    'oozie_database',
    'override_hbase_uid'
  ];

  hdp2properties = hdp2properties.filter(function (item) {
    return !excludedWindowsConfigs.contains(item.name);
  });

  hdp2properties.push(
    {
      "name": "hadoop.user.name",
      "serviceName": "MISC",
      "filename": "cluster-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["HDFS"],
      "index": 0
    },
    {
      "name": "hadoop.user.password",
      "serviceName": "MISC",
      "filename": "cluster-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["HDFS"],
      "index": 1
    },
    {
      "name": "hive_database",
      "options": [
        {
          displayName: 'Existing MSSQL Server database with SQL authentication',
          hidden: false
        },
        {
          displayName: 'Existing MSSQL Server database with integrated authentication',
          hidden: false
        }
      ],
      "displayType": "radio button",
      "radioName": "hive-database",
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "HIVE_METASTORE",
      "index": 2
    },
    {
      "name": "oozie_database",
      "recommendedValue": "Existing MSSQL Server database with SQL authentication",
      "options": [
        {
          displayName: 'Existing MSSQL Server database with SQL authentication',
          hidden: false
        },
        {
          displayName: 'Existing MSSQL Server database with integrated authentication',
          hidden: false
        }
      ],
      "displayType": "radio button",
      "radioName": "oozie-database",
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "OOZIE_SERVER",
      "index": 2
    }
  );
}

var atsProperties = [
  {
    "name": "yarn.timeline-service.enabled",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.leveldb-timeline-store.path",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.leveldb-timeline-store.ttl-interval-ms",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.store-class",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.ttl-enable",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.ttl-ms",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.generic-application-history.store-class",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.webapp.address",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.webapp.https.address",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.address",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  }
];

if (!App.get('isHadoop20Stack')) {
  hdp2properties.pushObjects(atsProperties);
}

module.exports =
{
  "configProperties": hdp2properties
};
