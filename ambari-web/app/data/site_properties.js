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
      "name": "fs.checkpoint.dir",
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
      "name": "fs.checkpoint.period",
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
      "name": "dfs.name.dir",
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
      "defaultValue": "",
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
      "description": "The number of volumes that are allowed to fail before a DataNode stops offering service",
      "defaultValue": "",
      "displayType": "int",
      "isVisible": true,
      "category": "DataNode",
      "serviceName": "HDFS",
      "index": 3
    },
    {
      "id": "site property",
      "name": "dfs.data.dir",
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
      "defaultValue": "",
      "displayType": "int",
      "unit": "bytes",
      "isVisible": true,
      "category": "General",
      "serviceName": "HDFS",
      "index": 2
    },

  /******************************************MAPREDUCE***************************************/
    {
      "id": "site property",
      "name": "mapred.local.dir",
      "displayName": "MapReduce local directories",
      "description": "Directories for MapReduce to store intermediate data files",
      "defaultValue": "",
      "defaultDirectory": "/hadoop/mapred",
      "displayType": "directories",
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker",
      "index": 1
    },
    {
      "id": "site property",
      "name": "mapred.jobtracker.taskScheduler",
      "displayName": "MapReduce Capacity Scheduler",
      "description": "The scheduler to use for scheduling of MapReduce jobs",
      "defaultValue": "",
      "displayType": "advanced",
      "isOverridable": false,
      "serviceName": "MAPREDUCE",
      "index": 0
    },
    {
      "id": "site property",
      "name": "mapred.tasktracker.map.tasks.maximum",
      "displayName": "Number of Map slots per node",
      "description": "Number of slots that Map tasks that run simultaneously can occupy on a TaskTracker",
      "defaultValue": "",
      "displayType": "int",
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker",
      "index": 2
    },
    {
      "id": "site property",
      "name": "mapred.tasktracker.reduce.tasks.maximum",
      "displayName": "Number of Reduce slots per node",
      "description": "Number of slots that Reduce tasks that run simultaneously can occupy on a TaskTracker.",
      "defaultValue": "",
      "displayType": "int",
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker",
      "index": 3
    },
    {
      "id": "site property",
      "name": "mapred.cluster.reduce.memory.mb",
      "displayName": "Cluster's Reduce slot size (virtual memory)",
      "description": "The virtual memory size of a single Reduce slot in the MapReduce framework",
      "defaultValue": "2048",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "index": 2
    },
    {
      "id": "site property",
      "name": "mapred.job.map.memory.mb",
      "displayName": "Default virtual memory for a job's map-task",
      "description": "Virtual memory for single Map task",
      "defaultValue": "",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "index": 5
    },
    {
      "id": "site property",
      "name": "mapred.cluster.max.map.memory.mb",
      "displayName": "Upper limit on virtual memory for single Map task",
      "description": "Upper limit on virtual memory size for a single Map task of any MapReduce job",
      "defaultValue": "",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "index": 3
    },
    {
      "id": "site property",
      "name": "mapred.cluster.max.reduce.memory.mb",
      "displayName": "Upper limit on virtual memory for single Reduce task",
      "description": "Upper limit on virtual memory size for a single Reduce task of any MapReduce job",
      "defaultValue": "",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "index": 4
    },
    {
      "id": "site property",
      "name": "mapred.job.reduce.memory.mb",
      "displayName": "Default virtual memory for a job's reduce-task",
      "description": "Virtual memory for single Reduce task",
      "defaultValue": "",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "index": 6
    },
    {
      "id": "site property",
      "name": "mapred.cluster.map.memory.mb",
      "displayName": "Cluster's Map slot size (virtual memory)",
      "description": "The virtual memory size of a single Map slot in the MapReduce framework",
      "defaultValue": "1536",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "index": 1
    },
    {
      "id": "site property",
      "name": "mapred.healthChecker.script.path",
      "displayName": "Job Status directory",
      "description": "Directory path to view job status",
      "defaultValue": "",
      "displayType": "advanced",
      "serviceName": "MAPREDUCE",
      "category": "Advanced"
    },
    {
      "id": "site property",
      "name": "io.sort.mb",
      "displayName": "Map-side sort buffer memory",
      "description": "The total amount of Map-side buffer memory to use while sorting files (Expert-only configuration)",
      "defaultValue": "",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "index": 7
    },
    {
      "id": "site property",
      "name": "io.sort.spill.percent",
      "displayName": "Limit on buffer",
      "description": "Percentage of sort buffer used for record collection",
      "defaultValue": "",
      "displayType": "float",
      "serviceName": "MAPREDUCE",
      "index": 8
    },
    {
      "id": "site property",
      "name": "mapred.system.dir",
      "displayName": "MapReduce system directories",
      "description": "Path on the HDFS where where the MapReduce framework stores system files",
      "defaultValue": "/mapred/system",
      "displayType": "directories",
      "serviceName": "MAPREDUCE",
      "category": "Advanced"
    },
    {
      "id": "site property",
      "name": "mapred.userlog.retain.hours",
      "displayName": "Job log retention (hours)",
      "description": "The maximum time, in hours, for which the user-logs are to be retained after the job completion.",
      "defaultValue": "",
      "displayType": "int",
      "unit": "hours",
      "serviceName": "MAPREDUCE",
      "index": 9
    },
    {
      "id": "site property",
      "name": "mapred.jobtracker.maxtasks.per.job",
      "displayName": "Maximum number tasks for a Job",
      "description": "Maximum number of tasks for a single Job",
      "defaultValue": "",
      "displayType": "int",
      "serviceName": "MAPREDUCE",
      "index": 10
    },
    {
      "id": "site property",
      "name": "mapred.hosts",
      "displayName": "mapred.hosts",
      "description": "Names a file that contains the list of nodes that may\
      connect to the jobtracker.  If the value is empty, all hosts are \
      permitted.",
      "defaultValue": "",
      "displayType": "directory",
      "category": "Advanced",
      "serviceName": "MAPREDUCE"
    },
    {
      "id": "site property",
      "name": "mapred.hosts.exclude",
      "displayName": "mapred.hosts.exclude",
      "description": " Names a file that contains the list of hosts that\
      should be excluded by the jobtracker.  If the value is empty, no\
      hosts are excluded.",
      "defaultValue": "",
      "displayType": "directory",
      "category": "Advanced",
      "serviceName": "MAPREDUCE"
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
    },
    {
      "id": "site property",
      "name": "dfs.client.read.shortcircuit",
      "displayName": "HDFS Short-circuit read",
      "description": "This configuration parameter turns on short-circuit local reads.",
      "defaultValue": "",
      "displayType": "checkbox",
      "category": "Advanced",
      "serviceName": "HBASE"
    },
    {
      "id": "site property",
      "name": "dfs.support.append",
      "displayName": "HDFS append support",
      "description": "HDFS append support",
      "defaultValue": "",
      "displayType": "checkbox",
      "serviceName": "HBASE",
      "category": "Advanced"
    }
  ]
};
