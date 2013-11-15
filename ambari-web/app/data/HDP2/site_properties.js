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
      "name": "fs.checkpoint.size",
      "displayName": "HDFS Maximum Edit Log Size for Checkpointing",
      "description": "Maximum size of the edits log file that forces an urgent checkpoint even if the maximum checkpoint delay is not reached",
      "defaultValue": "",
      "displayType": "int",
      "unit": "bytes",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "General",
      "index": 4
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
      "isVisible": false,
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
      "index": 5
    },
    {
      "id": "site property",
      "name": "oozie.service.JPAService.jdbc.driver",
      "displayName": "JDBC driver class",
      "defaultValue": "",
      "value": "",     // the value is overwritten in code
      "isVisible": false,
      "description": "Database name used for the Oozie",
      "category": "Advanced",
      "serviceName": "OOZIE"
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
      "index": 6
    },

  /**********************************************hive-site***************************************/
    {
      "id": "site property",
      "name": "javax.jdo.option.ConnectionDriverName",
      "displayName": "JDBC driver class",
      "defaultValue": "",
      "value": "",     // the value is overwritten in code
      "isVisible": false,
      "description": "Driver class name for a JDBC metastore",
      "category": "Advanced",
      "serviceName": "HIVE"
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
      "index": 7
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
    }
  ]
};
