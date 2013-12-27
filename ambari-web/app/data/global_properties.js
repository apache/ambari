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
 *   isOverridable:
 *     Whether the config property can be overridden by hosts.
 *     If this is unspecified, true is assumed.
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
 *   isRequiredByAgent:
 *     Whether the config property is required by agent or not.
 *     If value is false then it will be not persisted in global configuration
 *
 *   serviceName:
 *     The service that the config property belongs to.
 *     E.g., "HDFS", "MAPREDUCE", "ZOOKEEPER", etc.
 *
 *   category: the category that the config property belongs to (used for grouping config properties in the UI).
 *     if unspecified, "General" is assumed.
 *     E.g., "General", "Advanced", "NameNode", "DataNode"
 *
 *   index: the sequence number in category, that point to place where config located regarding all rest in category.
 *     if unspecified, push to the end of array.
 *     E.g., 0, 1, '2'
 */

var App = require('app');
require('config');

module.exports =
{
  "configProperties": [
  /**********************************************HDFS***************************************/
    {
      "id": "puppet var",
      "name": "namenode_host",
      "displayName": "NameNode host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run NameNode",
      "displayType": "masterHost",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "namenode_heapsize",
      "displayName": "NameNode Java heap size",
      "description": "Initial and maximum Java heap size for NameNode (Java options -Xms and -Xmx).  This also applies to the Secondary NameNode.",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode",
      "index": 2
    },
    {
      "id": "puppet var",
      "name": "namenode_opt_newsize",
      "displayName": "NameNode new generation size",
      "description": "Default size of Java new generation for NameNode (Java option -XX:NewSize).  This also applies to the Secondary NameNode.",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "snamenode_host",
      "displayName": "SNameNode host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run SecondaryNameNode",
      "displayType": "masterHost",
      "isRequiredByAgent": false,
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "SNameNode",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "datanode_hosts", //not in the schema. For UI purpose
      "displayName": "DataNode hosts",
      "value": "",
      "defaultValue": "",
      "description": "The hosts that have been assigned to run DataNode",
      "displayType": "slaveHosts",
      "isRequired": false,
      "isRequiredByAgent": false,
      "isOverridable": false,
      "isVisible": true,
      "domain": "datanode-global",
      "serviceName": "HDFS",
      "category": "DataNode",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "dtnode_heapsize",
      "displayName": "DataNode maximum Java heap size",
      "description": "Maximum Java heap size for DataNode (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "datanode-global",
      "serviceName": "HDFS",
      "category": "DataNode",
      "index": 2
    },
    {
      "id": "puppet var",
      "name": "hadoop_heapsize",
      "displayName": "Hadoop maximum Java heap size",
      "description": "Maximum Java heap size for daemons such as Balancer (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "HDFS",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "hdfs_log_dir_prefix",
      "displayName": "Hadoop Log Dir Prefix",
      "description": "The parent directory for Hadoop log files.  The HDFS log directory will be ${hadoop_log_dir_prefix} / ${hdfs_user} and the MapReduce log directory will be ${hadoop_log_dir_prefix} / ${mapred_user}.",
      "defaultValue": "/var/log/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "hadoop_pid_dir_prefix",
      "displayName": "Hadoop PID Dir Prefix",
      "description": "The parent directory in which the PID files for Hadoop processes will be created.  The HDFS PID directory will be ${hadoop_pid_dir_prefix} / ${hdfs_user} and the MapReduce PID directory will be ${hadoop_pid_dir_prefix} / ${mapred_user}.",
      "defaultValue": "/var/run/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "namenode_opt_maxnewsize",
      "displayName": "NameNode maximum new generation size",
      "description": "",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "id": "puppet var",
      "name": "security_enabled",
      "displayName": "Hadoop Security",
      "description": "Enable hadoop security",
      "defaultValue": 'false',
      "isRequired": false,
      "displayType": "checkbox",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "HDFS",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "namenode_formatted_mark_dir",
      "displayName": "Hadoop formatted mark directory",
      "description": "",
      "defaultValue": "/var/run/hadoop/hdfs/namenode/formatted/",
      "isRequired": false,
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "serviceName": "HDFS",
      "category": "NameNode"
    },

  /**********************************************GLUSTERFS***************************************/
    {
        "id": "puppet var",
        "name": "fs_glusterfs_default_name",
        "displayName": "GlusterFS default fs name 1.x Hadoop",
        "description": "GlusterFS default filesystem name (glusterfs://{MasterFQDN}:9000)",
        "defaultValue": "glusterfs:///",
        "displayType": "string",
        "isVisible": true,
        "domain": "global",
        "serviceName": "GLUSTERFS",
        "category": "General"
      },
      {
          "id": "puppet var",
          "name": "glusterfs_defaultFS_name",
          "displayName": "GlusterFS default fs name 2.x Hadoop",
          "description": "GlusterFS default filesystem name (glusterfs:///)",
          "defaultValue": "glusterfs:///",
          "displayType": "string",
          "isVisible": true,
          "domain": "global",
          "serviceName": "GLUSTERFS",
          "category": "General"
        },
      {
        "id": "puppet var",
        "name": "fs_glusterfs_volname",
        "displayName": "GlusterFS volume name",
        "description": "GlusterFS volume name",
        "defaultValue": "HadoopVol",
        "displayType": "string",
        "isVisible": true,
        "domain": "global",
        "serviceName": "GLUSTERFS",
        "category": "General"
      },
      {
          "id": "puppet var",
          "name": "fs_glusterfs_mount",
          "displayName": "GlusterFS mount point",
          "description": "GlusterFS mount point",
          "defaultValue": "/mnt/glusterfs",
          "displayType": "string",
          "isVisible": true,
          "domain": "global",
          "serviceName": "GLUSTERFS",
          "category": "General"
        },
        {
          "id": "puppet var",
          "name": "fs_glusterfs_impl",
          "displayName": "GlusterFS fs impl",
          "description": "GlusterFS fs impl",
          "defaultValue": "org.apache.hadoop.fs.glusterfs.GlusterFileSystem",
          "displayType": "string",
          "isVisible": true,
          "domain": "global",
          "serviceName": "GLUSTERFS",
          "category": "General"
        },
        {
          "id": "puppet var",
          "name": "fs_glusterfs_getfattrcmd",
          "displayName": "GlusterFS getfattr command",
          "description": "GlusterFS getfattr command",
          "defaultValue": "sudo getfattr -m . -n trusted.glusterfs.pathinfo",
          "displayType": "string",
          "isVisible": true,
          "domain": "global",
          "serviceName": "GLUSTERFS",
          "category": "General"
        },
        {
            "id": "puppet var",
            "name": "fs_AbstractFileSystem_glusterfs_impl",
            "displayName": "GlusterFS Abstract Filesystem declaration",
            "description": "GlusterFS Abstract Filesystem declaration",
            "defaultValue": "org.apache.hadoop.fs.local.GlusterFs",
            "displayType": "string",
            "isVisible": true,
            "domain": "global",
            "serviceName": "GLUSTERFS",
            "category": "General"
          },
  /**********************************************MAPREDUCE***************************************/
    {
      "id": "puppet var",
      "name": "jobtracker_host",
      "displayName": "JobTracker host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run JobTracker",
      "displayType": "masterHost",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "jtnode_opt_newsize",
      "displayName": "JobTracker new generation size",
      "description": "Default size of Java new generation size for JobTracker in MB (Java option -XX:NewSize)",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "jtnode_opt_maxnewsize",
      "displayName": "JobTracker maximum new generation size",
      "description": "Maximum size of Java new generation for JobTracker in MB (Java option -XX:MaxNewSize)",
      "defaultValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker",
      "index": 2
    },
    {
      "id": "puppet var",
      "name": "jtnode_heapsize",
      "displayName": "JobTracker maximum Java heap size",
      "description": "Maximum Java heap size for JobTracker in MB (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "tasktracker_hosts",
      "displayName": "TaskTracker hosts",
      "value": "",
      "defaultValue": "",
      "description": "The hosts that have been assigned to run TaskTracker",
      "displayType": "slaveHosts",
      "isOverridable": false,
      "isVisible": true,
      "isRequired": false,
      "isRequiredByAgent": false,
      "domain": "tasktracker-global",
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "lzo_enabled",
      "displayName": "LZO compression",
      "description": "LZO compression enabled",
      "defaultValue": true,
      "displayType": "checkbox",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "MAPREDUCE",
      "index": 11
    },
    {
      "id": "puppet var",
      "name": "snappy_enabled",
      "displayName": "Snappy compression",
      "description": "Snappy compression enabled",
      "defaultValue": true,
      "isReconfigurable": false,
      "displayType": "checkbox",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "MAPREDUCE",
      "index": 12
    },
    {
      "id": "puppet var",
      "name": "rca_enabled",
      "displayName": "Enable Job Diagnostics",
      "description": "Tools for tracing the path and troubleshooting the performance of MapReduce jobs",
      "defaultValue": true,
      "isReconfigurable": true,
      "isOverridable": false,
      "displayType": "checkbox",
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "index": 13
    },
  /**********************************************HBASE***************************************/
    {
      "id": "puppet var",
      "name": "hbasemaster_host",
      "displayName": "HBase Master hosts",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run HBase Master",
      "displayType": "masterHosts",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "HBase Master",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "hbase_master_heapsize",
      "displayName": "HBase Master Maximum Java heap size",
      "description": "Maximum Java heap size for HBase master (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "HBase Master",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "regionserver_hosts",
      "displayName": "RegionServer hosts",
      "value": "",
      "defaultValue": "",
      "description": "The hosts that have been assigned to run RegionServer",
      "displayType": "slaveHosts",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "isRequired": false,
      "domain": "regionserver-global",
      "serviceName": "HBASE",
      "category": "RegionServer",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "hbase_regionserver_heapsize",
      "displayName": "HBase RegionServers maximum Java heap size",
      "description": "Maximum Java heap size for HBase RegionServers (Java option -Xmx)",
      "defaultValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "domain": "regionserver-global",
      "serviceName": "HBASE",
      "category": "RegionServer",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "hbase_log_dir",
      "displayName": "HBase Log Dir",
      "description": "Directory for HBase logs",
      "defaultValue": "/var/log/hbase",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "hbase_pid_dir",
      "displayName": "HBase PID Dir",
      "description": "Directory in which the pid files for HBase processes will be created",
      "defaultValue": "/var/run/hbase",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "Advanced"
    },
  /**********************************************HIVE***************************************/
    {
      "id": "puppet var",
      "name": "hivemetastore_host",
      "displayName": "Hive Metastore host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run Hive Metastore",
      "displayType": "masterHost",
      "isRequiredByAgent": false,
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 0
    },
    // for existing MySQL
    {
      "id": "puppet var",
      "name": "hive_existing_mysql_database",
      "displayName": "Database Type",
      "value": "",
      "defaultValue": "MySQL",
      "description": "Using an existing MySQL database for Hive Metastore",
      "displayType": "masterHost",
      "isOverridable": false,
      "isVisible": false,
      "isReconfigurable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 1
    },
    // for existing Oracle
    {
      "id": "puppet var",
      "name": "hive_existing_oracle_database",
      "displayName": "Database Type",
      "value": "",
      "defaultValue": "Oracle",
      "description": "Using an existing Oracle database for Hive Metastore",
      "displayType": "masterHost",
      "isVisible": false,
      "isOverridable": false,
      "isReconfigurable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 1
    },
    // for new MySQL
    {
      "id": "puppet var",
      "name": "hive_ambari_database",
      "displayName": "Database Type",
      "value": "",
      "defaultValue": "MySQL",
      "description": "MySQL will be installed by Ambari",
      "displayType": "masterHost",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "hive_database",
      "displayName": "Hive Database",
      "value": "",
      "defaultValue": "New MySQL Database",
      "options": [
        {
          displayName: 'New MySQL Database',
          foreignKeys: ['hive_ambari_database', 'hive_ambari_host']
        },
        {
          displayName: 'Existing MySQL Database',
          foreignKeys: ['hive_existing_mysql_database', 'hive_existing_mysql_host']
        },
        {
          displayName: 'Existing Oracle Database',
          foreignKeys: ['hive_existing_oracle_database', 'hive_existing_oracle_host'],
          hidden: !App.supports.hiveOozieExtraDatabases
        }
      ],
      "description": "MySQL will be installed by Ambari",
      "displayType": "radio button",
      "isReconfigurable": false,
      "radioName": "hive-database",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 2
    },
    {
      "id": "puppet var",
      "name": "hive_hostname",
      "displayName": "Database Host",
      "description": "Specify the host on which the database is hosted",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "host",
      "isOverridable": false,
      "isVisible": false,
      "isObserved": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "hive_existing_mysql_host",
      "displayName": "Database Host",
      "description": "Specify the host on which the existing database is hosted",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "host",
      "isRequiredByAgent": false,
      "isOverridable": false,
      "isVisible": false,
      "isObserved": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "hive_existing_oracle_host",
      "displayName": "Database Host",
      "description": "Specify the host on which the existing database is hosted",
      "defaultValue": "",
      "isReconfigurable": false,
      "displayType": "host",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": false,
      "isObserved": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "hive_ambari_host",
      "value": "",
      "defaultValue": "",
      "displayName": "Database Host",
      "description": "The host where Hive Metastore database is located",
      "isReconfigurable": false,
      "displayType": "masterHost",
      "isRequiredByAgent": false,
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "hive_metastore_port",
      "displayName": "Hive metastore port",
      "description": "",
      "defaultValue": "9083",
      "isReconfigurable": false,
      "displayType": "int",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "hive_log_dir",
      "displayName": "Hive Log Dir",
      "description": "Directory for Hive log files",
      "defaultValue": "/var/log/hive",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "hive_pid_dir",
      "displayName": "Hive PID Dir",
      "description": "Directory in which the PID files for Hive processes will be created",
      "defaultValue": "/var/run/hive",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "mysql_connector_url",
      "displayName": "MySQL connector url",
      "description": "",
      "defaultValue": "${download_url}/mysql-connector-java-5.1.18.zip",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "serviceName": "HIVE",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "hive_aux_jars_path",
      "displayName": "Hive auxilary jar path",
      "description": "",
      "defaultValue": "/usr/lib/hcatalog/share/hcatalog/hcatalog-core.jar",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "serviceName": "HIVE",
      "category": "Advanced"
    },
  /**********************************************WEBHCAT***************************************/
    {
      "id": "puppet var",
      "name": "webhcatserver_host",
      "displayName": "WebHCat Server host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run WebHCat Server",
      "displayType": "masterHost",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "serviceName": "WEBHCAT",
      "category": "WebHCat Server"
    },
    {
      "id": "puppet var",
      "name": "hcat_log_dir",
      "displayName": "WebHCat Log Dir",
      "description": "Directory for WebHCat log files",
      "defaultValue": "/var/log/webhcat",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "WEBHCAT",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "hcat_pid_dir",
      "displayName": "WebHCat PID Dir",
      "description": "Directory in which the PID files for WebHCat processes will be created",
      "defaultValue": "/var/run/webhcat",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "WEBHCAT",
      "category": "Advanced"
    },
  /**********************************************OOZIE***************************************/
    {
      "id": "puppet var",
      "name": "oozieserver_host",
      "displayName": "Oozie Server host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run Oozie Server",
      "displayType": "masterHost",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 0
    },
    // for existing Oracle
    {
      "id": "puppet var",
      "name": "oozie_existing_oracle_database",
      "displayName": "Database Type",
      "value": "",
      "defaultValue": "Oracle",
      "description": "Using an existing Oracle database for Oozie Metastore",
      "displayType": "masterHost",
      "isVisible": false,
      "isObserved": true,
      "isReconfigurable": false,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 1
    },
    // for current derby
    {
      "id": "puppet var",
      "name": "oozie_derby_database",
      "displayName": "Database Type",
      "value": "",
      "defaultValue": "Derby",
      "description": "Using current Derby database for Oozie Metastore",
      "displayType": "masterHost",
      "isVisible": false,
      "isReconfigurable": false,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 1
    },
    // for existing MySQL oozie
    {
      "id": "puppet var",
      "name": "oozie_existing_mysql_database",
      "displayName": "Database Type",
      "value": "",
      "defaultValue": "MySQL",
      "description": "Using an existing MySQL database for Oozie Metastore",
      "displayType": "masterHost",
      "isVisible": false,
      "isReconfigurable": false,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "oozie_database",
      "displayName": "Oozie Database",
      "value": "",
      "defaultValue": "New Derby Database",
      "options": [
        {
          displayName: 'New Derby Database',
          foreignKeys: ['oozie_derby_database', 'oozie_ambari_host']
        },
        {
          displayName: 'Existing MySQL Database',
          foreignKeys: ['oozie_existing_mysql_database', 'oozie_existing_mysql_host'],
          hidden: !App.supports.hiveOozieExtraDatabases
        },
        {
          displayName: 'Existing Oracle Database',
          foreignKeys: ['oozie_existing_oracle_database', 'oozie_existing_oracle_host'],
          hidden: !App.supports.hiveOozieExtraDatabases
        }
      ],
      "description": "Current Derby Database will be installed by Ambari",
      "displayType": "radio button",
      "isReconfigurable": false,
      "isOverridable": false,
      "radioName": "oozie-database",
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 2
    },
    {
      "id": "puppet var",
      "name": "oozie_hostname",
      "defaultValue": "",
      "displayName": "Database Host",
      "description": "The host where the Oozie database is located",
      "isReconfigurable": true,
      "isOverridable": false,
      "displayType": "host",
      "isVisible": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "oozie_existing_mysql_host",
      "displayName": "Database Host",
      "description": "Specify the host on which the existing database is hosted",
      "defaultValue": "",
      "isReconfigurable": false,
      "isOverridable": false,
      "displayType": "host",
      "isRequiredByAgent": false,
      "isVisible": false,
      "isObserved": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "oozie_existing_oracle_host",
      "displayName": "Database Host",
      "description": "Specify the host on which the existing database is hosted",
      "defaultValue": "",
      "isReconfigurable": false,
      "isOverridable": false,
      "isRequiredByAgent": false,
      "displayType": "host",
      "isVisible": false,
      "isObserved": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "oozie_ambari_host",
      "value": "",
      "defaultValue": "",
      "displayName": "Database Host",
      "description": "Host on which the database will be created by Ambari",
      "isReconfigurable": false,
      "isOverridable": false,
      "isRequiredByAgent": false,
      "displayType": "masterHost",
      "isVisible": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "oozie_data_dir",
      "displayName": "Oozie Data Dir",
      "description": "Data directory in which the Oozie DB exists",
      "defaultValue": "",
      "defaultDirectory": "/hadoop/oozie/data",
      "isReconfigurable": true,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isRequired": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "index": 8
    },

    {
      "id": "puppet var",
      "name": "oozie_log_dir",
      "displayName": "Oozie Log Dir",
      "description": "Directory for oozie logs",
      "defaultValue": "/var/log/oozie",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "oozie_pid_dir",
      "displayName": "Oozie PID Dir",
      "description": "Directory in which the pid files for oozie processes will be created",
      "defaultValue": "/var/run/oozie",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Advanced"
    },
  /**********************************************NAGIOS***************************************/
    {
      "id": "puppet var",
      "name": "nagios_web_login",
      "displayName": "Nagios Admin username",
      "description": "Nagios Web UI Admin username",
      "defaultValue": "nagiosadmin",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "NAGIOS",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "nagios_web_password",
      "displayName": "Nagios Admin password",
      "description": "Nagios Web UI Admin password",
      "defaultValue": "",
      "isReconfigurable": true,
      "displayType": "password",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "NAGIOS",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "nagios_contact",
      "displayName": "Hadoop Admin email",
      "description": "Hadoop Administrator email for alert notification",
      "defaultValue": "",
      "displayType": "email",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "NAGIOS",
      "index": 2
    },
  /**********************************************ZOOKEEPER***************************************/
    {
      "id": "puppet var",
      "name": "zookeeperserver_hosts",
      "displayName": "ZooKeeper Server hosts",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run ZooKeeper Server",
      "displayType": "masterHosts",
      "isRequiredByAgent": false,
      "isVisible": true,
      "isOverridable": false,
      "isRequired": false,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "zk_data_dir",
      "displayName": "ZooKeeper directory",
      "description": "Data directory for ZooKeeper",
      "defaultValue": "",
      "defaultDirectory": "/hadoop/zookeeper",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "index": 1
    },
    {
      "id": "puppet var",
      "name": "tickTime",
      "displayName": "Length of single Tick",
      "description": "The length of a single tick in milliseconds, which is the basic time unit used by ZooKeeper",
      "defaultValue": "2000",
      "displayType": "int",
      "unit": "ms",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "index": 2
    },
    {
      "id": "puppet var",
      "name": "initLimit",
      "displayName": "Ticks to allow for sync at Init",
      "description": "Amount of time, in ticks to allow followers to connect and sync to a leader",
      "defaultValue": "10",
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "index": 3
    },
    {
      "id": "puppet var",
      "name": "syncLimit",
      "displayName": "Ticks to allow for sync at Runtime",
      "description": "Amount of time, in ticks to allow followers to connect",
      "defaultValue": "5",
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "index": 4
    },
    {
      "id": "puppet var",
      "name": "clientPort",
      "displayName": "Port for running ZK Server",
      "description": "Port for running ZooKeeper server",
      "defaultValue": "2181",
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "index": 5
    },
    {
      "id": "puppet var",
      "name": "zk_log_dir",
      "displayName": "ZooKeeper Log Dir",
      "description": "Directory for ZooKeeper log files",
      "defaultValue": "/var/log/zookeeper",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "category": "Advanced",
      "index": 0
    },
    {
      "id": "puppet var",
      "name": "zk_pid_dir",
      "displayName": "ZooKeeper PID Dir",
      "description": "Directory in which the pid files for zookeeper processes will be created",
      "defaultValue": "/var/run/zookeeper",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "category": "Advanced",
      "index": 1
    },
  /**********************************************HUE***************************************/
    {
      "id": "puppet var",
      "name": "hueserver_host",
      "displayName": "Hue Server host",
      "value": "",
      "defaultValue": "",
      "description": "The host that has been assigned to run Hue Server",
      "displayType": "masterHost",
      "isRequiredByAgent": false,
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HUE",
      "category": "Hue Server"
    },
    {
      "id": "puppet var",
      "name": "hue_log_dir",
      "displayName": "HUE Log Dir",
      "description": "Directory for HUE logs",
      "defaultValue": "/var/log/hue",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "serviceName": "HUE",
      "category": "Advanced"
    },
    {
      "id": "puppet var",
      "name": "hue_pid_dir",
      "displayName": "HUE Pid Dir",
      "description": "Directory in which the pid files for HUE processes will be created",
      "defaultValue": "/var/run/hue",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isRequiredByAgent": false,
      "isVisible": true,
      "serviceName": "HUE",
      "category": "Advanced"
    },
  /**********************************************GANGLIA***************************************/
    {
      "id": "puppet var",
      "name": "ganglia_conf_dir",
      "displayName": "Ganglia conf directory",
      "description": "",
      "defaultValue": "/etc/ganglia/hdp",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "serviceName": "GANGLIA",
      "category": "Advanced"
    },
  /**********************************************MISC******************************************/
    {
      "id": "puppet var",
      "name": "proxyuser_group",
      "displayName": "Proxy group for Hive, WebHCat, and Oozie",
      "description": "",
      "defaultValue": "users",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "filename": "core-site.xml",
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HIVE", "WEBHCAT", "OOZIE"]
    },
    {
      "id": "puppet var",
      "name": "ganglia_runtime_dir",
      "displayName": "Ganglia runtime directory",
      "description": "",
      "defaultValue": "/var/run/ganglia/hdp",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "serviceName": "MISC",
      "category": "General",
      "belongsToService": []
    },
    {
      "id": "puppet var",
      "name": "hdfs_user",
      "displayName": "HDFS User",
      "description": "User to run HDFS as",
      "defaultValue": "hdfs",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HDFS"]
    },
    {
      "id": "puppet var",
      "name": "mapred_user",
      "displayName": "MapReduce User",
      "description": "User to run MapReduce as",
      "defaultValue": "mapred",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["MAPREDUCE"]
    },
    {
      "id": "puppet var",
      "name": "hbase_user",
      "displayName": "HBase User",
      "description": "User to run HBase as",
      "defaultValue": "hbase",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HBASE"]
    },
    {
      "id": "puppet var",
      "name": "hive_user",
      "displayName": "Hive User",
      "description": "User to run Hive as",
      "defaultValue": "hive",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HIVE"]
    },
    {
      "id": "puppet var",
      "name": "hcat_user",
      "displayName": "HCat User",
      "description": "User to run HCatalog as",
      "defaultValue": "hcat",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HCATALOG"]
    },
    {
      "id": "puppet var",
      "name": "webhcat_user",
      "displayName": "WebHCat User",
      "description": "User to run WebHCat as",
      "defaultValue": "hcat",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["WEBHCAT"]
    },
    {
      "id": "puppet var",
      "name": "oozie_user",
      "displayName": "Oozie User",
      "description": "User to run Oozie as",
      "defaultValue": "oozie",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["OOZIE"]
    },
    {
      "id": "puppet var",
      "name": "zk_user",
      "displayName": "ZooKeeper User",
      "description": "User to run ZooKeeper as",
      "defaultValue": "zookeeper",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["ZOOKEEPER"]
    },
    {
      "id": "puppet var",
      "name": "gmetad_user",
      "displayName": "Ganglia User",
      "description": "The user used to run Ganglia",
      "defaultValue": "nobody",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["GANGLIA"]
    },
    {
      "id": "puppet var",
      "name": "gmond_user",
      "displayName": "Gmond User",
      "description": "The user used to run gmond for Ganglia",
      "defaultValue": "nobody",
      "isReconfigurable": false,
      "displayType": "advanced",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": []
    },
    {
      "id": "puppet var",
      "name": "nagios_user",
      "displayName": "Nagios User",
      "description": "User to run Nagios as",
      "defaultValue": "nagios",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["NAGIOS"]
    },
    {
      "id": "puppet var",
      "name": "nagios_group",
      "displayName": "Nagios Group",
      "description": "Nagios Group",
      "defaultValue": "nagios",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["NAGIOS"]
    },
    {
      "id": "puppet var",
      "name": "smokeuser",
      "displayName": "Smoke Test User",
      "description": "The user used to run service smoke tests",
      "defaultValue": "ambari-qa",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": App.supports.customizeSmokeTestUser,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HDFS"]
    },
    {
      "id": "puppet var",
      "name": "user_group",
      "displayName": "Hadoop Group",
      "description": "Group that the users specified above belong to",
      "defaultValue": "hadoop",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HDFS"]
    },
    {
      "id": "puppet var",
      "name": "rrdcached_base_dir",
      "displayName": "Ganglia rrdcached base directory",
      "description": "Default directory for saving the rrd files on ganglia server",
      "defaultValue": "/var/lib/ganglia/rrds",
      "displayType": "directory",
      "isReconfigurable": true,
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "GANGLIA",
      "category": "General",
      "belongsToService": ["GANGLIA"]
    }
  ]
};
