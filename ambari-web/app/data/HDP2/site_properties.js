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
  /**********************************************HDFS***************************************/
    {
      "id": "site property",
      "name": "dfs.namenode.checkpoint.dir",
      "displayName": "SecondaryNameNode Checkpoint directory",
      "description": "Directory on the local filesystem where the Secondary NameNode should store the temporary images to merge",
      "defaultValue": "",
      "defaultDirectory": "/hadoop/hdfs/namesecondary",
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "SNameNode",
      "index": 1
    },
    {
      "id": "site property",
      "name": "dfs.namenode.checkpoint.period",
      "displayName": "HDFS Maximum Checkpoint Delay",
      "description": "Maximum delay between two consecutive checkpoints for HDFS",
      "defaultValue": "",
      "displayType": "int",
      "unit": "seconds",
      "isVisible": true,
      "category": "General",
      "serviceName": "HDFS",
      "index": 3
    },
    {
      "id": "site property",
      "name": "dfs.namenode.name.dir",
      "displayName": "NameNode directories",
      "description": "NameNode directories for HDFS to store the file system image",
      "defaultValue": "",
      "defaultDirectory": "/hadoop/hdfs/namenode",
      "displayType": "directories",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode",
      "index": 1
    },
    {
      "id": "site property",
      "name": "dfs.webhdfs.enabled",
      "displayName": "WebHDFS enabled",
      "description": "Whether to enable WebHDFS feature",
      "defaultValue": true,
      "displayType": "checkbox",
      "isOverridable": false,
      "isVisible": true,
      "category": "General",
      "serviceName": "HDFS",
      "index": 0
    },
    {
      "id": "site property",
      "name": "dfs.datanode.failed.volumes.tolerated",
      "displayName": "DataNode volumes failure toleration",
      "description": "The number of volumes that are allowed to fail before a DataNode stops offering service",
      "defaultValue": "0",
      "displayType": "int",
      "isVisible": true,
      "category": "DataNode",
      "serviceName": "HDFS",
      "index": 3
    },
    {
      "id": "site property",
      "name": "dfs.datanode.data.dir",
      "displayName": "DataNode directories",
      "description": "DataNode directories for HDFS to store the data blocks",
      "defaultValue": "",
      "defaultDirectory": "/hadoop/hdfs/data",
      "displayType": "directories",
      "isVisible": true,
      "category": "DataNode",
      "serviceName": "HDFS",
      "index": 1
    },
    {
      "id": "site property",
      "name": "dfs.datanode.data.dir.perm",
      "displayName": "DataNode directories permission",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isVisible": true,
      "category": "DataNode",
      "serviceName": "HDFS"
    },
    {
      "id": "site property",
      "name": "dfs.replication",
      "displayName": "Block replication",
      "description": "Default block replication.",
      "displayType": "int",
      "defaultValue": "",
      "isVisible": true,
      "category": "Advanced",
      "serviceName": "HDFS"
    },
    {
      "id": "site property",
      "name": "dfs.datanode.du.reserved",
      "displayName": "Reserved space for HDFS",
      "description": "Reserved space in bytes per volume. Always leave this much space free for non dfs use.",
      "defaultValue": "1073741824",
      "displayType": "int",
      "unit": "bytes",
      "isVisible": true,
      "category": "General",
      "serviceName": "HDFS",
      "index": 2
    },
    {
      "id": "site property",
      "name": "dfs.client.read.shortcircuit",
      "displayName": "HDFS Short-circuit read",
      "description": "This configuration parameter turns on short-circuit local reads.",
      "defaultValue": "",
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
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {
      "id": "site property",
      "name": "yarn.admin.acl",
      "displayName": "yarn.admin.acl",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {
      "id": "site property",
      "name": "yarn.log-aggregation-enable",
      "displayName": "yarn.log-aggregation-enable",
      "displayType": "checkbox",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {
      "id": "site property",
      "name": "yarn.resourcemanager.scheduler.class",
      "displayName": "yarn.resourcemanager.scheduler.class",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "CapacityScheduler"
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.minimum-allocation-mb",
      "displayName": "yarn.scheduler.minimum-allocation-mb",
      "displayType": "int",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "CapacityScheduler"
    },
    {
      "id": "site property",
      "name": "yarn.scheduler.maximum-allocation-mb",
      "displayName": "yarn.scheduler.maximum-allocation-mb",
      "displayType": "int",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "CapacityScheduler"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.resource.memory-mb",
      "displayName": "yarn.nodemanager.resource.memory-mb",
      "displayType": "int",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.vmem-pmem-ratio",
      "displayName": "yarn.nodemanager.vmem-pmem-ratio",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.log-dirs",
      "displayName": "yarn.nodemanager.log-dirs",
      "defaultValue": "",
      "description": "Where to store container logs. An application's localized log directory\
      will be found in ${yarn.nodemanager.log-dirs}/application_${appid}.\
      Individual containers' log directories will be below this, in directories\
      named container_{$contid}. Each container directory will contain the files\
      stderr, stdin, and syslog generated by that container.",
      "defaultDirectory": "/hadoop/yarn/log",
      "displayType": "directories",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.local-dirs",
      "displayName": "yarn.nodemanager.local-dirs",
      "defaultValue": "",
      "description": "List of directories to store localized files in.\
      An application's localized file directory will be found in:\
      ${yarn.nodemanager.local-dirs}/usercache/${user}/appcache/application_${appid}.\
      Individual containers' work directories, called container_${contid}, will be subdirectories of this.",
      "defaultDirectory": "/hadoop/yarn/local",
      "displayType": "directories",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.remote-app-log-dir",
      "displayName": "yarn.nodemanager.remote-app-log-dir",
      "value": "",
      "defaultValue": "",
      "displayType": "directory",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.remote-app-log-dir-suffix",
      "displayName": "yarn.nodemanager.remote-app-log-dir-suffix",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.aux-services",
      "displayName": "yarn.nodemanager.aux-services",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.nodemanager.log.retain-second",
      "displayName": "yarn.nodemanager.log.retain-second",
      "value": "",
      "defaultValue": "",
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {
      "id": "site property",
      "name": "yarn.log.server.url",
      "displayName": "yarn.log.server.url",
      "value": "",
      "defaultValue": "",
      "category": "Advanced",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.leveldb-timeline-store.path",
      "displayName": "yarn.timeline-service.leveldb-timeline-store.path",
      "value": "",
      "defaultValue": "/var/log/hadoop-yarn/timeline",
      "isVisible": App.supports.appTimelineServer, // @todo remove after Application Timeline Server approving
      "category": "AppTimelineServer",
      "displayType": "directory",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.store-class",
      "displayName": "yarn.timeline-service.store-class",
      "value": "",
      "defaultValue": "org.apache.hadoop.yarn.server.applicationhistoryservice.timeline.LeveldbTimelineStore",
      "isVisible": App.supports.appTimelineServer, // @todo remove after Application Timeline Server approving
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.generic-application-history.store-class",
      "displayName": "yarn.timeline-service.generic-application-history.store-class",
      "value": "",
      "defaultValue": "org.apache.hadoop.yarn.server.applicationhistoryservice.NullApplicationHistoryStore",
      "isVisible": App.supports.appTimelineServer, // @todo remove after Application Timeline Server approving
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.webapp.address",
      "displayName": "yarn.timeline-service.webapp.address",
      "value": "",
      "defaultValue": "0.0.0.0:8188",
      "displayType": "string",
      "isVisible": App.supports.appTimelineServer, // @todo remove after Application Timeline Server approving
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.webapp.https.address",
      "displayName": "yarn.timeline-service.webapp.https.address",
      "value": "",
      "defaultValue": "0.0.0.0:8190",
      "displayType": "string",
      "isVisible": App.supports.appTimelineServer, // @todo remove after Application Timeline Server approving
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
    {
      "id": "site property",
      "name": "yarn.timeline-service.address",
      "displayName": "yarn.timeline-service.address",
      "value": "",
      "defaultValue": "0.0.0.0:10200",
      "displayType": "string",
      "isVisible": App.supports.appTimelineServer, // @todo remove after Application Timeline Server approving
      "category": "AppTimelineServer",
      "serviceName": "YARN"
    },
  /**********************************************MAPREDUCE2***************************************/
    {
      "id": "site property",
      "name": "mapreduce.map.memory.mb",
      "displayName": "Default virtual memory for a job's map-task",
      "description": "Virtual memory for single Map task",
      "value": "",
      "defaultValue": "",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "site property",
      "name": "mapreduce.reduce.memory.mb",
      "displayName": "Default virtual memory for a job's reduce-task",
      "description": "Virtual memory for single Reduce task",
      "value": "",
      "defaultValue": "",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2"
    },
    {
      "id": "site property",
      "name": "mapreduce.task.io.sort.mb",
      "displayName": "Map-side sort buffer memory",
      "description": "The total amount of buffer memory to use while sorting files, in megabytes.\
       By default, gives each merge stream 1MB, which should minimize seeks.",
      "defaultValue": "",
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
      "category": "Advanced",
      "value": "",
      "defaultValue": ""
    },
    {
      "id": "site property",
      "name": "yarn.app.mapreduce.am.resource.mb",
      "displayName": "yarn.app.mapreduce.am.resource.mb",
      "value": "",
      "defaultValue": "",
      "displayType": "int",
      "category": "Advanced",
      "serviceName": "MAPREDUCE2"
    },

  /**********************************************oozie-site***************************************/
    {
      "id": "site property",
      "name": "oozie.db.schema.name",
      "displayName": "Database Name",
      "description": "Database name used for the Oozie",
      "defaultValue": "",
      "isOverridable": false,
      "displayType": "host",
      "isVisible": true,
      "isObserved": true,
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "index": 3
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.username",
      "displayName": "Database Username",
      "description": "Database user name to use to connect to the database",
      "defaultValue": "",
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
      "description": "Database password to use to connect to the database",
      "defaultValue": "",
      "isOverridable": false,
      "displayType": "password",
      "isVisible": true,
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "filename": "oozie-site.xml",
      "index": 5
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.driver",
      "displayName": "JDBC Driver Class",
      "defaultValue": "",
      "value": "",     // the value is overwritten in code
      "isVisible": true,
      "isOverridable": false,
      "description": "Database name used for the Oozie",
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "index": 6
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.url",
      "displayName": "Database URL",
      "description": "The JDBC connection URL to the database",
      "defaultValue": "",
      "isOverridable": false,
      "displayType": "advanced",
      "category": "Oozie Server",
      "serviceName": "OOZIE",
      "index": 7
    },

  /**********************************************hive-site***************************************/
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionDriverName",
      "displayName": "JDBC Driver Class",
      "defaultValue": "",
      "value": "",     // the value is overwritten in code
      "isVisible": true,
      "isOverridable": false,
      "description": "Driver class name for a JDBC metastore",
      "category": "Hive Metastore",
      "serviceName": "HIVE",
      "index": 7
    },
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionUserName",
      "displayName": "Database Username",
      "description": "Database user name to use to connect to the database",
      "defaultValue": "hive",
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
      "description": "Database password to use to connect to the metastore database",
      "defaultValue": "",
      "displayType": "password",
      "isOverridable": false,
      "isVisible": true,
      "category": "Hive Metastore",
      "serviceName": "HIVE",
      "index": 6
    },
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionURL",
      "displayName": "Database URL",
      "value": "",
      "defaultValue": "", // set to a 'jdbc' to not include this in initial error count
      "description": "The JDBC connection URL to the database",
      "displayType": "advanced",
      "isOverridable": false,
      "isVisible": true,
      "category": "Hive Metastore",
      "serviceName": "HIVE",
      "index": 8
    },
    {
      "id": "site property",
      "name": "ambari.hive.db.schema.name",
      "displayName": "Database Name",
      "description": "Database name used as the Hive Metastore",
      "defaultValue": "",
      "isReconfigurable": true,
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
      "description": "Temporary directory on the local filesystem",
      "defaultDirectory": "/hadoop/hbase",
      "defaultValue": "",
      "displayType": "directory",
      "isVisible": true,
      "category": "Advanced",
      "serviceName": "HBASE"

    },
    {
      "id": "site property",
      "name": "hbase.regionserver.global.memstore.upperLimit",
      "displayName": "hbase.regionserver.global.memstore.upperLimit",
      "description": "",
      "defaultValue": "",
      "displayType": "float",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.regionserver.global.memstore.lowerLimit",
      "displayName": "hbase.regionserver.global.memstore.lowerLimit",
      "defaultValue": "",
      "description": "When memstores are being forced to flush to make room in\
      memory, keep flushing until we hit this mark. Defaults to 35% of heap.\
      This value equal to hbase.regionserver.global.memstore.upperLimit causes\
      the minimum possible flushing to occur when updates are blocked due to\
      memstore limiting.",
      "displayType": "float",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.hstore.blockingStoreFiles",
      "displayName": "hstore blocking storefiles",
      "description": "If more than this number of StoreFiles in any one Store (one StoreFile is written per flush of " +
        "MemStore) then updates are blocked for this HRegion until a compaction is completed, or until " +
        "hbase.hstore.blockingWaitTime has been exceeded.",
      "defaultValue": "",
      "isRequired": true,
      "displayType": "int",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.hstore.compactionThreshold",
      "displayName": "HBase HStore compaction threshold",
      "description": "If more than this number of HStoreFiles in any one HStore then a compaction is run to rewrite all HStoreFiles files as one.",
      "defaultValue": "3",
      "displayType": "int",
      "category": "General",
      "serviceName": "HBASE",
      "index": 0
    },
    {
      "id": "site property",
      "name": "hfile.block.cache.size",
      "displayName": "HFile block cache size ",
      "description": "Percentage of maximum heap (-Xmx setting) to allocate to block cache used by HFile/StoreFile. Set to 0 to disable but it's not recommended.",
      "defaultValue": "0.40",
      "displayType": "float",
      "category": "General",
      "serviceName": "HBASE",
      "index": 1
    },
    {
      "id": "site property",
      "name": "hbase.hregion.max.filesize",
      "displayName": "Maximum HStoreFile Size",
      "description": "If any one of a column families' HStoreFiles has grown to exceed this value, the hosting HRegion is split in two.",
      "defaultValue": "",
      "displayType": "int",
      "unit": "bytes",
      "isVisible": true,
      "category": "General",
      "serviceName": "HBASE",
      "index": 2
    },
    {
      "id": "site property",
      "name": "hbase.regionserver.handler.count",
      "displayName": "HBase RegionServer Handler",
      "description": "Count of RPC Listener instances spun up on RegionServers",
      "defaultValue": "60",
      "displayType": "int",
      "category": "RegionServer",
      "serviceName": "HBASE",
      "index": 2
    },
    {
      "id": "site property",
      "name": "hbase.hregion.majorcompaction",
      "displayName": "HBase Region Major Compaction",
      "description": "The time between major compactions of all HStoreFiles in a region. Set to 0 to disable automated major compactions.",
      "defaultValue": "",
      "displayType": "int",
      "unit": "ms",
      "isVisible": true,
      "category": "RegionServer",
      "serviceName": "HBASE",
      "index": 3
    },
    {
      "id": "site property",
      "name": "hbase.hregion.memstore.block.multiplier",
      "displayName": "HBase Region Block Multiplier",
      "description": "Block updates if memstore has \"Multiplier * HBase Region Memstore Flush Size\" bytes. Useful preventing runaway memstore during spikes in update traffic",
      "defaultValue": "",
      "displayType": "int",
      "category": "RegionServer",
      "serviceName": "HBASE",
      "index": 4
    },
    {
      "id": "site property",
      "name": "hbase.hregion.memstore.mslab.enabled",
      "displayName": "hbase.hregion.memstore.mslab.enabled",
      "description": "Enables the MemStore-Local Allocation Buffer,\
      a feature which works to prevent heap fragmentation under\
      heavy write loads. This can reduce the frequency of stop-the-world\
      GC pauses on large heaps.",
      "defaultValue": "",
      "displayType": "checkbox",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "hbase.hregion.memstore.flush.size",
      "displayName": "HBase Region Memstore Flush Size",
      "description": "Memstore will be flushed to disk if size of the memstore exceeds this number of bytes.",
      "defaultValue": "",
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
      "description": "Number of rows that will be fetched when calling next on a scanner if it is not served from \
      (local, client) memory. Do not set this value such that the time between invocations is greater than the scanner timeout",
      "defaultValue": "",
      "displayType": "int",
      "unit": "rows",
      "isVisible": true,
      "category": "General",
      "serviceName": "HBASE",
      "index": 3
    },
    {
      "id": "site property",
      "name": "zookeeper.session.timeout",
      "displayName": "Zookeeper timeout for HBase Session",
      "description": "HBase passes this to the zk quorum as suggested maximum time for a session",
      "defaultValue": "",
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
      "description": "Specifies the combined maximum allowed size of a KeyValue instance. It should be set to a fraction of the maximum region size.",
      "defaultValue": "",
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
      "defaultValue": "",
      "displayType": "multiLine",
      "serviceName": "HBASE",
      "category": "Advanced"
    },

  /**********************************************storm-site***************************************/
    {
      "id": "site property",
      "name": "storm.zookeeper.root",
      "displayName": "storm.zookeeper.root",
      "description": "",
      "defaultValue": "/storm",
      "isReconfigurable": true,
      "displayType": "directory",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.local.dir",
      "displayName": "storm.local.dir",
      "description": "",
      "defaultValue": "storm-local",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.servers",
      "displayName": "storm.zookeeper.servers",
      "description": "",
      "defaultValue": "",
      "displayType": "masterHosts",
      "isOverridable": false,
      "isReconfigurable": false,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.port",
      "displayName": "storm.zookeeper.port",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.session.timeout",
      "displayName": "storm.zookeeper.session.timeout",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.connection.timeout",
      "displayName": "storm.zookeeper.connection.timeout",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.retry.times",
      "displayName": "storm.zookeeper.retry.times",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.retry.interval",
      "displayName": "storm.zookeeper.retry.interval",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General",
      "unit": "ms"
    },
    {
      "id": "site property",
      "name": "storm.zookeeper.retry.intervalceiling.millis",
      "displayName": "storm.zookeeper.retry.intervalceiling.millis",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General",
      "unit": "ms"
    },
    {
      "id": "site property",
      "name": "storm.cluster.mode",
      "displayName": "storm.cluster.mode",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.local.mode.zmq",
      "displayName": "storm.local.mode.zmq",
      "description": "",
      "defaultValue": false,
      "isReconfigurable": true,
      "displayType": "checkbox",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.thrift.transport",
      "displayName": "storm.thrift.transport",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id": "site property",
      "name": "storm.messaging.transport",
      "displayName": "storm.messaging.transport",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"General",
      "displayName":"storm.messaging.netty.buffer_size",
      "name":"storm.messaging.netty.buffer_size",
      "defaultValue":"5242880",
      "displayType":"int",
      "unit": "bytes"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"General",
      "displayName":"storm.messaging.netty.max_retries",
      "name":"storm.messaging.netty.max_retries",
      "defaultValue":"30",
      "displayType":"int"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"General",
      "displayName":"storm.messaging.netty.max_wait_ms",
      "name":"storm.messaging.netty.max_wait_ms",
      "defaultValue":"1000",
      "displayType":"int",
      "unit": "ms"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"General",
      "displayName":"storm.messaging.netty.min_wait_ms",
      "name":"storm.messaging.netty.min_wait_ms",
      "defaultValue":"100",
      "displayType":"int",
      "unit": "ms"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.server_worker_threads",
      "name": "storm.messaging.netty.server_worker_threads",
      "defaultValue": "1",
      "displayType": "int"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.client_worker_threads",
      "name": "storm.messaging.netty.client_worker_threads",
      "defaultValue": "1",
      "displayType": "int"
    },
    {
      "id": "site property",
      "name": "nimbus.host",
      "displayName": "Nimbus Host",
      "description": "",
      "defaultValue": "",
      "displayType": "masterHost",
      "isOverridable": false,
      "isReconfigurable": false,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "nimbus.thrift.port",
      "displayName": "nimbus.thrift.port",
      "description": "",
      "defaultValue": "6627",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "nimbus.thrift.max_buffer_size",
      "displayName": "nimbus.thrift.max_buffer_size",
      "description": "",
      "defaultValue": "1048576",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "bytes"
    },
    {
      "id": "site property",
      "name": "nimbus.childopts",
      "displayName": "nimbus.childopts",
      "description": "This parameter is used by the storm-deploy project to configure the jvm options for the nimbus daemon.",
      "defaultValue": "-javaagent:/usr/lib/storm/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host={0},port=8649,wireformat31x=true,mode=multicast,config=/usr/lib/storm/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Nimbus_JVM",
      "isReconfigurable": true,
      "isOverridable": false,
      "isVisible": false,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "filename": "storm-site.xml"
    },
    {
      "id": "site property",
      "name": "nimbus.task.timeout.secs",
      "displayName": "nimbus.task.timeout.secs",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.supervisor.timeout.secs",
      "displayName": "nimbus.supervisor.timeout.secs",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.monitor.freq.secs",
      "displayName": "nimbus.monitor.freq.secs",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.cleanup.inbox.freq.secs",
      "displayName": "nimbus.cleanup.inbox.freq.secs",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.inbox.jar.expiration.secs",
      "displayName": "nimbus.inbox.jar.expiration.secs",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.task.launch.secs",
      "displayName": "nimbus.task.launch.secs",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.reassign",
      "displayName": "nimbus.reassign",
      "description": "",
      "defaultValue": true,
      "displayType": "checkbox",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "nimbus.file.copy.expiration.secs",
      "displayName": "nimbus.file.copy.expiration.secs",
      "description": "",
      "defaultValue": "",
      "displayType": "int",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "name": "nimbus.topology.validator",
      "displayName": "nimbus.topology.validator",
      "description": "",
      "defaultValue": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Nimbus"
    },
    {
      "id": "site property",
      "name": "supervisor.slots.ports",
      "displayName": "supervisor.slots.ports",
      "description": "",
      "defaultValue": "",
      "displayType": "string",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Supervisor"
    },
    {
      "id":"site property",
      "description":"This parameter is used by the storm-deploy project to configure the jvm options for the supervisor daemon.",
      "isReconfigurable": true,
      "isVisible":false,
      "isOverrideable": false,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"Supervisor",
      "displayName":"supervisor.childopts",
      "name":"supervisor.childopts",
      "defaultValue":"-javaagent:/usr/lib/storm/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host={0},port=8650,wireformat31x=true,mode=multicast,config=/usr/lib/storm/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Supervisor_JVM",
      "displayType":"string",
      "filename": "storm-site.xml"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"Supervisor",
      "displayName":"supervisor.worker.start.timeout.secs",
      "name":"supervisor.worker.start.timeout.secs",
      "defaultValue":"120",
      "displayType":"int",
      "unit": "seconds"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"Supervisor",
      "displayName":"supervisor.worker.timeout.secs",
      "name":"supervisor.worker.timeout.secs",
      "defaultValue":"30",
      "displayType":"int",
      "unit": "seconds"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"Supervisor",
      "displayName":"supervisor.monitor.frequency.secs",
      "name":"supervisor.monitor.frequency.secs",
      "defaultValue":"3",
      "displayType":"int",
      "unit": "seconds"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable": true,
      "isVisible":true,
      "isRequiredByAgent":true,
      "serviceName":"STORM",
      "category":"Supervisor",
      "displayName":"supervisor.heartbeat.frequency.secs",
      "name":"supervisor.heartbeat.frequency.secs",
      "defaultValue":"5",
      "displayType":"int",
      "unit": "seconds"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":true,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"STORM",
      "category":"Supervisor",
      "displayName":"supervisor.enable",
      "name":"supervisor.enable",
      "defaultValue":true,
      "displayType":"checkbox"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.port",
      "name": "drpc.port",
      "defaultValue": "3772",
      "displayType": "int"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.worker.threads",
      "name": "drpc.worker.threads",
      "defaultValue": "64",
      "displayType": "int"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.queue.size",
      "name": "drpc.queue.size",
      "defaultValue": "128",
      "displayType": "int"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.invocations.port",
      "name": "drpc.invocations.port",
      "defaultValue": "3773",
      "displayType": "int"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.request.timeout.secs",
      "name": "drpc.request.timeout.secs",
      "defaultValue": "600",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "DRPCServer",
      "displayName": "drpc.childopts",
      "name": "drpc.childopts",
      "defaultValue": "-Xmx768m",
      "displayType": "string"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "StormUIServer",
      "displayName": "ui.port",
      "name": "ui.port",
      "defaultValue": "8744",
      "displayType": "int"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "StormUIServer",
      "displayName": "ui.childopts",
      "name": "ui.childopts",
      "defaultValue": "-Xmx768m",
      "displayType": "string"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.port",
      "name": "logviewer.port",
      "defaultValue": "8000",
      "displayType": "int"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.childopts",
      "name": "logviewer.childopts",
      "defaultValue": "-Xmx128m",
      "displayType": "string"
    },
    {
      "id": "site property",
      "description": "",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.appender.name",
      "name": "logviewer.appender.name",
      "defaultValue": "A1",
      "displayType": "string"
    },
    {
      "id": "site property",
      "description": "The jvm opts provided to workers launched by this supervisor. All \"%ID%\" substrings are replaced with an identifier for this worker.",
      "isReconfigurable": true,
      "isVisible": false,
      "isOverridable": false,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "category": "Advanced",
      "displayName": "worker.childopts",
      "name": "worker.childopts",
      "defaultValue": "-javaagent:/usr/lib/storm/contrib/storm-jmxetric/lib/jmxetric-1.0.4.jar=host={0},port=8650,wireformat31x=true,mode=multicast,config=/usr/lib/storm/contrib/storm-jmxetric/conf/jmxetric-conf.xml,process=Worker_%ID%_JVM",
      "displayType": "string",
      "filename": "storm-site.xml"
    },
  /*********************************************oozie-site for Falcon*****************************/
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":true,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"Falcon proxyuser hosts",
      "name":"oozie.service.ProxyUserService.proxyuser.falcon.hosts",
      "defaultValue":"*",
      "displayType":"advanced"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":true,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"Falcon proxyuser groups",
      "name":"oozie.service.ProxyUserService.proxyuser.falcon.groups",
      "defaultValue":"*",
      "displayType":"advanced"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":false,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"oozie.service.ELService.ext.functions.coord-job-submit-instances",
      "name":"oozie.service.ELService.ext.functions.coord-job-submit-instances",
      "defaultValue":"now=org.apache.oozie.extensions.OozieELExtensions#ph1_now_echo,"
        + "today=org.apache.oozie.extensions.OozieELExtensions#ph1_today_echo,"
        + "yesterday=org.apache.oozie.extensions.OozieELExtensions#ph1_yesterday_echo,"
        + "currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_currentMonth_echo,"
        + "lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_lastMonth_echo,"
        + "currentYear=org.apache.oozie.extensions.OozieELExtensions#ph1_currentYear_echo,"
        + "lastYear=org.apache.oozie.extensions.OozieELExtensions#ph1_lastYear_echo,"
        + "formatTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_formatTime_echo,"
        + "latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo,"
        + "future=org.apache.oozie.coord.CoordELFunctions#ph2_coord_future_echo",
      "displayType":"custom"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":false,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"oozie.service.ELService.ext.functions.coord-action-create-inst",
      "name":"oozie.service.ELService.ext.functions.coord-action-create-inst",
      "defaultValue":"now=org.apache.oozie.extensions.OozieELExtensions#ph2_now_inst,"
        + "today=org.apache.oozie.extensions.OozieELExtensions#ph2_today_inst,"
        + "yesterday=org.apache.oozie.extensions.OozieELExtensions#ph2_yesterday_inst,"
        + "currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_currentMonth_inst,"
        + "lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_lastMonth_inst,"
        + "currentYear=org.apache.oozie.extensions.OozieELExtensions#ph2_currentYear_inst,"
        + "lastYear=org.apache.oozie.extensions.OozieELExtensions#ph2_lastYear_inst,"
        + "latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo,"
        + "future=org.apache.oozie.coord.CoordELFunctions#ph2_coord_future_echo,"
        + "formatTime=org.apache.oozie.coord.CoordELFunctions#ph2_coord_formatTime,"
        + "user=org.apache.oozie.coord.CoordELFunctions#coord_user",
      "displayType":"custom"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":false,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"oozie.service.ELService.ext.functions.coord-action-create",
      "name": "oozie.service.ELService.ext.functions.coord-action-create",
      "defaultValue":"now=org.apache.oozie.extensions.OozieELExtensions#ph2_now,"
        + "today=org.apache.oozie.extensions.OozieELExtensions#ph2_today,"
        + "yesterday=org.apache.oozie.extensions.OozieELExtensions#ph2_yesterday,"
        + "currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_currentMonth,"
        + "lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_lastMonth,"
        + "currentYear=org.apache.oozie.extensions.OozieELExtensions#ph2_currentYear,"
        + "lastYear=org.apache.oozie.extensions.OozieELExtensions#ph2_lastYear,"
        + "latest=org.apache.oozie.coord.CoordELFunctions#ph2_coord_latest_echo,"
        + "future=org.apache.oozie.coord.CoordELFunctions#ph2_coord_future_echo,"
        + "formatTime=org.apache.oozie.coord.CoordELFunctions#ph2_coord_formatTime,"
        + "user=org.apache.oozie.coord.CoordELFunctions#coord_user,",
      "displayType":"custom"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":false,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"oozie.service.ELService.ext.functions.coord-job-submit-data",
      "name":"oozie.service.ELService.ext.functions.coord-job-submit-data",
      "defaultValue":"now=org.apache.oozie.extensions.OozieELExtensions#ph1_now_echo,"
        + "today=org.apache.oozie.extensions.OozieELExtensions#ph1_today_echo,"
        + "yesterday=org.apache.oozie.extensions.OozieELExtensions#ph1_yesterday_echo,"
        + "currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_currentMonth_echo,"
        + "lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph1_lastMonth_echo,"
        + "currentYear=org.apache.oozie.extensions.OozieELExtensions#ph1_currentYear_echo,"
        + "lastYear=org.apache.oozie.extensions.OozieELExtensions#ph1_lastYear_echo,"
        + "dataIn=org.apache.oozie.extensions.OozieELExtensions#ph1_dataIn_echo,"
        + "instanceTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_nominalTime_echo_wrap,"
        + "formatTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_formatTime_echo,"
        + "dateOffset=org.apache.oozie.coord.CoordELFunctions#ph1_coord_dateOffset_echo,"
        + "user=org.apache.oozie.coord.CoordELFunctions#coord_user",
      "displayType":"custom"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":false,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"oozie.service.ELService.ext.functions.coord-action-start",
      "name":"oozie.service.ELService.ext.functions.coord-action-start",
      "defaultValue":"now=org.apache.oozie.extensions.OozieELExtensions#ph2_now,"
        + "today=org.apache.oozie.extensions.OozieELExtensions#ph2_today,"
        + "yesterday=org.apache.oozie.extensions.OozieELExtensions#ph2_yesterday,"
        + "currentMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_currentMonth,"
        + "lastMonth=org.apache.oozie.extensions.OozieELExtensions#ph2_lastMonth,"
        + "currentYear=org.apache.oozie.extensions.OozieELExtensions#ph2_currentYear,"
        + "lastYear=org.apache.oozie.extensions.OozieELExtensions#ph2_lastYear,"
        + "latest=org.apache.oozie.coord.CoordELFunctions#ph3_coord_latest,"
        + "future=org.apache.oozie.coord.CoordELFunctions#ph3_coord_future,"
        + "dataIn=org.apache.oozie.extensions.OozieELExtensions#ph3_dataIn,"
        + "instanceTime=org.apache.oozie.coord.CoordELFunctions#ph3_coord_nominalTime,"
        + "dateOffset=org.apache.oozie.coord.CoordELFunctions#ph3_coord_dateOffset,"
        + "formatTime=org.apache.oozie.coord.CoordELFunctions#ph3_coord_formatTime,"
        + "user=org.apache.oozie.coord.CoordELFunctions#coord_user",
      "displayType":"custom"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":false,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"oozie.service.ELService.ext.functions.coord-sla-submit",
      "name":"oozie.service.ELService.ext.functions.coord-sla-submit",
      "defaultValue":"instanceTime=org.apache.oozie.coord.CoordELFunctions#ph1_coord_nominalTime_echo_fixed,"
        + "user=org.apache.oozie.coord.CoordELFunctions#coord_user",
      "displayType":"custom"
    },
    {
      "id":"site property",
      "description":"",
      "isReconfigurable":false,
      "isVisible":true,
      "isRequiredByAgent": true,
      "serviceName":"FALCON",
      "category":"Falcon - Oozie integration",
      "displayName":"oozie.service.ELService.ext.functions.coord-sla-create",
      "name":"oozie.service.ELService.ext.functions.coord-sla-create",
      "defaultValue":"instanceTime=org.apache.oozie.coord.CoordELFunctions#ph2_coord_nominalTime,"
        + "user=org.apache.oozie.coord.CoordELFunctions#coord_user",
      "displayType":"custom"
    },
  /**********************************************webhcat-site***************************************/
    {
      "id": "site property",
      "name": "templeton.zookeeper.hosts",
      "displayName": "templeton.zookeeper.hosts",
      "defaultValue": "",
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
     "serviceName": "MAPREDUCE2",
     "filename": "mapreduce2-log4j.xml",
     "category": "AdvancedMapredLog4j"
     },
     {
     "id": "site property",
     "name": "content",
     "displayName": "content",
     "value": "",
     "defaultValue": "",
     "description": "log4j properties",
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
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
     "displayType": "custom",
     "isOverridable": true,
     "isRequired": false,
     "isVisible": true,
     "serviceName": "PIG",
     "filename": "pig-log4j.xml",
     "category": "AdvancedPigLog4j"
     }
  ]
};
