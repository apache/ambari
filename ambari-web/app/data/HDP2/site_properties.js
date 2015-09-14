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
    "displayType": "directories",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "category": "SECONDARY_NAMENODE",
    "index": 1
  },
  {
    "name": "dfs.namenode.checkpoint.period",
    "displayType": "int",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 3
  },
  {
    "name": "dfs.namenode.name.dir",
    "displayType": "directories",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "category": "NAMENODE",
    "index": 1
  },
  {
    "name": "dfs.webhdfs.enabled",
    "displayType": "checkbox",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 0
  },
  {
    "name": "dfs.datanode.failed.volumes.tolerated",
    "displayType": "int",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 3
  },
  {
    "name": "dfs.datanode.data.dir.mount.file",
    "displayType": "directory",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "index": 4
  },
  {
    "name": "dfs.datanode.data.dir",
    "displayType": "directories",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 1
  },
  {
    "name": "dfs.datanode.data.dir.perm",
    "displayType": "int",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "nfs.file.dump.dir",
    "displayType": "directory",
    "category": "NFS_GATEWAY",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 1
  },
  {
    "name": "dfs.namenode.accesstime.precision",
    "displayType": "long",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 2
  },
  {
    "name": "nfs.exports.allowed.hosts",
    "displayType": "string",
    "category": "NFS_GATEWAY",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 3
  },
  {
    "name": "dfs.replication",
    "displayType": "int",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.du.reserved",
    "displayType": "int",
    "category": "General",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 2
  },
  {
    "name": "dfs.client.read.shortcircuit",
    "displayType": "checkbox",
    "category": "Advanced hdfs-site",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "namenode_host",
    "displayName": "NameNode hosts",
    "value": "",
    "recommendedValue": "",
    "description": "The hosts that has been assigned to run NameNode",
    "displayType": "masterHosts",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 0
  },
  {
    "name": "namenode_heapsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 2
  },
  {
    "name": "namenode_opt_newsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 3
  },
  {
    "name": "namenode_opt_permsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 5
  },
  {
    "name": "namenode_opt_maxpermsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 6
  },
  {
    "name": "namenode_opt_maxnewsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NAMENODE",
    "index": 4
  },
  {
    "name": "snamenode_host",
    "displayName": "SNameNode host",
    "value": "",
    "recommendedValue": "",
    "description": "The host that has been assigned to run SecondaryNameNode",
    "displayType": "masterHost",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "SECONDARY_NAMENODE",
    "index": 0
  },
  {
    "name": "datanode_hosts", //not in the schema. For UI purpose
    "displayName": "DataNode hosts",
    "value": "",
    "recommendedValue": "",
    "description": "The hosts that have been assigned to run DataNode",
    "displayType": "slaveHosts",
    "isRequired": false,
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "DATANODE",
    "index": 0
  },
  {
    "name": "dtnode_heapsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "DATANODE",
    "index": 2
  },
  {
    "name": "nfsgateway_hosts", //not in the schema. For UI purpose
    "displayName": "NFSGateway hosts",
    "value": "",
    "recommendedValue": "",
    "description": "The hosts that have been assigned to run NFSGateway",
    "displayType": "slaveHosts",
    "isRequired": false,
    "isOverridable": false,
    "isVisible": true,
    "isRequiredByAgent": false,
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NFS_GATEWAY",
    "index": 0
  },
  {
    "name": "nfsgateway_heapsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "NFS_GATEWAY",
    "index": 1
  },
  {
    "name": "hadoop_heapsize",
    "displayType": "int",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "General",
    "index": 1
  },
  {
    "name": "hdfs_log_dir_prefix",
    "displayType": "directory",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "Advanced hadoop-env"
  },
  {
    "name": "hadoop_pid_dir_prefix",
    "displayType": "directory",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "Advanced hadoop-env"
  },
  {
    "name": "hadoop_root_logger",
    "displayType": "string",
    "serviceName": "HDFS",
    "filename": "hadoop-env.xml",
    "category": "Advanced hadoop-env"
  },

/**********************************************YARN***************************************/
  {
    "name": "yarn.acl.enable",
    "displayType": "checkbox",
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
    "displayType": "checkbox",
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
    "displayType": "int",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "CapacityScheduler"
  },
  {
    "name": "yarn.scheduler.maximum-allocation-mb",
    "displayType": "int",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "CapacityScheduler"
  },
  {
    "name": "yarn.nodemanager.resource.memory-mb",
    "displayType": "int",
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
    "displayType": "directories",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "NODEMANAGER"
  },
  {
    "name": "yarn.nodemanager.local-dirs",
    "displayType": "directories",
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
    "displayType": "int",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "General",
    "index": 0
  },
  {
    "name": "rm_host",
    "displayName": "ResourceManager",
    "description": "ResourceManager",
    "recommendedValue": "",
    "isOverridable": false,
    "displayType": "masterHost",
    "isRequiredByAgent": false,
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "RESOURCEMANAGER",
    "index": 0
  },
  {
    "name": "resourcemanager_heapsize",
    "displayType": "int",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "RESOURCEMANAGER",
    "index": 1
  },
  {
    "name": "nm_hosts",
    "displayName": "NodeManager",
    "description": "List of NodeManager Hosts.",
    "recommendedValue": "",
    "isOverridable": false,
    "displayType": "slaveHosts",
    "isRequiredByAgent": false,
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "NODEMANAGER",
    "index": 0
  },
  {
    "name": "nodemanager_heapsize",
    "displayType": "int",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "NODEMANAGER",
    "index": 0
  },
  {
    "name": "yarn_log_dir_prefix",
    "displayType": "directory",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "Advanced yarn-env"
  },
  {
    "name": "yarn_pid_dir_prefix",
    "displayType": "directory",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "Advanced yarn-env"
  },
  {
    "name": "min_user_id",
    "displayType": "int",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "Advanced yarn-env"
  },
  {
    "name": "ats_host",
    "displayName": "App Timeline Server",
    "description": "Application Timeline Server Host",
    "recommendedValue": "",
    "isOverridable": false,
    "displayType": "masterHost",
    "isRequiredByAgent": false,
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "APP_TIMELINE_SERVER",
    "index": 0
  },
  {
    "name": "apptimelineserver_heapsize",
    "displayType": "int",
    "serviceName": "YARN",
    "filename": "yarn-env.xml",
    "category": "APP_TIMELINE_SERVER",
    "index": 1
  },
/**********************************************MAPREDUCE2***************************************/
  {
    "name": "mapreduce.map.memory.mb",
    "displayType": "int",
    "category": "General",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.reduce.memory.mb",
    "displayType": "int",
    "category": "General",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.task.io.sort.mb",
    "displayType": "int",
    "category": "General",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-site.xml"
  },
  {
    "name": "hadoop.security.auth_to_local",
    "displayType": "multiLine",
    "serviceName": "HDFS",
    "filename": "core-site.xml",
    "category": "Advanced core-site"
  },
  {
    "name": "yarn.app.mapreduce.am.resource.mb",
    "displayType": "int",
    "category": "Advanced mapred-site",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-site.xml"
  },
  {
    "name": "hs_host",
    "displayName": "History Server",
    "description": "History Server",
    "recommendedValue": "",
    "isOverridable": false,
    "displayType": "masterHost",
    "isRequiredByAgent": false,
    "isVisible": true,
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-env.xml",
    "category": "HISTORYSERVER",
    "index": 0
  },
  {
    "name": "jobhistory_heapsize",
    "displayType": "int",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-env.xml",
    "category": "HISTORYSERVER",
    "index": 1
  },
  {
    "name": "mapred_log_dir_prefix",
    "displayType": "directory",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-env.xml",
    "category": "Advanced mapred-env"
  },
  {
    "name": "mapred_pid_dir_prefix",
    "displayType": "directory",
    "serviceName": "MAPREDUCE2",
    "filename": "mapred-env.xml",
    "category": "Advanced mapred-env"
  },
/**********************************************oozie-site***************************************/
  {
    "name": "oozie.db.schema.name",
    "displayType": "host",
    "category": "OOZIE_SERVER",
    "serviceName": "OOZIE",
    "filename": "oozie-site.xml",
    "index": 4
  },
  {
    "name": "oozie.service.JPAService.jdbc.username",
    "displayType": "user",
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
    "displayType": "advanced",
    "category": "OOZIE_SERVER",
    "serviceName": "OOZIE",
    "filename": "oozie-site.xml",
    "index": 8
  },
  {
    "name": "oozieserver_host",
    "displayName": "Oozie Server host",
    "value": "",
    "recommendedValue": "",
    "description": "The hosts that have been assigned to run Oozie Server",
    "displayType": "masterHosts",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "OOZIE_SERVER",
    "index": 0
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
        displayName: 'Existing SQLA Database',
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
    "displayType": "directory",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "OOZIE_SERVER",
    "index": 9
  },
  {
    "name": "oozie_hostname",
    "displayType": "host",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "OOZIE_SERVER",
    "index": 3
  },
  {
    "name": "oozie_log_dir",
    "displayType": "directory",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "Advanced oozie-env"
  },
  {
    "name": "oozie_pid_dir",
    "displayType": "directory",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "Advanced oozie-env"
  },
  {
    "name": "oozie_admin_port",
    "displayType": "int",
    "serviceName": "OOZIE",
    "filename": "oozie-env.xml",
    "category": "Advanced oozie-env"
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
    "displayType": "int",
    "index": 11
  },
  {
    "name": "hive.heapsize",
    "displayType": "int",
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
    "displayType": "int",
    "index": 10
  },
  {
    "name": "javax.jdo.option.ConnectionUserName",
    "displayType": "user",
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "index": 5
  },
  {
    "name": "javax.jdo.option.ConnectionPassword",
    "displayType": "password",
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "index": 6
  },
  {
    "name": "javax.jdo.option.ConnectionURL",
    "displayType": "advanced",
    "category": "HIVE_METASTORE",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "index": 8
  },
  {
    "name": "ambari.hive.db.schema.name",
    "displayType": "host",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "category": "HIVE_METASTORE",
    "index": 4
  },
  {
    "name": "hive.server2.tez.default.queues",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "category": "Advanced hive-site"
  },
  {
    "name": "hive.server2.thrift.port",
    "displayType": "int",
    "category": "Advanced hive-site",
    "serviceName": "HIVE",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.server2.support.dynamic.service.discovery",
    "displayType": "checkbox",
    "category": "Advanced hive-site",
    "serviceName": "HIVE",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.security.authorization.enabled",
    "displayType": "checkbox",
    "category": "Advanced hive-site",
    "serviceName": "HIVE",
    "filename": "hive-site.xml"
  },
  {
    "name": "hivemetastore_host",
    "displayName": "Hive Metastore hosts",
    "value": "",
    "recommendedValue": "",
    "description": "The hosts that have been assigned to run Hive Metastore",
    "displayType": "masterHosts",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "HIVE_METASTORE",
    "index": 0
  },
  {
    "name": "hive_master_hosts",
    "value": "",
    "recommendedValue": "",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "isVisible": false,
    "serviceName": "HIVE",
    "filename": "hive-env.xml"
  },
  {
    "name": "hive_ambari_database",
    "displayName": "Database Type",
    "value": "",
    "recommendedValue": "MySQL",
    "description": "MySQL will be installed by Ambari",
    "displayType": "masterHost",
    "isOverridable": false,
    "isReconfigurable": false,
    "isRequiredByAgent": false,
    "isVisible": false,
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "HIVE_METASTORE",
    "index": 1
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
        displayName: 'Existing SQLA Database',
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
    "name": "hive_ambari_host",
    "value": "",
    "recommendedValue": "",
    "displayName": "Database Host",
    "description": "Host on which the database will be created by Ambari",
    "isReconfigurable": false,
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isRequiredByAgent": false,
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "HIVE_METASTORE",
    "index": 3
  },
  {
    "name": "hive_log_dir",
    "displayType": "directory",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "Advanced hive-env"
  },
  {
    "name": "hive_pid_dir",
    "displayType": "directory",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "Advanced hive-env"
  },
  {
    "name": "webhcatserver_host",
    "displayName": "WebHCat Server host",
    "value": "",
    "recommendedValue": "",
    "description": "The host that has been assigned to run WebHCat Server",
    "displayType": "masterHost",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "HIVE",
    "filename": "webhcat-env.xml",
    "category": "WEBHCAT_SERVER"
  },
  {
    "name": "hcat_log_dir",
    "displayType": "directory",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "Advanced webhcat-env"
  },
  {
    "name": "hcat_pid_dir",
    "displayType": "directory",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "Advanced webhcat-env"
  },
  {
    "name": "hive_database_name",
    "serviceName": "HIVE",
    "filename": "hive-env.xml",
    "category": "Advanced hive-env"
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
    "displayType": "int",
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
    "displayType": "float",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.grouping.min-size",
    "displayType": "int",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.grouping.max-size",
    "displayType": "int",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.am.log.level",
    "displayType": "string",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-input.compress.codec",
    "displayType": "string",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-input.is-compressed",
    "displayType": "checkbox",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-output.compress.codec",
    "displayType": "string",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },
  {
    "name": "tez.runtime.intermediate-output.should-compress",
    "displayType": "checkbox",
    "category": "General",
    "serviceName": "TEZ",
    "filename": "tez-site.xml"
  },

/**********************************************HBASE***************************************/
  {
    "name": "hbase.tmp.dir",
    "displayType": "directory",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.master.port",
    "displayType": "int",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "name": "hbase.regionserver.global.memstore.upperLimit",
    "displayType": "float",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.regionserver.global.memstore.lowerLimit",
    "displayType": "float",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hstore.blockingStoreFiles",
    "displayType": "int",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hstore.compactionThreshold",
    "displayType": "int",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 0
  },
  {
    "name": "hfile.block.cache.size",
    "displayType": "float",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 1
  },
  {
    "name": "hbase.hregion.max.filesize",
    "displayType": "int",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 2
  },
  {
    "name": "hbase.regionserver.handler.count",
    "displayType": "int",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 2
  },
  {
    "name": "hbase.hregion.majorcompaction",
    "displayType": "int",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 3
  },
  {
    "name": "hbase.hregion.memstore.block.multiplier",
    "displayType": "int",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 4
  },
  {
    "name": "hbase.hregion.memstore.mslab.enabled",
    "displayType": "checkbox",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.hregion.memstore.flush.size",
    "displayType": "int",
    "category": "HBASE_REGIONSERVER",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 5
  },
  {
    "name": "hbase.client.scanner.caching",
    "displayType": "int",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 3
  },
  {
    "name": "zookeeper.session.timeout",
    "displayType": "int",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 4
  },
  {
    "name": "hbase.client.keyvalue.maxsize",
    "displayType": "int",
    "category": "General",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "index": 5
  },
  {
    "name": "hbase.coprocessor.region.classes",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.coprocessor.master.classes",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.coprocessor.regionserver.classes",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.zookeeper.quorum",
    "displayType": "multiLine",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "name": "hbasemaster_host",
    "displayName": "HBase Master hosts",
    "value": "",
    "recommendedValue": "",
    "description": "The host that has been assigned to run HBase Master",
    "displayType": "masterHosts",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "isVisible": true,
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_MASTER",
    "index": 0
  },
  {
    "name": "hbase_master_heapsize",
    "displayType": "int",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_MASTER",
    "index": 1
  },
  {
    "name": "regionserver_hosts",
    "displayName": "RegionServer hosts",
    "value": "",
    "recommendedValue": "",
    "description": "The hosts that have been assigned to run RegionServer",
    "displayType": "slaveHosts",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_REGIONSERVER",
    "index": 0
  },
  {
    "name": "hbase_regionserver_heapsize",
    "displayType": "int",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_REGIONSERVER",
    "index": 1
  },
  {
    "name": "hbase_regionserver_xmn_max",
    "displayType": "int",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_REGIONSERVER",
    "index": 6
  },
  {
    "name": "hbase_regionserver_xmn_ratio",
    "displayType": "float",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "HBASE_REGIONSERVER",
    "index": 7
  },
  {
    "name": "hbase_log_dir",
    "displayType": "directory",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "Advanced hbase-env"
  },
  {
    "name": "hbase_pid_dir",
    "displayType": "directory",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "Advanced hbase-env"
  },

/**********************************************storm-site***************************************/
  {
    "name": "storm.zookeeper.root",
    "displayType": "directory",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.local.dir",
    "displayType": "directory",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.servers",
    "displayType": "masterHosts",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.port",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.session.timeout",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.connection.timeout",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.retry.times",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.retry.interval",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "General"
  },
  {
    "name": "storm.zookeeper.retry.intervalceiling.millis",
    "displayType": "int",
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
    "displayType": "checkbox",
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
    "displayType": "int",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "serviceName": "STORM",
    "category": "General",
    "name": "storm.messaging.netty.max_retries",
    "displayType": "int",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.max_wait_ms",
    "displayType": "int",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.min_wait_ms",
    "displayType": "int",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.server_worker_threads",
    "displayType": "int",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "storm.messaging.netty.client_worker_threads",
    "displayType": "int",
    "category": "General",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "nimbus.host",
    "displayType": "masterHost",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.thrift.port",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.thrift.max_buffer_size",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.childopts",
    "displayType": "multiLine",
    "serviceName": "STORM",
    "category": "NIMBUS",
    "filename": "storm-site.xml"
  },
  {
    "name": "nimbus.task.timeout.secs",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.supervisor.timeout.secs",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.monitor.freq.secs",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.cleanup.inbox.freq.secs",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.inbox.jar.expiration.secs",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.task.launch.secs",
    "displayType": "int",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.reassign",
    "displayType": "checkbox",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "nimbus.file.copy.expiration.secs",
    "displayType": "int",
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
    "displayType": "string",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "SUPERVISOR"
  },
  {
    "name": "supervisor.childopts",
    "displayType": "multiLine",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.worker.start.timeout.secs",
    "displayType": "int",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.worker.timeout.secs",
    "displayType": "int",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.monitor.frequency.secs",
    "displayType": "int",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "supervisor.heartbeat.frequency.secs",
    "displayType": "int",
    "category": "SUPERVISOR",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.port",
    "displayType": "int",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.worker.threads",
    "displayType": "int",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.queue.size",
    "displayType": "int",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.invocations.port",
    "displayType": "int",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.request.timeout.secs",
    "displayType": "int",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "drpc.childopts",
    "displayType": "string",
    "category": "DRPC_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "ui.port",
    "displayType": "int",
    "category": "STORM_UI_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "ui.childopts",
    "displayType": "string",
    "category": "STORM_UI_SERVER",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
  {
    "name": "worker.childopts",
    "displayType": "multiLine",
    "category": "Advanced storm-site",
    "serviceName": "STORM",
    "filename": "storm-site.xml"
  },
/*********************************************oozie-site for Falcon*****************************/
  {
    "name": "oozie.service.ELService.ext.functions.coord-job-submit-instances",
    "displayType": "custom",
    "category": "Falcon - Oozie integration",
    "serviceName": "FALCON",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-action-create-inst",
    "displayType": "custom",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-action-create",
    "displayType": "custom",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-job-submit-data",
    "displayType": "custom",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-action-start",
    "displayType": "custom",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-sla-submit",
    "displayType": "custom",
    "filename": "oozie-site.xml"
  },
  {
    "serviceName": "FALCON",
    "category": "Falcon - Oozie integration",
    "name": "oozie.service.ELService.ext.functions.coord-sla-create",
    "displayType": "custom",
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
    "displayType": "multiLine",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.configstore.listeners",
    "displayType": "multiLine",
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
    "displayType": "int",
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
    "displayType": "int",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.internal.queue.size",
    "displayType": "int",
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
    "displayType": "int",
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
    "displayType": "multiLine",
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
    "displayType": "directory",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.graph.serialize.path",
    "displayType": "directory",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.graph.preserve.history",
    "displayType": "checkbox",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },

/**********************************************webhcat-site***************************************/
  {
    "name": "templeton.hive.archive",
    "serviceName": "HIVE",
    "filename": "webhcat-site.xml",
    "category": "Advanced webhcat-site"
  },
  {
    "name": "templeton.pig.archive",
    "serviceName": "HIVE",
    "filename": "webhcat-site.xml",
    "category": "Advanced webhcat-site"
  },
  {
    "name": "templeton.zookeeper.hosts",
    "displayType": "multiLine",
    "serviceName": "HIVE",
    "filename": "webhcat-site.xml",
    "category": "Advanced webhcat-site"
  },
/**********************************************pig.properties*****************************************/
  {
    "name": "content",
    "category": "Advanced pig-properties",
    "serviceName": "PIG",
    "filename": "pig-properties.xml"
  },

/**********************************************KNOX*****************************************/
  {
    "name": "content",
    "displayType": "content",
    "serviceName": "KNOX",
    "filename": "topology.xml",
    "category": "Advanced topology"
  },

  {
    "name": "content",
    "displayType": "content",
    "serviceName": "KNOX",
    "filename": "users-ldif.xml",
    "category": "Advanced users-ldif"
  },
  {
    "name": "knox_gateway_host",
    "displayName": "Knox Gateway host",
    "value": "",
    "recommendedValue": "",
    "description": "The hosts that have been assigned to run Knox Gateway",
    "displayType": "masterHosts",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "KNOX",
    "filename": "knox-env.xml",
    "category": "KNOX_GATEWAY",
    "index": 0
  },
  {
    "name": "knox_master_secret",
    "serviceName": "KNOX",
    "filename": "knox-env.xml",
    "category": "KNOX_GATEWAY"
  },
  {
    "name": "knox_pid_dir",
    "displayType": "directory",
    "serviceName": "KNOX",
    "filename": "knox-env.xml",
    "category": "Advanced knox-env"
  },

/********************************************* KAFKA *****************************/
  {
    "name": "kafka_broker_hosts",
    "displayName": "Kafka Broker host",
    "value": "",
    "recommendedValue": "",
    "description": "The host that has been assigned to run Kafka Broker",
    "displayType": "masterHosts",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "log.dirs",
    "displayType": "directories",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "port",
    "displayType": "int",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
    {
    "name": "listeners",
    "displayType": "advanced",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER"
  },
  {
    "name": "log.roll.hours",
    "displayType": "advanced",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "log.retention.hours",
    "displayType": "advanced",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "zookeeper.connect",
    "displayType": "advanced",
    "serviceName": "KAFKA",
    "filename": "kafka-broker.xml",
    "category": "KAFKA_BROKER",
    "index": 0
  },
  {
    "name": "kafka_pid_dir",
    "displayType": "directory",
    "serviceName": "KAFKA",
    "filename": "kafka-env.xml",
    "category": "Advanced kafka-env",
    "index": 0
  },

/********************************************* ACCUMULO *****************************/
  {
    "name": "accumulo_instance_name",
    "displayType": "string",
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
    "displayType": "string",
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
    "displayType": "password",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "General"
  },
  {
    "name": "server_content",
    "displayType": "content",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_master_heapsize",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_tserver_heapsize",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_monitor_heapsize",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_gc_heapsize",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_other_heapsize",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_log_dir",
    "displayType": "directory",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_pid_dir",
    "displayType": "directory",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "accumulo_monitor_bind_all",
    "displayType": "checkbox",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-env.xml",
    "category": "Advanced accumulo-env"
  },
  {
    "name": "instance.volumes",
    "displayType": "string",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 0
  },
  {
    "name": "instance.zookeeper.host",
    "displayType": "string",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 1
  },
  {
    "name": "instance.zookeeper.timeout",
    "displayType": "string",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 2
  },
  {
    "name": "master.port.client",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 3
  },
  {
    "name": "tserver.port.client",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 4
  },
  {
    "name": "monitor.port.client",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 5
  },
  {
    "name": "monitor.port.log4j",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 6
  },
  {
    "name": "gc.port.client",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 7
  },
  {
    "name": "trace.port.client",
    "displayType": "int",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 8
  },
  {
    "name": "tserver.memory.maps.native.enabled",
    "displayType": "checkbox",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 9
  },
  {
    "name": "general.classpaths",
    "displayType": "content",
    "serviceName": "ACCUMULO",
    "filename": "accumulo-site.xml",
    "category": "Advanced accumulo-site",
    "index": 10
  },

/*******************************************kerberos***********************************/
  {
    "name": "kdc_type",
    "displayType": "masterHost",
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
    "displayType": "host",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "KDC",
    "index": 2
  },
  {
    "name": "ldap_url",
    "displayType": "host",
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
    "name": "manage_identities",
    "displayType": "checkbox",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 0
  },
  {
    "name": "install_packages",
    "displayType": "checkbox",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 1
  },
  {
    "name": "executable_search_paths",
    "displayType": "multiline",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 2
  },
  {
    "name": "encryption_types",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "displayType": "multiLine",
    "category": "Advanced kerberos-env",
    "index" : 3
  },
  {
    "name": "password_length",
    "displayType": "int",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 4
  },
  {
    "name": "password_min_lowercase_letters",
    "displayType": "int",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 5
  },
  {
    "name": "password_min_uppercase_letters",
    "displayType": "int",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 6
  },
  {
    "name": "password_min_digits",
    "displayType": "int",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 7
  },
  {
    "name": "password_min_punctuation",
    "displayType": "int",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 8
  },
  {
    "name": "password_min_whitespace",
    "displayType": "int",
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
    "displayType": "content",
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
    "displayType": "checkbox",
    "serviceName": "KERBEROS",
    "filename": "kerberos-env.xml",
    "category": "Advanced kerberos-env",
    "index" : 13
  },
  {
    "name": "domains",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "KDC",
    "index": 5
  },
  {
    "name": "admin_server_host",
    "displayType": "host",
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
    "displayType": "checkbox",
    "dependentConfigPattern": "CATEGORY",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Advanced krb5-conf",
    "index": 0
  },
  {
    "name": "conf_dir",
    "displayType": "directory",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Advanced krb5-conf",
    "index": 1
  },
  {
    "name": "content",
    "displayType": "content",
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Advanced krb5-conf",
    "index": 2
  },
/********************************************* flume-agent *****************************/
  {
    "name": "content",
    "displayType": "content",
    "serviceName": "FLUME",
    "category": "FLUME_HANDLER",
    "filename": "flume-conf.xml"
  },
  {
    "name": "flume_conf_dir",
    "displayType": "directory",
    "isOverridable": false,
    "serviceName": "FLUME",
    "filename": "flume-env.xml",
    "category": "Advanced flume-env",
    "index": 0
  },
  {
    "name": "flume_log_dir",
    "displayType": "directory",
    "serviceName": "FLUME",
    "filename": "flume-env.xml",
    "category": "Advanced flume-env",
    "index": 1
  },

  //***************************************** GLUSTERFS stack********************************************
  {
    "name": "fs.glusterfs.impl",
    "displayType": "string",
    "filename": "core-site.xml",
    "serviceName": "GLUSTERFS",
    "category": "General"
  },
  {
    "name": "fs.AbstractFileSystem.glusterfs.impl",
    "displayType": "string",
    "filename": "core-site.xml",
    "serviceName": "GLUSTERFS",
    "category": "General"
  },

/***************************************** ECS stack********************************************/
  {
    "name": "hdfs_log_dir_prefix",
    "displayType": "directory",
    "serviceName": "ECS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },

/**********************************************GLUSTERFS***************************************/
  {
    "name": "hadoop_heapsize",
    "displayType": "int",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop",
    "index": 1
  },
  {
    "name": "hdfs_log_dir_prefix",
    "displayType": "directory",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "hadoop_pid_dir_prefix",
    "displayType": "directory",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_heapsize",
    "displayType": "int",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_newsize",
    "displayType": "int",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_maxnewsize",
    "displayType": "int",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_permsize",
    "displayType": "int",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_opt_maxpermsize",
    "displayType": "int",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "dtnode_heapsize",
    "displayType": "int",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "glusterfs_user",
    "displayType": "string",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "namenode_host",
    "displayType": "string",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },
  {
    "name": "snamenode_host",
    "displayType": "string",
    "serviceName": "GLUSTERFS",
    "filename": "hadoop-env.xml",
    "category": "General Hadoop"
  },

/**********************************************ZOOKEEPER***************************************/
  {
    "name": "zookeeperserver_hosts",
    "displayName": "ZooKeeper Server hosts",
    "value": "",
    "recommendedValue": "",
    "description": "The host that has been assigned to run ZooKeeper Server",
    "displayType": "masterHosts",
    "isVisible": true,
    "isRequiredByAgent": false,
    "isOverridable": false,
    "isRequired": false,
    "serviceName": "ZOOKEEPER",
    "filename": "zookeeper-env.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 0
  },
  {
    "name": "dataDir",
    "displayType": "directory",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 1
  },
  {
    "name": "tickTime",
    "displayType": "int",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 2
  },
  {
    "name": "initLimit",
    "displayType": "int",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 3
  },
  {
    "name": "syncLimit",
    "displayType": "int",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 4
  },
  {
    "name": "clientPort",
    "displayType": "int",
    "serviceName": "ZOOKEEPER",
    "filename": "zoo.cfg.xml",
    "category": "ZOOKEEPER_SERVER",
    "index": 5
  },
  {
    "name": "zk_log_dir",
    "displayType": "directory",
    "serviceName": "ZOOKEEPER",
    "filename": "zookeeper-env.xml",
    "category": "Advanced zookeeper-env",
    "index": 0
  },
  {
    "name": "zk_pid_dir",
    "displayType": "directory",
    "serviceName": "ZOOKEEPER",
    "filename": "zookeeper-env.xml",
    "category": "Advanced zookeeper-env",
    "index": 1
  },
/**********************************************FALCON***************************************/
  {
    "name": "falconserver_host",
    "displayName": "Falcon Server",
    "description": "The host that has been assigned to run Falcon Server",
    "recommendedValue": "falcon",
    "displayType": "masterHost",
    "isRequiredByAgent": false,
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "FALCON_SERVER"
  },
  {
    "name": "falcon_port",
    "displayType": "int",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "FALCON_SERVER"
  },
  {
    "name": "falcon_local_dir",
    "displayType": "directory",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "FALCON_SERVER"
  },
  {
    "name": "falcon_store_uri",
    "displayType": "string",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "FALCON_SERVER"
  },
  {
    "name": "falcon_log_dir",
    "displayType": "directory",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "Advanced falcon-env"
  },
  {
    "name": "falcon_pid_dir",
    "displayType": "directory",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "Advanced falcon-env"
  },
  {
    "name": "falcon.embeddedmq",
    "displayType": "string",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "Advanced falcon-env"
  },
  {
    "name": "falcon.embeddedmq.data",
    "displayType": "directory",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "Advanced falcon-env"
  },
  {
    "name": "falcon.emeddedmq.port",
    "displayType": "string",
    "serviceName": "FALCON",
    "filename": "falcon-env.xml",
    "category": "Advanced falcon-env"
  },
/**********************************************STORM***************************************/
  {
    "name": "storm_log_dir",
    "displayType": "directory",
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Advanced storm-env"
  },
  {
    "name": "storm_pid_dir",
    "displayType": "directory",
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Advanced storm-env"
  },
  {
    "name": "stormuiserver_host",
    "displayName": "Storm UI Server host",
    "description": "The host that has been assigned to run Storm UI Server",
    "recommendedValue": "",
    "displayType": "masterHost",
    "isReconfigurable": false,
    "isRequiredByAgent": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "STORM_UI_SERVER"
  },
  {
    "name": "drpcserver_host",
    "displayName": "DRPC Server host",
    "description": "The host that has been assigned to run DRPC Server",
    "recommendedValue": "",
    "displayType": "masterHost",
    "isReconfigurable": false,
    "isRequiredByAgent": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "DRPC_SERVER"
  },
  {
    "name": "supervisor_hosts",
    "displayName": "Supervisor hosts",
    "description": "The host that has been assigned to run Supervisor",
    "recommendedValue": "",
    "isRequired": false,
    "displayType": "slaveHosts",
    "isReconfigurable": false,
    "isRequiredByAgent": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "isOverridable": false,
    "category": "SUPERVISOR"
  },
  {
    "name": "storm_rest_api_host",
    "displayName": "Storm REST API host",
    "description": "The host that has been assigned to run Storm REST API Server",
    "recommendedValue": "",
    "displayType": "masterHost",
    "isReconfigurable": false,
    "isRequiredByAgent": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "isOverridable": false,
    "category": "STORM_REST_API"
  },
/**********************************************MISC***************************************/
  {
    "name": "ignore_groupsusers_create",
    "displayType": "checkbox",
    "filename": "cluster-env.xml",
    "category": "Users and Groups"
  },
  {
    "name": "override_uid",
    "displayType": "checkbox",
    "filename": "cluster-env.xml",
    "category": "Users and Groups"
  },
  /******************************************Alert Notification***************************/
  {
    "name": "create_notification",
    "displayName": "Create Notification",
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "MISC",
    "category": "Notifications",
    "recommendedValue": "no",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.host",
    "displayName": "SMTP Host",
    "displayType": "host",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.port",
    "displayName": "SMTP Port",
    "displayType": "int",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.from",
    "displayName": "FROM Email Address",
    "displayType": "email",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "ambari.dispatch.recipients",
    "displayName": " TO Email Address",
    "displayType": "email",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "smtp_use_auth",
    "displayName": "SMTP server requires authentication",
    "displayType": "checkbox",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "recommendedValue": true,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "ambari.dispatch.credential.username",
    "displayName": "SMTP Username",
    "displayType": "string",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-2",
    "filename": "alert_notification"
  },
  {
    "name": "ambari.dispatch.credential.password",
    "displayName": "SMTP Password",
    "displayType": "string",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-2",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.starttls.enable",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
  {
    "name": "mail.smtp.startssl.enable",
    "isRequired": false,
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "MISC",
    "category": "Notifications",
    "rowStyleClass": "indent-1",
    "filename": "alert_notification"
  },
/************************************************AMBARI_METRICS******************************************/
  {
    "name": "timeline.metrics.service.operation.mode",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "General",
    "index": 1
  },
  {
    "name": "metrics_collector_log_dir",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 2
  },
  {
    "name": "metrics_collector_pid_dir",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 3
  },
  {
    "name": "metrics_monitor_log_dir",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 4
  },
  {
    "name": "metrics_monitor_pid_dir",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-env.xml",
    "category": "General",
    "index": 5
  },
  {
    "name": "timeline.metrics.aggregator.checkpoint.dir",
    "displayType": "directory",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 17
  },
  {
    "name": "timeline.metrics.cluster.aggregator.hourly.checkpointCutOffMultiplier",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 16
  },
  {
    "name": "timeline.metrics.cluster.aggregator.hourly.disabled",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 14
  },
  {
    "name": "timeline.metrics.cluster.aggregator.hourly.interval",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 15
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.checkpointCutOffMultiplier",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 13
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.disabled",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 10
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.interval",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 11
  },
  {
    "name": "timeline.metrics.cluster.aggregator.minute.timeslice.interval",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 12
  },
  {
    "name": "timeline.metrics.host.aggregator.hourly.checkpointCutOffMultiplier",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 9
  },
  {
    "name": "timeline.metrics.host.aggregator.hourly.disabled",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 7
  },
  {
    "name": "timeline.metrics.host.aggregator.hourly.interval",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 8
  },
  {
    "name": "timeline.metrics.host.aggregator.minute.checkpointCutOffMultiplier",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 6
  },
  {
    "name": "timeline.metrics.host.aggregator.minute.disabled",
    "displayType": "string",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 4
  },
  {
    "name": "timeline.metrics.host.aggregator.minute.interval",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 5
  },
  {
    "name": "timeline.metrics.service.checkpointDelay",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 1
  },
  {
    "name": "timeline.metrics.service.default.result.limit",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 2
  },
  {
    "name": "timeline.metrics.service.resultset.fetchSize",
    "displayType": "int",
    "serviceName": "AMBARI_METRICS",
    "filename": "ams-site.xml",
    "category": "MetricCollector",
    "index": 3
  },
  {
    "name": "ams.zookeeper.keytab",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "ams.zookeeper.principal",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hadoop.security.authentication",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.coprocessor.master.classes",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.coprocessor.region.classes",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.master.kerberos.principal",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.master.keytab.file",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.myclient.keytab",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.myclient.principal",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.regionserver.kerberos.principal",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.regionserver.keytab.file",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.security.authentication",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.security.authorization",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.zookeeper.property.authProvider.1",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.zookeeper.property.jaasLoginRenew",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.zookeeper.property.kerberos.removeHostFromPrincipal",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "hbase.zookeeper.property.kerberos.removeRealmFromPrincipal",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
  },
  {
    "name": "zookeeper.znode.parent",
    "serviceName": "AMBARI_METRICS",
    "category": "Advanced ams-hbase-security-site",
    "filename": "ams-hbase-security-site.xml"
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
];

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
      "displayType": "user",
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
          foreignKeys: ['hive_existing_mssql_server_database', 'hive_existing_mssql_server_host'],
          hidden: false
        },
        {
          displayName: 'Existing MSSQL Server database with integrated authentication',
          foreignKeys: ['hive_existing_mssql_server_2_database', 'hive_existing_mssql_server_2_host'],
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
          foreignKeys: ['oozie_existing_mssql_server_database', 'oozie_existing_mssql_server_host'],
          hidden: false
        },
        {
          displayName: 'Existing MSSQL Server database with integrated authentication',
          foreignKeys: ['oozie_existing_mssql_server_2_database', 'oozie_existing_mssql_server_2_host'],
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
    "displayType": "checkbox",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.leveldb-timeline-store.path",
    "category": "APP_TIMELINE_SERVER",
    "displayType": "directory",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.leveldb-timeline-store.ttl-interval-ms",
    "displayType": "int",
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
    "displayType": "checkbox",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.ttl-ms",
    "displayType": "int",
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
    "displayType": "string",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.webapp.https.address",
    "displayType": "string",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.address",
    "displayType": "string",
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
