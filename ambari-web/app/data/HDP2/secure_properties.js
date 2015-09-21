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


/*
 * Description of fields:
 * name: has to coincide with how the property will be saved to the file
 * displayName: how it will be shown in the Admin Security Wizard
 * serviceName: the tab in which it will appear in the Admin Security Wizard
 * filename: the file it is saved to, and the section of the command-#.json file
 * category: the accordion name in the tab shown in the Admin Security Wizard
 * component: Ambari component name
 */

var props = {
  "configProperties": [
    {

      "name": "security_enabled",
      "displayName": "Enable security",
      "value": "",
      "recommendedValue": 'true',
      "description": "Enable kerberos security for the cluster",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "filename": "cluster-env.xml",
      "category": "KERBEROS"
    },
    {

      "name": "kerberos_install_type",
      "displayName": "Type of security",
      "value": "",
      "recommendedValue": "MANUALLY_SET_KERBEROS",
      "description": "Type of kerberos security for the cluster",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {

      "name": "kerberos_domain",
      "displayName": "Realm name",
      "value": "",
      "recommendedValue": "EXAMPLE.COM",
      "description": "Realm name to be used for all principal names",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "filename": "cluster-env.xml",
      "category": "KERBEROS"
    },
    {

      "name": "kinit_path_local",
      "displayName": "Kerberos tool path",
      "value": "",
      "recommendedValue": "/usr/bin",
      "description": "Directoy path to installed kerberos tools like kinit, kdestroy etc. This can have multiple comma delimited paths",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "filename": "cluster-env.xml",
      "category": "KERBEROS"
    },
    {

      "name": "smokeuser_principal_name",
      "displayName": "Smoke test user principal",
      "value": "",
      "recommendedValue": "ambari-qa",
      "description": "This is the principal name for Smoke test user",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "isReconfigurable": false,
      "serviceName": "GENERAL",
      "category": "AMBARI"
    },
    {

      "name": "smokeuser_keytab",
      "displayName": "Path to smoke test user keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/smokeuser.headless.keytab",
      "description": "Path to keytab file for smoke test user",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "filename": "cluster-env.xml",
      "category": "AMBARI"
    },
    {

      "name": "hdfs_principal_name",
      "displayName": "HDFS user principal",
      "value": "",
      "recommendedValue": "hdfs",
      "description": "This is the principal name for HDFS user",
      "displayType": "principal",
      "isVisible": false,
      "isOverridable": false,
      "isReconfigurable": false,
      "serviceName": "GENERAL",
      "filename": "hadoop-env.xml",
      "category": "AMBARI"
    },
    {

      "name": "hdfs_user_keytab",
      "displayName": "Path to HDFS user keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/hdfs.headless.keytab",
      "description": "Path to keytab file for HDFS user",
      "displayType": "directory",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "filename": "hadoop-env.xml",
      "category": "AMBARI"
    },
    {

      "name": "hbase_principal_name",
      "displayName": "HBase user principal",
      "value": "",
      "recommendedValue": "hbase",
      "description": "This is the principal name for HBase user",
      "displayType": "principal",
      "isVisible": false,
      "isOverridable": false,
      "isReconfigurable": false,
      "serviceName": "GENERAL",
      "filename": "hbase-env.xml",
      "category": "AMBARI"
    },
    {

      "name": "hbase_user_keytab",
      "displayName": "Path to HBase user keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/hbase.headless.keytab",
      "description": "Path to keytab file for Hbase user",
      "displayType": "directory",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "filename": "hbase-env.xml",
      "category": "AMBARI"
    },

  /**********************************************HDFS***************************************/
    {

      "name": "namenode_host",
      "displayName": "NameNode hosts",
      "value": "",
      "recommendedValue": "",
      "description": "The hosts that has been assigned to run NameNode",
      "displayType": "masterHosts",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {

      "name": "namenode_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "nn/_HOST",
      "description": "Principal name for NameNode. _HOST will get automatically replaced with actual hostname at an instance of NameNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "NameNode",
      "components": ["NAMENODE"]
    },
    {

      "name": "namenode_keytab",
      "displayName": "Path to Keytab File",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/nn.service.keytab",
      "description": "Path to NameNode keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "NameNode",
      "components": ["NAMENODE"]
    },
    {

      "name": "snamenode_host",
      "displayName": "SNameNode host",
      "value": "",
      "recommendedValue": "localhost",
      "description": "The host that has been assigned to run SecondaryNameNode",
      "displayType": "masterHost",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "SNameNode"
    },
    {

      "name": "snamenode_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "nn/_HOST",
      "description": "Principal name for SNameNode. _HOST will get automatically replaced with actual hostname at an instance of SNameNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "SNameNode",
      "components": ["SECONDARY_NAMENODE"]
    },
    {

      "name": "snamenode_keytab",
      "displayName": "Path to Keytab File",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/nn.service.keytab",
      "description": "Path to SNameNode keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "SNameNode",
      "components": ["SECONDARY_NAMENODE"]
    },
    {

      "name": "journalnode_hosts",
      "displayName": "JournalNode hosts",
      "value": "",
      "recommendedValue": "localhost",
      "description": "The hosts that have been assigned to run JournalNodes",
      "displayType": "masterHosts",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "JournalNode"
    },
    {

      "name": "journalnode_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "jn/_HOST",
      "description": "Principal name for JournalNode. _HOST will get automatically replaced with actual hostname at every instance of JournalNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "JournalNode",
      "component": "JOURNALNODE"
    },
    {

      "name": "journalnode_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/jn.service.keytab",
      "description": "Path to JournalNode keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "JournalNode",
      "component": "JOURNALNODE"
    },
    {

      "name": "datanode_hosts", //not in the schema. For UI purpose
      "displayName": "DataNode hosts",
      "value": "",
      "recommendedValue": "",
      "description": "The hosts that have been assigned to run DataNode",
      "displayType": "slaveHosts",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {

      "name": "dfs_datanode_address",
      "displayName": "Datanode address",
      "value": "",
      "recommendedValue": "1019",
      "description": "Address for DataNode",
      "displayType": "principal",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {

      "name": "dfs_datanode_http_address",
      "displayName": "Datanode HTTP address",
      "value": "",
      "recommendedValue": "1022",
      "description": "Address for DataNode",
      "displayType": "principal",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {

      "name": "datanode_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "dn/_HOST",
      "description": "Principal name for DataNode. _HOST will get automatically replaced with actual hostname at every instance of DataNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode",
      "component": "DATANODE"
    },
    {

      "name": "datanode_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/dn.service.keytab",
      "description": "Path to DataNode keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode",
      "component": "DATANODE"
    },
    {

      "name": "hadoop_http_principal_name",
      "displayName": "DFS web principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access for HDFS components. _HOST will get automatically replaced with actual hostname at instance of HDFS component",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "General"
    },
    {

      "name": "hadoop_http_keytab",
      "displayName": "Path to SPNEGO keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for NameNode and SNameNode",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "General"
    },

  /**********************************************MAPREDUCE2***************************************/
    {

      "name": "jobhistoryserver_host",
      "displayName": "History Server host",
      "value": "",
      "recommendedValue": "",
      "description": "The host that has been assigned to run History Server",
      "displayType": "masterHost",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MAPREDUCE2",
      "category": "JobHistoryServer"
    },
    {

      "name": "jobhistory_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "jhs/_HOST",
      "description": "Principal name for History Server. _HOST will get automatically replaced with actual hostname at an instance of History Server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE2",
      "category": "JobHistoryServer",
      "component": "HISTORYSERVER"
    },
    {

      "name": "jobhistory_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/jhs.service.keytab",
      "description": "Path to History Server keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE2",
      "category": "JobHistoryServer",
      "component": "HISTORYSERVER"
    },
    {

      "name": "jobhistory_http_principal_name",
      "displayName": "Web principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access to Job History Server. _HOST will get automatically replaced with actual hostname at an instance of Job History Server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE2",
      "category": "JobHistoryServer"
    },
    {

      "name": "jobhistory_http_keytab",
      "displayName": "Path to SPNEGO keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for Job History Server",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE2",
      "category": "JobHistoryServer"
    },

  /**********************************************YARN***************************************/
    {

      "name": "resourcemanager_host",
      "displayName": "ResourceManager host",
      "value": "",
      "recommendedValue": "",
      "description": "The host that has been assigned to run ResourceManager",
      "displayType": "masterHost",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    // YARN Application Timeline Server
    {

      "name": "apptimelineserver_host",
      "displayName": "Application Timeline Server host",
      "value": "",
      "recommendedValue": "",
      "description": "The host that has been assigned to run AppTimelineServer",
      "displayType": "masterHost",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "YARN",
      "category": "AppTimelineServer"
    },
    {

      "name": "apptimelineserver_principal_name",
      "displayName": "App Timeline Server Principal name",
      "value": "",
      "recommendedValue": "yarn/_HOST",
      "description": "Principal name for App Timeline Server. _HOST will get automatically replaced with actual hostname at an instance of App Timeline Server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": true,
      "serviceName": "YARN",
      "category": "AppTimelineServer",
      "component": "APP_TIMELINE_SERVER"
    },
    {

      "name": "apptimelineserver_keytab",
      "displayName": "Path to App Timeline Server keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/yarn.service.keytab",
      "description": "Path to App Timeline Server keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": true,
      "serviceName": "YARN",
      "category": "AppTimelineServer",
      "component": "APP_TIMELINE_SERVER"
    },
    {

      "name": "apptimelineserver_http_principal_name",
      "displayName": "App Timeline Server HTTP Principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for App Timeline Server HTTP. _HOST will get automatically replaced with actual hostname at an instance of App Timeline Server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": true,
      "serviceName": "YARN",
      "category": "AppTimelineServer"
    },
    {

      "name": "apptimelineserver_http_keytab",
      "displayName": "Path to App Timeline Server SPNEGO HTTP keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to App Timeline Server SPNEGO HTTP keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": true,
      "serviceName": "YARN",
      "category": "AppTimelineServer"
    },

    // YARN Resource Manager
    {

      "name": "resourcemanager_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "rm/_HOST",
      "description": "Principal name for ResourceManager. _HOST will get automatically replaced with actual hostname at an instance of ResourceManager",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "ResourceManager",
      "component": "RESOURCEMANAGER"
    },
    {

      "name": "resourcemanager_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/rm.service.keytab",
      "description": "Path to ResourceManager keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "ResourceManager",
      "component": "RESOURCEMANAGER"
    },
    {

      "name": "resourcemanager_http_principal_name",
      "displayName": "Web principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access to ResourceManager. _HOST will get automatically replaced with actual hostname at an instance of ResourceManager",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {

      "name": "resourcemanager_http_keytab",
      "displayName": "Path to SPNEGO keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for ResourceManager",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "ResourceManager"
    },
    {

      "name": "nodemanager_host",
      "displayName": "NodeManager hosts",
      "value": "",
      "recommendedValue": "",
      "description": "The hosts that has been assigned to run NodeManager",
      "displayType": "slaveHosts",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {

      "name": "nodemanager_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "nm/_HOST",
      "description": "Principal name for NodeManager. _HOST will get automatically replaced with actual hostname at all instances of NodeManager",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "NodeManager",
      "component": "NODEMANAGER"
    },
    {

      "name": "nodemanager_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/nm.service.keytab",
      "description": "Path to NodeManager keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "NodeManager",
      "component": "NODEMANAGER"
    },
    {

      "name": "nodemanager_http_principal_name",
      "displayName": "Web principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access to NodeManager. _HOST will get automatically replaced with actual hostname at all instances of NodeManager",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {

      "name": "nodemanager_http_keytab",
      "displayName": "Path to SPNEGO keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for NodeManager",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {

      "name": "yarn_nodemanager_container-executor_class",
      "displayName": "yarn.nodemanager.container-executor.class",
      "value": "",
      "recommendedValue": "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor",
      "description": "Executor(launcher) of the containers",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "YARN",
      "category": "NodeManager"
    },
    {

      "name": "webhcatserver_host",
      "displayName": "WebHCat Server host",
      "value": "",
      "recommendedValue": "localhost",
      "description": "The host that has been assigned to run WebHCat Server",
      "displayType": "masterHost",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "WebHCat Server"
    },
    {

      "name": "webHCat_http_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access for WebHCat",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "WebHCat Server"
    },
    {

      "name": "webhcat_http_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for WebHCat",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "WebHCat Server"
    },

  /**********************************************HBASE***************************************/
    {

      "name": "hbasemaster_host",
      "displayName": "HBase Master hosts",
      "value": "",
      "recommendedValue": "",
      "description": "The host that has been assigned to run HBase Master",
      "displayType": "masterHosts",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "HBase Master"
    },
    {

      "name": "hbase_master_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "hbase/_HOST",
      "description": "Principal name for HBase master. _HOST will get automatically replaced with actual hostname at an instance of HBase Master",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HBASE",
      "category": "HBase Master",
      "components": ["HBASE_MASTER"]
    },
    {

      "name": "hbase_master_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/hbase.service.keytab",
      "description": "Path to HBase master keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HBASE",
      "category": "HBase Master",
      "components": ["HBASE_MASTER"]
    },
    {

      "name": "regionserver_hosts",
      "displayName": "RegionServer hosts",
      "value": "",
      "recommendedValue": "",
      "description": "The hosts that have been assigned to run RegionServer",
      "displayType": "slaveHosts",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {

      "name": "hbase_regionserver_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "hbase/_HOST",
      "description": "Principal name for RegionServer. _HOST will get automatically replaced with actual hostname at every instance of RegionServer",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HBASE",
      "category": "RegionServer",
      "components": ["HBASE_REGIONSERVER"]
    },
    {

      "name": "hbase_regionserver_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/hbase.service.keytab",
      "description": "Path to RegionServer keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HBASE",
      "category": "RegionServer",
      "components": ["HBASE_REGIONSERVER"]
    },

  /**********************************************HIVE***************************************/
    {

      "name": "hive_metastore",
      "displayName": "Hive Metastore hosts",
      "value": "",
      "recommendedValue": "localhost",
      "description": "The hosts that have been assigned to run Hive Metastore and HiveServer2",
      "displayType": "masterHosts",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {

      "name": "hive_metastore_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "hive/_HOST",
      "description": "Principal name for Hive Metastore and HiveServer2. _HOST will get automatically replaced with actual hostname at an instance of Hive Metastore and HiveServer2",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "components": ["HIVE_SERVER", "HIVE_METASTORE"]
    },
    {

      "name": "hive_metastore_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/hive.service.keytab",
      "description": "Path to Hive Metastore and HiveServer2 keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "components": ["HIVE_SERVER", "HIVE_METASTORE"]
    },
    {

      "name": "hive_metastore_http_principal_name",
      "displayName": "Web principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access to Hive Metastore and HiveServer2. _HOST will get automatically replaced with actual hostname at an instance of Hive Metastore and HiveServer2",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {

      "name": "hive_metastore_http_keytab",
      "displayName": "Path to SPNEGO keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for  Hive Metastore and HiveServer2",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },

  /**********************************************OOZIE***************************************/
    {

      "name": "oozie_servername",
      "displayName": "Oozie Server host",
      "value": "",
      "recommendedValue": "localhost",
      "description": "Oozie server host name",
      "displayType": "masterHost",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {

      "name": "oozie_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "oozie/_HOST",
      "description": "Principal name for Oozie server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "component": "OOZIE_SERVER"
    },
    {

      "name": "oozie_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/oozie.service.keytab",
      "description": "Path to Oozie server keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "Oozie Server",
      "component": "OOZIE_SERVER"
    },
    {

      "name": "oozie_http_principal_name",
      "displayName": "Web principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access to Oozie",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {

      "name": "oozie_http_keytab",
      "displayName": "Path to SPNEGO keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for oozie",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
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
      "isOverridable": false,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    },
    {

      "name": "zookeeper_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "zookeeper/_HOST",
      "description": "Principal name for ZooKeeper. _HOST will get automatically replaced with actual hostname at every instance of zookeeper server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "ZooKeeper Server",
      "component": "ZOOKEEPER_SERVER"
    },
    {

      "name": "zookeeper_keytab_path",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/zk.service.keytab",
      "description": "Path to ZooKeeper keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "ZooKeeper Server",
      "component": "ZOOKEEPER_SERVER"
    },
  /**********************************************Falcon***************************************/
    {

      "name": "falcon_server_host",
      "displayName": "Falcon server host",
      "value": "",
      "recommendedValue": "",
      "description": "Falcon Server host",
      "displayType": "masterHost",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "FALCON",
      "category": "Falcon Server"
    },
    {

      "name": "falcon_principal_name",
      "displayName": "Falcon principal name",
      "value": "",
      "recommendedValue": "falcon/_HOST",
      "description": "This is the principal name for Falcon Server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "FALCON",
      "category": "Falcon Server",
      "component": "FALCON_SERVER"
    },
    {

      "name": "falcon_keytab",
      "displayName": "Path to Falcon server keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/falcon.service.keytab",
      "description": "Path to the Falcon Server keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "FALCON",
      "category": "Falcon Server",
      "component": "FALCON_SERVER"
    },
    {

      "name": "falcon_http_principal_name",
      "displayName": "Web principal name",
      "value": "",
      "recommendedValue": "HTTP/_HOST",
      "description": "Principal name for SPNEGO access to Falcon",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "FALCON",
      "category": "Falcon Server"
    },
    {

      "name": "falcon_http_keytab",
      "displayName": "Path to SPNEGO keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to SPNEGO keytab file for Falcon",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "FALCON",
      "category": "Falcon Server"
    },
    {

      "name": "namenode_principal_name_falcon",
      "displayName": "NameNode principal name",
      "value": "",
      "recommendedValue": "nn/_HOST",
      "description": "NameNode principal to talk to config store",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "FALCON",
      "category": "Falcon Server"
    },

  /**********************************************Knox***************************************/
    {

      "name": "knox_gateway_hosts",
      "displayName": "Knox Gateway hosts",
      "value": "",
      "recommendedValue": "",
      "description": "The hosts that has been assigned to run Knox Gateway",
      "displayType": "masterHosts",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "KNOX",
      "category": "Knox Gateway"
    },
    {

      "name": "knox_principal_name",
      "displayName": "Principal name",
      "value": "",
      "recommendedValue": "knox/_HOST",
      "description": "This is the principal name for Knox Gateway",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "KNOX",
      "filename": "knox-env.xml",
      "category": "Knox Gateway",
      "component": "KNOX_GATEWAY"
    },
    {

      "name": "knox_keytab_path",
      "displayName": "Path to keytab file",
      "value": "",
      "recommendedValue": "/etc/security/keytabs/knox.service.keytab",
      "description": "This is the keytab file for Knox Gateway",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "KNOX",
      "filename": "knox-env.xml",
      "category": "Knox Gateway",
      "component": "KNOX_GATEWAY"
    }
  ]
};

var stormProperties = [
  {
    "id": "puppet var",
    "name": "storm_host",
    "displayName": "Storm component hosts",
    "value": "",
    "recommendedValue": "",
    "description": "Storm component hosts",
    "displayType": "slaveHosts",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "category": "Storm Topology"
  },
  {
    "id": "puppet var",
    "name": "storm_principal_name",
    "displayName": " Storm principal name",
    "value": "",
    "recommendedValue": "storm/_HOST",
    "description": "Principal name for Supervisor. _HOST will get automatically replaced with actual hostname at an instance of every storm component.",
    "displayType": "principal",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Storm Topology",
    "components": ["SUPERVISOR", "NIMBUS", "STORM_UI_SERVER"]
  },
  {
    "id": "puppet var",
    "name": "storm_keytab",
    "displayName": "Path to Storm keytab file",
    "value": "",
    "recommendedValue": "/etc/security/keytabs/storm.service.keytab",
    "description": "Path to the storm keytab file",
    "displayType": "directory",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Storm Topology",
    "components": ["SUPERVISOR", "NIMBUS"]
  }
];
var storm22Properties = [
  {
    "id": "puppet var",
    "name": "storm_principal_name",
    "displayName": " Storm principal name",
    "value": "",
    "recommendedValue": "storm/_HOST",
    "description": "Principal name for Storm components. _HOST will get automatically replaced with actual hostname at an instance of every storm component.",
    "displayType": "principal",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Storm Topology",
    "components": ["SUPERVISOR", "STORM_UI_SERVER", "DRPC_SERVER", "STORM_REST_API"]
  },
  {
    "id": "puppet var",
    "name": "storm_keytab",
    "displayName": "Path to Storm keytab file",
    "value": "",
    "recommendedValue": "/etc/security/keytabs/storm.service.keytab",
    "description": "Path to the storm keytab file",
    "displayType": "directory",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Storm Topology",
    "components": ["SUPERVISOR", "STORM_UI_SERVER", "DRPC_SERVER", "STORM_REST_API"]
  },
  {
    "id": "puppet var",
    "name": "nimbus_host",
    "displayName": "Nimbus hosts",
    "value": "",
    "recommendedValue": "",
    "description": "Nimbus component hosts",
    "displayType": "slaveHosts",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "category": "Nimbus"
  },
  {
    "id": "puppet var",
    "name": "nimbus_principal_name",
    "displayName": " Nimbus principal name",
    "value": "",
    "recommendedValue": "nimbus/_HOST",
    "description": "Nimbus Principal name",
    "displayType": "principal",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Nimbus",
    "components": ["NIMBUS","DRPC_SERVER"]
  },
  {
    "id": "puppet var",
    "name": "nimbus_keytab",
    "displayName": "Path to Nimbus keytab file",
    "value": "",
    "recommendedValue": "/etc/security/keytabs/nimbus.service.keytab",
    "description": "Path to the nimbus keytab file",
    "displayType": "directory",
    "isVisible": true,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Nimbus",
    "components": ["NIMBUS","DRPC_SERVER"]
  },
  {
    "id": "puppet var",
    "name": "storm_ui_principal_name",
    "displayName": "Storm UI principal name",
    "value": "",
    "recommendedValue": "HTTP/_HOST",
    "description": "Principal name for Storm UI",
    "displayType": "principal",
    "isVisible": false,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Nimbus"
  },
  {
    "id": "puppet var",
    "name": "storm_ui_keytab",
    "displayName": "Path to Nimbus UI keytab file",
    "value": "",
    "recommendedValue": "/etc/security/keytabs/spnego.service.keytab",
    "description": "Path to the Storm UI keytab file",
    "displayType": "directory",
    "isVisible": false,
    "isOverridable": false,
    "serviceName": "STORM",
    "filename": "storm-env.xml",
    "category": "Nimbus"
  }
];

if(App.get('isHadoop22Stack')) {
  props.configProperties.pushObjects(storm22Properties);
} else {
  props.configProperties.pushObjects(stormProperties);
}

module.exports = props;
