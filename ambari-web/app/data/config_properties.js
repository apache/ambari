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
/**
 * Defines service configuration properties.
 *   name:
 *     The name of the config property that is understood by Ambari server and agent.
 *     E.g., "datanode_du_reserved"
 *
 *   displayName:
 *     The human-friendly display name of the config property.
 *     E.g., "Reserved space for HDFS"
 *
 *   description:
 *     The description of the config property.
 *     E.g., "Reserved space in GB per volume"
 *
 *   defaultValue:
 *     The default value of the config property.
 *     E.g., "1"
 *
 *   isReconfigurable:
 *     Whether the config property can be reconfigured after it has been initially set and deployed.
 *     If this is unspecified, true is assumed.
 *     E.g., true, false
 *
 *   isRequired:
 *     Whether the config property is required or not.
 *     If this is unspecified, true is assumed.
 *     E.g., true, false
 *
 *   displayType:
 *     How the config property is to be rendered for user input.
 *     If this is left unspecified, "string" is assumed
 *     E.g., "string", "int", "float", "checkbox", "directories", "custom", "email", "masterHost", "slaveHosts"
 *
 *   unit
 *     The unit for the config property.
 *     E.g., "ms", "MB", "bytes"
 *
 *   serviceName:
 *     The service that the config property belongs to.
 *     E.g., "HDFS", "MAPREDUCE", "ZOOKEEPER", etc.
 *
 *   category: the category that the config property belongs to (used for grouping config properties in the UI).
 *     if unspecified, "General" is assumed.
 *     E.g., "General", "Advanced", "NameNode", "DataNode"
 */

module.exports =
{
  "configProperties": [
    {
      "name": "hbasemaster.host",
      "displayName": "HBase Master host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run HBase Master",
      "displayType": "masterHost",
      "serviceName": "HBASE",
      "category": "HBase Master"
    },
    {
      "name": "regionserver.hosts",
      "displayName": "RegionServer hosts",
      "value": "",
      "defaultValue": "",
      "description": "The hosts that have been assigned to run RegionServer",
      "displayType": "slaveHosts",
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {
      "name": "hbase_log_dir",
      "displayName": "HBase Log Dir",
      "description": "Directory for HBase logs",
      "defaultValue": "/var/log/hbase",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HBASE",
      "category": "Advanced"
    },
    {
      "name": "hbase_pid_dir",
      "displayName": "HBase PID Dir",
      "description": "Directory in which the pid files for HBase processes will be created",
      "defaultValue": "/var/run/hbase",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HBASE",
      "category": "Advanced"
    },
    {
      "name": "hbase_regionserver_heapsize",
      "displayName": "HBase Region Servers maximum Java heap size",
      "description": "Maximum Java heap size for HBase Region Servers (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {
      "name": "hbase_master_heapsize",
      "displayName": "HBase Master Maximum Java heap size",
      "description": "Maximum Java heap size for HBase master (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "HBASE",
      "category": "HBase Master"
    },
    {
      "name": "hstore_compactionthreshold",
      "displayName": "HBase HStore compaction threshold",
      "description": "If more than this number of HStoreFiles in any one HStore then a compaction is run to rewrite all HStoreFiles files as one.",
      "defaultValue": "3",
      "displayType": "int",
      "serviceName": "HBASE"
    },
    {
      "name": "hfile_blockcache_size",
      "displayName": "HFile block cache size ",
      "description": "Percentage of maximum heap (-Xmx setting) to allocate to block cache used by HFile/StoreFile. Set to 0 to disable but it's not recommended.",
      "defaultValue": "0.25",
      "displayType": "float",
      "serviceName": "HBASE"
    },
    {
      "name": "hstorefile_maxsize",
      "displayName": "Maximum HStoreFile Size",
      "description": "If any one of a column families' HStoreFiles has grown to exceed this value, the hosting HRegion is split in two.",
      "defaultValue": "1073741824",
      "displayType": "int",
      "unit": "bytes",
      "serviceName": "HBASE"
    },
    {
      "name": "regionserver_handlers",
      "displayName": "HBase Region Server Handler",
      "description": "Count of RPC Listener instances spun up on RegionServers",
      "defaultValue": "30",
      "displayType": "int",
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {
      "name": "hregion_majorcompaction",
      "displayName": "HBase Region Major Compaction",
      "description": "The time between major compactions of all HStoreFiles in a region. Set to 0 to disable automated major compactions.",
      "defaultValue": "86400000",
      "displayType": "int",
      "unit": "ms",
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {
      "name": "hregion_blockmultiplier",
      "displayName": "HBase Region Block Multiplier",
      "description": "Block updates if memstore has \"Multiplier * HBase Region Memstore Flush Size\" bytes. Useful preventing runaway memstore during spikes in update traffic",
      "defaultValue": "2",
      "displayType": "int",
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {
      "name": "hregion_memstoreflushsize",
      "displayName": "HBase Region Memstore Flush Size",
      "description": "Memstore will be flushed to disk if size of the memstore exceeds this number of bytes.",
      "defaultValue": "134217728",
      "displayType": "int",
      "unit": "bytes",
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {
      "name": "client_scannercaching",
      "displayName": "HBase Client Scanner Caching",
      "description": "Number of rows that will be fetched when calling next on a scanner if it is not served from (local, client) memory. Do not set this value such that the time between invocations is greater than the scanner timeout",
      "defaultValue": "100",
      "displayType": "int",
      "unit": "rows",
      "serviceName": "HBASE"
    },
    {
      "name": "zookeeper_sessiontimeout",
      "displayName": "Zookeeper timeout for HBase Session",
      "description": "HBase passes this to the zk quorum as suggested maximum time for a session",
      "defaultValue": "60000",
      "displayType": "int",
      "unit": "ms",
      "serviceName": "HBASE"
    },
    {
      "name": "hfile_max_keyvalue_size",
      "displayName": "HBase Client Maximum key-value Size",
      "description": "Specifies the combined maximum allowed size of a KeyValue instance. It should be set to a fraction of the maximum region size.",
      "defaultValue": "10485760",
      "displayType": "int",
      "unit": "bytes",
      "serviceName": "HBASE"
    },
    {
      "name": "hbase-site.xml",
      "displayName": "Custom HBase Configs",
      "description": "If you wish to set configuration parameters not exposed through this page, you can specify them here.<br>The text you specify here will be injected into hbase-site.xml verbatim.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "serviceName": "HBASE",
      "category": "Advanced"
    },
    {
      "name": "namenode.host",
      "displayName": "NameNode host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run NameNode",
      "displayType": "masterHost",
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "name": "dfs_name_dir",
      "displayName": "NameNode directories",
      "description": "NameNode directories for HDFS to store the file system image",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "directories",
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "name": "snamenode.host",
      "displayName": "SNameNode host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run SecondaryNameNode",
      "displayType": "masterHost",
      "serviceName": "HDFS",
      "category": "SNameNode"
    },
    {
      "name": "fs_checkpoint_dir",
      "displayName": "SecondaryNameNode Checkpoint directory",
      "description": "Directory on the local filesystem where the Secondary NameNode should store the temporary images to merge",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HDFS",
      "category": "SNameNode"
    },
    {
      "name": "datanode.hosts",
      "displayName": "DataNode hosts",
      "value": "",
      "defaultValue": "",
      "description": "The hosts that have been assigned to run DataNode",
      "displayType": "slaveHosts",
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {
      "name": "dfs_data_dir",
      "displayName": "DataNode directories",
      "description": "DataNode directories for HDFS to store the data blocks",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "directories",
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {
      "name": "hdfs_log_dir_prefix",
      "displayName": "HDFS Log Dir Prefix",
      "description": "The parent directory for HDFS log files.  The actual directory will be ${hdfs_log_dir_prefix}/${hdfs_user}",
      "defaultValue": "/var/log/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "name": "hdfs_pid_dir_prefix",
      "displayName": "HDFS PID Dir Prefix",
      "description": "The parent directory in which the PID files for HDFS processes will be created.  The actual directory will be ${hdfs_log_dir_prefix}/${hdfs_user}",
      "defaultValue": "/var/run/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "name": "dfs_support_append",
      "displayName": "Append enabled",
      "description": "Whether to enable HDFS Append feature",
      "defaultValue": true,
      "displayType": "checkbox",
      "serviceName": "HDFS"
    },
    {
      "name": "dfs_webhdfs_enabled",
      "displayName": "WebHDFS enabled",
      "description": "Whether to enable WebHDFS feature",
      "defaultValue": false,
      "displayType": "checkbox",
      "serviceName": "HDFS"
    },
    {
      "name": "hadoop_heapsize",
      "displayName": "Hadoop maximum Java heap size",
      "description": "Maximum Java heap size for daemons such as Balancer (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "HDFS"
    },
    {
      "name": "namenode_heapsize",
      "displayName": "NameNode Java heap size",
      "description": "Initial and maximum Java heap size for NameNode (Java options -Xms and -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "name": "namenode_opt_newsize",
      "displayName": "NameNode new generation size",
      "description": "Default size of Java new generation for NameNode (Java option -XX:NewSize)",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "name": "datanode_du_reserved",
      "displayName": "Reserved space for HDFS",
      "description": "Reserved space in GB per volume",
      "defaultValue": "1",
      "displayType": "int",
      "unit": "GB",
      "serviceName": "HDFS"
    },
    {
      "name": "dtnode_heapsize",
      "displayName": "DataNode maximum Java heap size",
      "description": "Maximum Java heap size for DataNode (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {
      "name": "dfs_datanode_failed_volume_tolerated",
      "displayName": "DataNode volumes failure toleration",
      "description": "The number of volumes that are allowed to fail before a DataNode stops offering service",
      "defaultValue": "0",
      "displayType": "int",
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {
      "name": "fs_checkpoint_period",
      "displayName": "HDFS Maximum Checkpoint Delay",
      "description": "Maximum delay between two consecutive checkpoints for HDFS",
      "defaultValue": "21600",
      "isReconfigurable": false,
      "displayType": "int",
      "unit": "seconds",
      "serviceName": "HDFS"
    },
    {
      "name": "fs_checkpoint_size",
      "displayName": "HDFS Maximum Edit Log Size for Checkpointing",
      "description": "Maximum size of the edits log file that forces an urgent checkpoint even if the maximum checkpoint delay is not reached",
      "defaultValue": "0.5",
      "isReconfigurable": false,
      "displayType": "float",
      "unit": "GB",
      "serviceName": "HDFS"
    },
    {
      "name": "hdfs-site.xml",
      "displayName": "Custom HDFS Configs",
      "description": "If you wish to set configuration parameters not exposed through this page, you can specify them here.<br>The text you specify here will be injected into hdfs-site.xml verbatim.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "name": "hivemetastore.host",
      "displayName": "Hive Metastore host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run Hive Metastore",
      "displayType": "masterHost",
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "name": "hive_mysql_host",
      "displayName": "MySQL host",
      "description": "MySQL host on which the Hive Metastore is hosted. If left empty, the metastore will be set up on the same host as the Hive Server using the database name and user credentials specified",
      "defaultValue": "",
      "isRequired": false,
      "isReconfigurable": false,
      "displayType": "host",
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "name": "hive_database_name",
      "displayName": "MySQL database name",
      "description": "MySQL database name used as the Hive Metastore",
      "defaultValue": "hive",
      "isReconfigurable": false,
      "displayType": "host",
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "name": "hive_metastore_user_name",
      "displayName": "MySQL user",
      "description": "MySQL user to use to connect to the MySQL database",
      "defaultValue": "hive",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "name": "hive_metastore_user_passwd",
      "displayName": "MySQL password",
      "description": "MySQL password to use to connect to the MySQL database",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "password",
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "name": "hive_log_dir",
      "displayName": "Hive Log Dir",
      "description": "Directory for Hive log files",
      "defaultValue": "/var/log/hive",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "name": "hive_pid_dir",
      "displayName": "Hive PID Dir",
      "description": "Directory in which the PID files for Hive processes will be created",
      "defaultValue": "/var/run/hive",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "name": "hcat_log_dir",
      "displayName": "HCat Log Dir",
      "description": "Directory for HCatalog log files",
      "defaultValue": "/var/log/hcatalog",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "name": "hcat_pid_dir",
      "displayName": "HCat PID Dir",
      "description": "Directory in which the PID files for HCatalog processes will be created",
      "defaultValue": "/var/run/hcatalog",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "name": "hive-site.xml",
      "displayName": "Custom Hive Configs",
      "description": "If you wish to set configuration parameters not exposed through this page, you can specify them here.<br>The text you specify here will be injected into hive-site.xml verbatim.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "name": "jobtracker.host",
      "displayName": "JobTracker host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run JobTracker",
      "displayType": "masterHost",
      "serviceName": "MAPREDUCE",
      "category": "JobTracker"
    },
    {
      "name": "tasktracker.hosts",
      "displayName": "TaskTracer hosts",
      "value": "",
      "defaultValue": "",
      "description": "The hosts that have been assigned to run TaskTracker",
      "displayType": "slaveHosts",
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker"
    },
    {
      "name": "mapred_local_dir",
      "displayName": "MapReduce local directories",
      "description": "Directories for MapReduce to store intermediate data files",
      "defaultValue": "",
      "displayType": "directories",
      "isReconfigurable": false,
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred_log_dir_prefix",
      "displayName": "MapReduce Log Dir Prefix",
      "description": "The parent directory for MapReduce log files.  The actual directory will be ${mapred_log_dir_prefix}/${mapred_user}",
      "defaultValue": "/var/log/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "MAPREDUCE",
      "category": "Advanced"
    },
    {
      "name": "mapred_pid_dir_prefix",
      "displayName": "MapReduce PID Dir Prefix",
      "description": "The parent directory in which the PID files for MapReduce processes will be created.  The actual directory will be ${mapred_log_dir_prefix}/${mapred_user}",
      "defaultValue": "/var/run/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "MAPREDUCE",
      "category": "Advanced"
    },
    {
      "name": "scheduler_name",
      "displayName": "MapReduce Capacity Scheduler",
      "description": "The scheduler to use for scheduling of MapReduce jobs",
      "defaultValue": "org.apache.hadoop.mapred.CapacityTaskScheduler",
      "displayType": "directory",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "jtnode_opt_newsize",
      "displayName": "JobTracker new generation size",
      "description": "Default size of Java new generation size for JobTracker in MB (Java option -XX:NewSize)",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "category": "JobTracker"
    },
    {
      "name": "jtnode_opt_maxnewsize",
      "displayName": "JobTracker maximum new generation size",
      "description": "Maximum size of Java new generation for JobTracker in MB (Java option -XX:MaxNewSize)",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "category": "JobTracker"
    },
    {
      "name": "jtnode_heapsize",
      "displayName": "JobTracker maximum Java heap size",
      "description": "Maximum Java heap size for JobTracker in MB (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE",
      "category": "JobTracker"
    },
    {
      "name": "mapred_map_tasks_max",
      "displayName": "Number of Map slots per node",
      "description": "Number of slots that Map tasks that run simultaneously can occupy on a TaskTracker",
      "defaultValue": "4",
      "displayType": "int",
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker"
    },
    {
      "name": "mapred_red_tasks_max",
      "displayName": "Number of Reduce slots per node",
      "description": "Number of slots that Reduce tasks that run simultaneously can occupy on a TaskTracker.",
      "defaultValue": "2",
      "displayType": "int",
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker"
    },
    {
      "name": "mapred_cluster_map_mem_mb",
      "displayName": "Cluster's Map slot size (virtual memory)",
      "description": "The virtual memory size of a single Map slot in the MapReduce framework",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred_cluster_red_mem_mb",
      "displayName": "Cluster's Reduce slot size (virtual memory)",
      "description": "The virtual memory size of a single Reduce slot in the MapReduce framework",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred_cluster_max_map_mem_mb",
      "displayName": "Upper limit on virtual memory for single Map task",
      "description": "Upper limit on virtual memory size for a single Map task of any MapReduce job",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred_cluster_max_red_mem_mb",
      "displayName": "Upper limit on virtual memory for single Reduce task",
      "description": "Upper limit on virtual memory size for a single Reduce task of any MapReduce job",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred_job_map_mem_mb",
      "displayName": "Default virtual memory for a job's map-task",
      "description": "Virtual memory for single Map task",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred_job_red_mem_mb",
      "displayName": "Default virtual memory for a job's reduce-task",
      "description": "Virtual memory for single Reduce task",
      "defaultValue": "-1",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred_child_java_opts_sz",
      "displayName": "Java options for MapReduce tasks",
      "description": "Java options for the TaskTracker child processes.",
      "defaultValue": "768",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "io_sort_mb",
      "displayName": "Map-side sort buffer memory",
      "description": "The total amount of Map-side buffer memory to use while sorting files (Expert-only configuration)",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "io_sort_spill_percent",
      "displayName": "Limit on buffer",
      "description": "Percentage of sort buffer used for record collection (Expert-only configuration)",
      "defaultValue": "0.9",
      "displayType": "float",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapreduce_userlog_retainhours",
      "displayName": "Job log retention (hours)",
      "description": "The maximum time, in hours, for which the user-logs are to be retained after the job completion.",
      "defaultValue": "24",
      "displayType": "int",
      "unit": "hours",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "maxtasks_per_job",
      "displayName": "Maximum number tasks for a Job",
      "description": "Maximum number of tasks for a single Job",
      "defaultValue": "-1",
      "displayType": "int",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "lzo_enabled",
      "displayName": "LZO compression",
      "description": "LZO compression enabled",
      "defaultValue": false,
      "displayType": "checkbox",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "snappy_enabled",
      "displayName": "Snappy compression",
      "description": "Snappy compression enabled",
      "defaultValue": true,
      "isReconfigurable": false,
      "displayType": "checkbox",
      "serviceName": "MAPREDUCE"
    },
    {
      "name": "mapred-site.xml",
      "displayName": "Custom MapReduce Configs",
      "description": "If you wish to set configuration parameters not exposed through this page, you can specify them here.<br>The text you specify here will be injected into mapred-site.xml verbatim.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "serviceName": "MAPREDUCE",
      "category": "Advanced"
    },
    {
      "name": "jdk_location",
      "displayName": "URL to download 64-bit JDK",
      "description": "URL from where the 64-bit JDK binary can be downloaded",
      "defaultValue": "",
      "isRequired": false,
      "isReconfigurable": false,
      "displayType": "url",
      "serviceName": "MISC"
    },
    {
      "name": "java64_home",
      "displayName": "Path to 64-bit JAVA_HOME",
      "description": "Path to 64-bit JAVA_HOME",
      "defaultValue": "",
      "isRequired": false,
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "MISC"
    },
    /*
    {
      "name": "hadoop_log_dir",
      "displayName": "Hadoop Log Dir",
      "description": "Directory for Hadoop log files",
      "defaultValue": "/var/log/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "MISC",
      "category": "Advanced"
    },
    {
      "name": "hadoop_pid_dir",
      "displayName": "Hadoop PID Dir",
      "description": "Directory in which the pid files for Hadoop processes will be created",
      "defaultValue": "/var/run/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "MISC",
      "category": "Advanced"
    },
    */
    {
      "name": "using_local_repo",
      "displayName": "Whether a local repo is being used",
      "description": "Whether a local repo is being used",
      "defaultValue": false,
      "isReconfigurable": false,
      "displayType": "checkbox",
      "serviceName": "MISC"
    },
    {
      "name": "yum_repo_file",
      "displayName": "Path to local repo file",
      "description": "Path to local repository file that configures from where to download software packages",
      "defaultValue": "/etc/yum.repos.d/hdp.repo",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "MISC"
    },
    {
      "name": "hdfs_user",
      "displayName": "HDFS User",
      "description": "User to run HDFS as",
      "defaultValue": "hdfs",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "mapred_user",
      "displayName": "MapReduce User",
      "description": "User to run MapReduce as",
      "defaultValue": "mapred",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "hbase_user",
      "displayName": "HBase User",
      "description": "User to run HBase as",
      "defaultValue": "hbase",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "hive_user",
      "displayName": "Hive User",
      "description": "User to run Hive as",
      "defaultValue": "hive",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "hcat_user",
      "displayName": "HCat User",
      "description": "User to run HCatalog as",
      "defaultValue": "hcat",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "oozie_user",
      "displayName": "Oozie User",
      "description": "User to run Oozie as",
      "defaultValue": "oozie",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "pig_user",
      "displayName": "Pig User",
      "description": "User to run Pig as",
      "defaultValue": "pig",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "sqoop_user",
      "displayName": "Sqoop User",
      "description": "User to run Sqoop as",
      "defaultValue": "sqoop",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "zk_user",
      "displayName": "ZooKeeper User",
      "description": "User to run ZooKeeper as",
      "defaultValue": "zookeeper",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "user_group",
      "displayName": "Group",
      "description": "Group that the users specified above belong to",
      "defaultValue": "hadoop",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "MISC",
      "category": "Users/Groups"
    },
    {
      "name": "nagios_web_login",
      "displayName": "Nagios Admin username",
      "description": "Nagios Web UI Admin username",
      "defaultValue": "nagiosadmin",
      "isReconfigurable": false,
      "displayType": "user",
      "serviceName": "NAGIOS"
    },
    {
      "name": "nagios_web_password",
      "displayName": "Nagios Admin password",
      "description": "Nagios Web UI Admin password",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "password",
      "serviceName": "NAGIOS"
    },
    {
      "name": "nagios_contact",
      "displayName": "Hadoop Admin email",
      "description": "Hadoop Administrator email for alert notification",
      "defaultValue": "",
      "displayType": "email",
      "serviceName": "NAGIOS"
    },
    {
      "name": "oozieserver.host",
      "displayName": "Oozie Server host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run Oozie Server",
      "displayType": "masterHost",
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "name": "oozie_data_dir",
      "displayName": "Oozie DB directory",
      "description": "Data directory in which the Oozie DB exists",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "name": "oozie_log_dir",
      "displayName": "Oozie Log Dir",
      "description": "Directory for oozie logs",
      "defaultValue": "/var/log/oozie",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "OOZIE",
      "category": "Advanced"
    },
    {
      "name": "oozie_pid_dir",
      "displayName": "Oozie PID Dir",
      "description": "Directory in which the pid files for oozie processes will be created",
      "defaultValue": "/var/run/oozie",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "OOZIE",
      "category": "Advanced"
    },
    {
      "name": "oozie-site.xml",
      "displayName": "Custom Oozie Configs",
      "description": "If you wish to set configuration parameters not exposed through this page, you can specify them here.<br>The text you specify here will be injected into oozie-site.xml verbatim.",
      "defaultValue": "",
      "isRequired": false,
      "displayType": "custom",
      "serviceName": "OOZIE",
      "category": "Advanced"
    },
    {
      "name": "zookeeperserver.hosts",
      "displayName": "ZooKeeper Server hosts",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run ZooKeeper Server",
      "displayType": "masterHosts",
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    },
    {
      "name": "zk_data_dir",
      "displayName": "ZooKeeper directory",
      "description": "Data directory for ZooKeeper",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    },
    {
      "name": "zk_log_dir",
      "displayName": "ZooKeeper Log Dir",
      "description": "Directory for ZooKeeper log files",
      "defaultValue": "/var/log/zookeeper",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "ZOOKEEPER",
      "category": "Advanced"
    },
    {
      "name": "zk_pid_dir",
      "displayName": "ZooKeeper PID Dir",
      "description": "Directory in which the pid files for zookeeper processes will be created",
      "defaultValue": "/var/run/zookeeper",
      "isReconfigurable": false,
      "displayType": "directory",
      "serviceName": "ZOOKEEPER",
      "category": "Advanced"
    },
    {
      "name": "tickTime",
      "displayName": "Length of single Tick",
      "description": "The length of a single tick in milliseconds, which is the basic time unit used by ZooKeeper",
      "defaultValue": "2000",
      "displayType": "int",
      "unit": "ms",
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    },
    {
      "name": "initLimit",
      "displayName": "Ticks to allow for sync at Init",
      "description": "Amount of time, in ticks to allow followers to connect and sync to a leader",
      "defaultValue": "10",
      "displayType": "int",
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    },
    {
      "name": "syncLimit",
      "displayName": "Ticks to allow for sync at Runtime",
      "description": "Amount of time, in ticks to allow followers to connect",
      "defaultValue": "5",
      "displayType": "int",
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    },
    {
      "name": "clientPort",
      "displayName": "Port for running ZK Server",
      "description": "Port for running ZooKeeper server",
      "defaultValue": "2181",
      "displayType": "int",
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    }
  ]
};
