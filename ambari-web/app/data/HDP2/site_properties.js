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

module.exports =
{
  "configProperties": [
  //***************************************** HDP stack **************************************
  /**********************************************HDFS***************************************/
    {
      "id": "site property",
      "name": "dfs.namenode.checkpoint.dir",
      "displayName": "SecondaryNameNode Checkpoint directory",
      "defaultDirectory": "/hadoop/hdfs/namesecondary",
      "displayType": "directory",
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "SNameNode",
      "index": 1
    },
    {
      "id": "site property",
      "name": "dfs.namenode.checkpoint.period",
      "displayName": "HDFS Maximum Checkpoint Delay",
      "displayType": "int",
      "unit": "seconds",
      "category": "General",
      "serviceName": "HDFS",
      "index": 3
    },
    {
      "id": "site property",
      "name": "dfs.namenode.name.dir",
      "displayName": "NameNode directories",
      "defaultDirectory": "/hadoop/hdfs/namenode",
      "displayType": "directories",
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "NameNode",
      "index": 1
    },
    {
      "id": "site property",
      "name": "dfs.webhdfs.enabled",
      "displayName": "WebHDFS enabled",
      "displayType": "checkbox",
      "isOverridable": false,
      "category": "General",
      "serviceName": "HDFS",
      "index": 0
    },
    {
      "id": "site property",
      "name": "dfs.datanode.failed.volumes.tolerated",
      "displayName": "DataNode volumes failure toleration",
      "displayType": "int",
      "category": "DataNode",
      "serviceName": "HDFS",
      "index": 3
    },
    {
      "id": "site property",
      "name": "dfs.datanode.data.dir",
      "displayName": "DataNode directories",
      "defaultDirectory": "/hadoop/hdfs/data",
      "displayType": "directories",
      "category": "DataNode",
      "serviceName": "HDFS",
      "index": 1
    },
    {
      "id": "site property",
      "name": "dfs.datanode.data.dir.perm",
      "displayName": "DataNode directories permission",
      "displayType": "int",
      "category": "DataNode",
      "serviceName": "HDFS"
    },
    {
      "id": "site property",
      "name": "dfs.replication",
      "displayName": "Block replication",
      "displayType": "int",
      "category": "Advanced",
      "serviceName": "HDFS"
    },
    {
      "id": "site property",
      "name": "dfs.datanode.du.reserved",
      "displayName": "Reserved space for HDFS",
      "displayType": "int",
      "unit": "bytes",
      "category": "General",
      "serviceName": "HDFS",
      "index": 2
    },
    {
      "id": "site property",
      "name": "dfs.client.read.shortcircuit",
      "displayName": "HDFS Short-circuit read",
      "displayType": "checkbox",
      "category": "Advanced",
      "serviceName": "HDFS"

    },

  /**********************************************YARN***************************************/
    {
      "id": "site property",
      "name": "yarn.acl.enable",
      "displayName": "yarn.acl.enable",
      "displayType": "checkbox",
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {
      "id": "site property",
      "name": "yarn.admin.acl",
      "displayName": "yarn.admin.acl",
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {
      "id": "site property",
      "name": "yarn.log-aggregation-enable",
      "displayName": "yarn.log-aggregation-enable",
      "displayType": "checkbox",
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {
      "id": "site property",
      "name": "yarn.resourcemanager.scheduler.class",
      "displayName": "yarn.resourcemanager.scheduler.class",
      "serviceName": "YARN",
      "category": "CapacityScheduler"
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.minimum-allocation-mb",
      "displayName": "yarn.scheduler.minimum-allocation-mb",
      "displayType": "int",
      "serviceName": "YARN",
      "category": "CapacityScheduler"
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.maximum-allocation-mb",
      "displayName": "yarn.scheduler.maximum-allocation-mb",
      "displayType": "int",
      "serviceName": "YARN",
      "category": "CapacityScheduler"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.resource.memory-mb",
      "displayName": "yarn.nodemanager.resource.memory-mb",
      "displayType": "int",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.vmem-pmem-ratio",
      "displayName": "yarn.nodemanager.vmem-pmem-ratio",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.linux-container-executor.group",
      "displayName": "yarn.nodemanager.linux-container-executor.group",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.log-dirs",
      "displayName": "yarn.nodemanager.log-dirs",
      "defaultDirectory": "/hadoop/yarn/log",
      "displayType": "directories",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.local-dirs",
      "displayName": "yarn.nodemanager.local-dirs",
      "defaultDirectory": "/hadoop/yarn/local",
      "displayType": "directories",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.remote-app-log-dir",
      "displayName": "yarn.nodemanager.remote-app-log-dir",
      "displayType": "directory",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.remote-app-log-dir-suffix",
      "displayName": "yarn.nodemanager.remote-app-log-dir-suffix",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.aux-services",
      "displayName": "yarn.nodemanager.aux-services",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.log.retain-second",
      "displayName": "yarn.nodemanager.log.retain-second",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.log.server.url",
      "displayName": "yarn.log.server.url",
      "category": "Advanced",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.leveldb-timeline-store.path",
      "displayName": "yarn.timeline-service.leveldb-timeline-store.path",
      "category": "AppTimelineServer",
      "displayType": "directory",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.store-class",
      "displayName": "yarn.timeline-service.store-class",
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.generic-application-history.store-class",
      "displayName": "yarn.timeline-service.generic-application-history.store-class",
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.webapp.address",
      "displayName": "yarn.timeline-service.webapp.address",
      "displayType": "string",
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.webapp.https.address",
      "displayName": "yarn.timeline-service.webapp.https.address",
      "displayType": "string",
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.address",
      "displayName": "yarn.timeline-service.address",
      "displayType": "string",
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
  /**********************************************MAPREDUCE2***************************************/
    {
      "id": "site property",
      "name": "mapreduce.map.memory.mb",
      "displayName": "Default virtual memory for a job's map-task",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "site property",
      "name": "mapreduce.reduce.memory.mb",
      "displayName": "Default virtual memory for a job's reduce-task",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "site property",
      "name": "mapreduce.task.io.sort.mb",
      "displayName": "Map-side sort buffer memory",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "site property",
      "name": "hadoop.security.auth_to_local",
      "displayName": "hadoop.security.auth_to_local",
      "displayType": "multiLine",
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "id": "site property",
      "name": "yarn.app.mapreduce.am.resource.mb",
      "displayName": "yarn.app.mapreduce.am.resource.mb",
      "displayType": "int",
      "category": "Advanced",
      "serviceName": "MAPREDUCE2"
    },

  /**********************************************oozie-site***************************************/
    {
      "id": "site property",
      "name": "oozie.db.schema.name",
      "displayName": "Database Name",
      "isOverridable": false,
      "displayType": "host",
      "isObserved": true,
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "index": 3
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.username",
      "displayName": "Database Username",
      "isOverridable": false,
      "displayType": "host",
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "index": 4
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.password",
      "displayName": "Database Password",
      "isOverridable": false,
      "displayType": "password",
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "filename": "oozie-site.xml",
      "index": 5
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.driver", // the default value of this property is overriden in code
      "displayName": "JDBC Driver Class",
      "isOverridable": false,
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "index": 6
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.url",
      "displayName": "Database URL",
      "isOverridable": false,
      "displayType": "advanced",
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "index": 7
    },

  /**********************************************hive-site***************************************/
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionDriverName",  // the default value is overwritten in code
      "displayName": "JDBC Driver Class",
      "isOverridable": false,
      "category": "Hive Metastore",
      "serviceName": "HIVE",
      "index": 7
    },
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionUserName",
      "displayName": "Database Username",
      "displayType": "host",
      "isOverridable": false,
      "category": "Hive Metastore",
      "serviceName": "HIVE",
      "index": 5
    },
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionPassword",
      "displayName": "Database Password",
      "displayType": "password",
      "isOverridable": false,
      "category": "Hive Metastore",
      "serviceName": "HIVE",
      "index": 6
    },
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionURL",
      "displayName": "Database URL",
      "displayType": "advanced",
      "isOverridable": false,
      "category": "Hive Metastore",
      "serviceName": "HIVE",
      "index": 8
    },
    {
      "id": "site property",
      "name": "ambari.hive.db.schema.name",
      "displayName": "Database Name",
      "displayType": "host",
      "isOverridable": false,
      "isObserved": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 4
    },

  /**********************************************hbase-site***************************************/
    {
      "id": "site property",
      "name": "hbase.tmp.dir",
      "displayName": "HBase local directory",
      "defaultDirectory": "/hadoop/hbase",
      "displayType": "directory",
      "category": "Advanced",
      "serviceName": "HBASE"

    },
    {
      "id": "site property",
      "name": "hbase.regionserver.global.memstore.upperLimit",
      "displayName": "hbase.regionserver.global.memstore.upperLimit",
      "displayType": "float",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.regionserver.global.memstore.lowerLimit",
      "displayName": "hbase.regionserver.global.memstore.lowerLimit",
      "displayType": "float",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.hstore.blockingStoreFiles",
      "displayName": "hstore blocking storefiles",
      "displayType": "int",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.hstore.compactionThreshold",
      "displayName": "HBase HStore compaction threshold",
      "displayType": "int",
      "category": "General",
      "serviceName": "HBASE",
      "index": 0
    },
    {
      "id": "site property",
      "name": "hfile.block.cache.size",
      "displayName": "HFile block cache size ",
      "displayType": "float",
      "category": "General",
      "serviceName": "HBASE",
      "index": 1
    },
    {
      "id": "site property",
      "name": "hbase.hregion.max.filesize",
      "displayName": "Maximum HStoreFile Size",
      "displayType": "int",
      "unit": "bytes",
      "category": "General",
      "serviceName": "HBASE",
      "index": 2
    },
    {
      "id": "site property",
      "name": "hbase.regionserver.handler.count",
      "displayName": "HBase RegionServer Handler",
      "displayType": "int",
      "category": "RegionServer",
      "serviceName": "HBASE",
      "index": 2
    },
    {
      "id": "site property",
      "name": "hbase.hregion.majorcompaction",
      "displayName": "HBase Region Major Compaction",
      "displayType": "int",
      "unit": "ms",
      "category": "RegionServer",
      "serviceName": "HBASE",
      "index": 3
    },
    {
      "id": "site property",
      "name": "hbase.hregion.memstore.block.multiplier",
      "displayName": "HBase Region Block Multiplier",
      "displayType": "int",
      "category": "RegionServer",
      "serviceName": "HBASE",
      "index": 4
    },
    {
      "id": "site property",
      "name": "hbase.hregion.memstore.mslab.enabled",
      "displayName": "hbase.hregion.memstore.mslab.enabled",
      "displayType": "checkbox",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.hregion.memstore.flush.size",
      "displayName": "HBase Region Memstore Flush Size",
      "displayType": "int",
      "unit": "bytes",
      "category": "RegionServer",
      "serviceName": "HBASE",
      "index": 5
    },
    {
      "id": "site property",
      "name": "hbase.client.scanner.caching",
      "displayName": "HBase Client Scanner Caching",
      "displayType": "int",
      "unit": "rows",
      "category": "General",
      "serviceName": "HBASE",
      "index": 3
    },
    {
      "id": "site property",
      "name": "zookeeper.session.timeout",
      "displayName": "Zookeeper timeout for HBase Session",
      "displayType": "int",
      "unit": "ms",
      "category": "General",
      "serviceName": "HBASE",
      "index": 4
    },
    {
      "id": "site property",
      "name": "hbase.client.keyvalue.maxsize",
      "displayName": "HBase Client Maximum key-value Size",
      "displayType": "int",
      "unit": "bytes",
      "category": "General",
      "serviceName": "HBASE",
      "index": 5
    },
    {
      "id": "site property",
      "name": "hbase.zookeeper.quorum",
      "displayName": "hbase.zookeeper.quorum",
      "displayType": "multiLine",
      "serviceName": "HBASE",
      "category": "Advanced"
    },

  /**********************************************storm-site***************************************/
    {
      "id": "site property",
      "name": "storm.zookeeper.root",
      "displayName": "storm.zookeeper.root",
      "displayType": "directory",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.local.dir",
      "displayName": "storm.local.dir",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.servers",
      "displayName": "storm.zookeeper.servers",
      "displayType": "masterHosts",
      "isOverridable": false,
      "isReconfigurable": false,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.port",
      "displayName": "storm.zookeeper.port",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.session.timeout",
      "displayName": "storm.zookeeper.session.timeout",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.connection.timeout",
      "displayName": "storm.zookeeper.connection.timeout",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.retry.times",
      "displayName": "storm.zookeeper.retry.times",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.retry.interval",
      "displayName": "storm.zookeeper.retry.interval",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General",
      "unit": "ms"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.retry.intervalceiling.millis",
      "displayName": "storm.zookeeper.retry.intervalceiling.millis",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General",
      "unit": "ms"
    },
    {
      "id": "site property",
      "name": "storm.cluster.mode",
      "displayName": "storm.cluster.mode",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.local.mode.zmq",
      "displayName": "storm.local.mode.zmq",
      "displayType": "checkbox",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.thrift.transport",
      "displayName": "storm.thrift.transport",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.messaging.transport",
      "displayName": "storm.messaging.transport",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.buffer_size",
      "name": "storm.messaging.netty.buffer_size",
      "displayType": "int",
      "unit": "bytes"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.max_retries",
      "name": "storm.messaging.netty.max_retries",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.max_wait_ms",
      "name": "storm.messaging.netty.max_wait_ms",
      "displayType": "int",
      "unit": "ms"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.min_wait_ms",
      "name": "storm.messaging.netty.min_wait_ms",
      "displayType": "int",
      "unit": "ms"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.server_worker_threads",
      "name": "storm.messaging.netty.server_worker_threads",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.client_worker_threads",
      "name": "storm.messaging.netty.client_worker_threads",
      "displayType": "int"
    },
    {
      "id": "site property",
      "name": "nimbus.host",
      "displayName": "nimbus.host",
      "displayType": "masterHost",
      "isOverridable": false,
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "nimbus.thrift.port",
      "displayName": "nimbus.thrift.port",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "nimbus.thrift.max_buffer_size",
      "displayName": "nimbus.thrift.max_buffer_size",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "bytes"
    },
    {
      "id": "site property",
      "name": "nimbus.childopts",
      "displayName": "nimbus.childopts",
      "displayType": "multiLine",
      "isOverridable": false,
      "serviceName": "STORM",
      "category": "Nimbus",
      "filename": "storm-site.xml"
    },
    {
      "id": "site property",
      "name": "nimbus.task.timeout.secs",
      "displayName": "nimbus.task.timeout.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.supervisor.timeout.secs",
      "displayName": "nimbus.supervisor.timeout.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.monitor.freq.secs",
      "displayName": "nimbus.monitor.freq.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.cleanup.inbox.freq.secs",
      "displayName": "nimbus.cleanup.inbox.freq.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.inbox.jar.expiration.secs",
      "displayName": "nimbus.inbox.jar.expiration.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.task.launch.secs",
      "displayName": "nimbus.task.launch.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.reassign",
      "displayName": "nimbus.reassign",
      "displayType": "checkbox",
      "isReconfigurable": true,
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "nimbus.file.copy.expiration.secs",
      "displayName": "nimbus.file.copy.expiration.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.topology.validator",
      "displayName": "nimbus.topology.validator",
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "supervisor.slots.ports",
      "displayName": "supervisor.slots.ports",
      "displayType": "string",
      "serviceName": "STORM",
      "category": "Supervisor"
    },
    {
      "id": "site property",
      "isOverrideable": false,
      "serviceName": "STORM",
      "category": "Supervisor",
      "displayName": "supervisor.childopts",
      "name": "supervisor.childopts",
      "displayType": "multiLine",
      "filename": "storm-site.xml"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "Supervisor",
      "displayName": "supervisor.worker.start.timeout.secs",
      "name": "supervisor.worker.start.timeout.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "Supervisor",
      "displayName": "supervisor.worker.timeout.secs",
      "name": "supervisor.worker.timeout.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "Supervisor",
      "displayName": "supervisor.monitor.frequency.secs",
      "name": "supervisor.monitor.frequency.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "Supervisor",
      "displayName": "supervisor.heartbeat.frequency.secs",
      "name": "supervisor.heartbeat.frequency.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.port",
      "name": "drpc.port",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.worker.threads",
      "name": "drpc.worker.threads",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.queue.size",
      "name": "drpc.queue.size",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.invocations.port",
      "name": "drpc.invocations.port",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.request.timeout.secs",
      "name": "drpc.request.timeout.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.childopts",
      "name": "drpc.childopts",
      "displayType": "string"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "StormUIServer",
      "displayName": "ui.port",
      "name": "ui.port",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "StormUIServer",
      "displayName": "ui.childopts",
      "name": "ui.childopts",
      "displayType": "string"
    },
    //@Todo: uncomment following properties when logviewer is treated as different section on storm service page
    /*
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.port",
      "name": "logviewer.port",
      "displayType": "int"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.childopts",
      "name": "logviewer.childopts",
      "displayType": "string"
    },
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.appender.name",
      "name": "logviewer.appender.name",
      "displayType": "string"
    },
    */
    {
      "id": "site property",
      "serviceName": "STORM",
      "category": "Advanced",
      "displayName": "worker.childopts",
      "name": "worker.childopts",
      "displayType": "multiLine",
      "filename": "storm-site.xml"
    },
  /*********************************************oozie-site for Falcon*****************************/
    {
      "id": "site property",
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-job-submit-instances",
      "name": "oozie.service.ELService.ext.functions.coord-job-submit-instances",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "id": "site property",
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-action-create-inst",
      "name": "oozie.service.ELService.ext.functions.coord-action-create-inst",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "id": "site property",
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-action-create",
      "name": "oozie.service.ELService.ext.functions.coord-action-create",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "id": "site property",
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-job-submit-data",
      "name": "oozie.service.ELService.ext.functions.coord-job-submit-data",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "id": "site property",
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-action-start",
      "name": "oozie.service.ELService.ext.functions.coord-action-start",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "id": "site property",
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-sla-submit",
      "name": "oozie.service.ELService.ext.functions.coord-sla-submit",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "id": "site property",
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-sla-create",
      "name": "oozie.service.ELService.ext.functions.coord-sla-create",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },

    // Runtime properties
    {
      "id": "site property",
      "name": "*.domain",
      "displayName": "*.domain",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"

    },
    {
      "id": "site property",
      "name": "*.log.cleanup.frequency.minutes.retention",
      "displayName": "*.log.cleanup.frequency.minutes.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.log.cleanup.frequency.hours.retention",
      "displayName": "*.log.cleanup.frequency.hours.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.log.cleanup.frequency.days.retention",
      "displayName": "*.log.cleanup.frequency.days.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.log.cleanup.frequency.months.retention",
      "displayName": "*.log.cleanup.frequency.months.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },

    //  Startup properties

    {
      "id": "site property",
      "name": "*.domain",
      "displayName": "*.domain",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.workflow.engine.impl",
      "displayName": "*.workflow.engine.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.oozie.process.workflow.builder",
      "displayName": "*.oozie.process.workflow.builder",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.oozie.feed.workflow.builder",
      "displayName": "*.oozie.feed.workflow.builder",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.SchedulableEntityManager.impl",
      "displayName": "*.SchedulableEntityManager.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.ConfigSyncService.impl",
      "displayName": "*.ConfigSyncService.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.ProcessInstanceManager.impl",
      "displayName": "*.ProcessInstanceManager.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.catalog.service.impl",
      "displayName": "*.catalog.service.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.application.services",
      "displayName": "*.application.services",
      "displayType": "multiLine",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "prism.application.services",
      "displayName": "prism.application.services",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.configstore.listeners",
      "displayName": "*.configstore.listeners",
      "displayType": "multiLine",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "prism.configstore.listeners",
      "displayName": "prism.configstore.listeners",
      "displayType": "multiLine",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.broker.impl.class",
      "displayName": "*.broker.impl.class",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.shared.libs",
      "displayName": "*.shared.libs",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.config.store.uri",
      "displayName": "*.config.store.uri",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.system.lib.location",
      "displayName": "*.system.lib.location",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "prism.system.lib.location",
      "displayName": "prism.system.lib.location",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.retry.recorder.path",
      "displayName": "*.retry.recorder.path",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.falcon.cleanup.service.frequency",
      "displayName": "*.falcon.cleanup.service.frequency",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.broker.url",
      "displayName": "*.broker.url",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.broker.ttlInMins",
      "displayName": "*.broker.ttlInMins",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.entity.topic",
      "displayName": "*.entity.topic",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.max.retry.failure.count",
      "displayName": "*.max.retry.failure.count",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.internal.queue.size",
      "displayName": "*.internal.queue.size",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.falcon.authentication.type",
      "displayName": "*.falcon.authentication.type",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.falcon.http.authentication.type",
      "displayName": "*.falcon.http.authentication.type",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.falcon.http.authentication.token.validity",
      "displayName": "*.falcon.http.authentication.token.validity",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.falcon.http.authentication.signature.secret",
      "displayName": "*.falcon.http.authentication.signature.secret",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.falcon.http.authentication.simple.anonymous.allowed",
      "displayName": "*.falcon.http.authentication.simple.anonymous.allowed",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "id": "site property",
      "name": "*.falcon.http.authentication.kerberos.name.rules",
      "displayName": "*.falcon.http.authentication.kerberos.name.rules",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
  /**********************************************webhcat-site***************************************/
    {
      "id": "site property",
      "name": "templeton.zookeeper.hosts",
      "displayName": "templeton.zookeeper.hosts",
      "displayType": "multiLine",
      "serviceName": "WEBHCAT",
      "category": "Advanced"
      /**********************************************log4j.properties***************************************/
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isRequired": false,
      "showLabel": false,
      "serviceName": "HDFS",
      "filename": "hdfs-log4j.xml",
      "category": "AdvancedHDFSLog4j"
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isRequired": false,
      "showLabel": false,
      "serviceName": "YARN",
      "filename": "yarn-log4j.xml",
      "category": "AdvancedYARNLog4j"
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isRequired": false,
      "showLabel": false,
      "serviceName": "HBASE",
      "filename": "hbase-log4j.xml",
      "category": "AdvancedHbaseLog4j"
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isRequired": false,
      "showLabel": false,
      "serviceName": "HIVE",
      "filename": "hive-exec-log4j.xml",
      "category": "AdvancedHiveExecLog4j"
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isRequired": false,
      "showLabel": false,
      "serviceName": "HIVE",
      "filename": "hive-log4j.xml",
      "category": "AdvancedHiveLog4j"
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isOverridable": true,
      "isRequired": false,
      "showLabel": false,
      "serviceName": "OOZIE",
      "filename": "oozie-log4j.xml",
      "category": "AdvancedOozieLog4j"
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isOverridable": true,
      "isRequired": false,
      "showLabel": false,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-log4j.xml",
      "category": "AdvancedZooLog4j"
    },
    {
      "id": "site property",
      "name": "content",
      "displayName": "content",
      "value": "",
      "defaultValue": "",
      "description": "log4j properties",
      "displayType": "content",
      "isRequired": false,
      "showLabel": false,
      "serviceName": "PIG",
      "filename": "pig-log4j.xml",
      "category": "AdvancedPigLog4j"
    },



    //***************************************** GLUSTERFS stack********************************************

    {
      "id": "site property",
      "name": "fs.glusterfs.impl",
      "displayName": "GlusterFS fs impl",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "fs.glusterfs.volname",
      "displayName": "GlusterFS volume name",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "fs.glusterfs.mount",
      "displayName": "GlusterFS mount point",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "fs.glusterfs.getfattrcmd",
      "displayName": "GlusterFS getfattr command",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "fs.AbstractFileSystem.glusterfs.impl",
      "displayName": "GlusterFS Abstract Filesystem declaration",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "gluster.daemon.user",
      "displayName": "GlusterFS Daemon user",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    }

  ]
};
