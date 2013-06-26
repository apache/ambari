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
    {
      "id": "puppet var",
      "name": "security_enabled",
      "displayName": "Enable security",
      "value": "",
      "defaultValue": 'true',
      "description": "Enable kerberos security for the cluster",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "kerberos_install_type",
      "displayName": "Type of security",
      "value": "",
      "defaultValue": "MANUALLY_SET_KERBEROS",
      "description": "Type of kerberos security for the cluster",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "keytab_path",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs",
      "description": "Type of kerberos security for the cluster",
      "displayType": "principal",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "kerberos_domain",
      "displayName": "Realm name",
      "value": "",
      "defaultValue": "EXAMPLE.COM",
      "description": "Realm name to be used for all principal names",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "kinit_path_local",
      "displayName": "kinit path",
      "value": "",
      "defaultValue": "/usr/bin/kinit",
      "description": "Path to installed kinit command",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_principal_name",
      "displayName": "DFS Web Principal name",
      "value": "",
      "defaultValue": "HTTP/_HOST",
      "description": "Principal name for spnego access for NameNode and SNameNode. _HOST will get automatically replaced with actual hostname at instance of NameNode and SNameNode",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "oozie_http_principal_name",
      "displayName": "Oozie Web Principal name",
      "value": "",
      "defaultValue": "HTTP/_HOST",
      "description": "Principal name for spnego access for Oozie",
      "displayType": "advanced",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "webHCat_http_principal_name",
      "displayName": "WebHCat Principal name",
      "value": "",
      "defaultValue": "HTTP/_HOST",
      "description": "Principal name for spnego access for WebHCat",
      "displayType": "advanced",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_keytab",
      "displayName": "Path to spnego keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to spnego keytab file for NameNode, SNameNode, Oozie and WebHCat",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "smokeuser_keytab",
      "displayName": "Path to smoke test user keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/smokeuser.headless.keytab",
      "description": "Path to keytab file for smoke test user",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },

    //HDFS
    {
      "id": "puppet var",
      "name": "namenode_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "nn/_HOST",
      "description": "Principal name for NameNode and SNameNode. _HOST will get automatically replaced with actual hostname at instance of NameNode and SNameNode",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "NameNode",
      "components": ["NAMENODE", "SECONDARY_NAMENODE"]
    },
    {
      "id": "puppet var",
      "name": "namenode_keytab",
      "displayName": "Path to Keytab File",
      "value": "",
      "defaultValue": "/etc/security/keytabs/nn.service.keytab",
      "description": "Path to NameNode and SNameNode keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "NameNode",
      "components": ["NAMENODE", "SECONDARY_NAMENODE"]
    },
    {
      "id": "puppet var",
      "name": "dfs_datanode_address",
      "displayName": "Datanode address",
      "value": "",
      "defaultValue": "1019",
      "description": "Address for DataNode",
      "displayType": "principal",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {
      "id": "puppet var",
      "name": "dfs_datanode_http_address",
      "displayName": "Datanode HTTP address",
      "value": "",
      "defaultValue": "1022",
      "description": "Address for DataNode",
      "displayType": "principal",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {
      "id": "puppet var",
      "name": "datanode_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "dn/_HOST",
      "description": "Principal name for DataNode. _HOST will get automatically replaced with actual hostname at every instance of DataNode",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode",
      "component": "DATANODE"
    },
    {
      "id": "puppet var",
      "name": "datanode_keytab",
      "displayName": "Path to Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/dn.service.keytab",
      "description": "Path to DataNode keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode",
      "component": "DATANODE"
    },
    //MAPREDUCE
    {
      "id": "puppet var",
      "name": "jobtracker_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "jt/_HOST",
      "description": "Principal name for JobTracker. _HOST will get automatically replaced with actual hostname at an instance of JobTracker",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker",
      "component": "JOBTRACKER"
    },
    {
      "id": "puppet var",
      "name": "jobtracker_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/jt.service.keytab",
      "description": "Path to JobTracker keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker",
      "component": "JOBTRACKER"
    },
    {
      "id": "puppet var",
      "name": "tasktracker_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "tt/_HOST",
      "description": "Principal name for TaskTracker. _HOST will get automatically replaced with actual hostname at every instance of TaskTracker",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker",
      "component": "TASKTRACKER"
    },
    {
      "id": "puppet var",
      "name": "tasktracker_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/tt.service.keytab",
      "description": "Path to TaskTracker keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker",
      "component": "TASKTRACKER"
    },

    //HBASE
    {
      "id": "puppet var",
      "name": "hbase_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "hbase/_HOST",
      "description": "Principal name for HBase master and RegionServer. _HOST will get automatically replaced with actual hostname at every instance of HBase master and RegionServer",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HBASE",
      "category": "HBase",
      "components": ["HBASE_MASTER", "HBASE_REGIONSERVER"]
    },
    {
      "id": "puppet var",
      "name": "hbase_service_keytab",
      "displayName": "Path to Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/hbase.service.keytab",
      "description": "Path to HBase master and RegionServer keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HBASE",
      "category": "HBase",
      "components": ["HBASE_MASTER", "HBASE_REGIONSERVER"]
    },

    //HIVE
    {
      "id": "puppet var",
      "name": "hive_metastore",
      "displayName": "Hive Metastore host",
      "value": "",
      "defaultValue": "localhost",
      "description": "The host that has been assigned to run Hive Metastore",
      "displayType": "masterHost",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "id": "puppet var",
      "name": "hive_metastore_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "hive/_HOST",
      "description": "Principal name for Hive Metastore. _HOST will get automatically replaced with actual hostname at an instance of Hive Metastore",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "component": "HIVE_SERVER"
    },
    {
      "id": "puppet var",
      "name": "hive_metastore__keytab",
      "displayName": "Path to Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/hive.service.keytab",
      "description": "Path to Hive Metastore keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore",
      "component": "HIVE_SERVER"

    },

    //OOZIE
    {
      "id": "puppet var",
      "name": "oozie_servername",
      "displayName": "Oozie Server host",
      "value": "",
      "defaultValue": "localhost",
      "description": "Oozie server host name",
      "displayType": "masterHost",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "oozie/_HOST",
      "description": "Principal name for Oozie server",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "component": "OOZIE_SERVER"
    },
    {
      "id": "puppet var",
      "name": "oozie_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/oozie.service.keytab",
      "description": "Path to Oozie server keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server",
      "component": "OOZIE_SERVER"
    },

    //ZooKeeper
    {
      "id": "puppet var",
      "name": "zookeeper_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "zookeeper/_HOST",
      "description": "Principal name for ZooKeeper. _HOST will get automatically replaced with actual hostname at every instance of zookeeper server",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "component": "ZOOKEEPER_SERVER"
    },
    {
      "id": "puppet var",
      "name": "zookeeper_keytab_path",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/zk.service.keytab",
      "description": "Path to ZooKeeper keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server",
      "component": "ZOOKEEPER_SERVER"
    },
    //NAGIOS
    {
      "id": "puppet var",
      "name": "nagios_server",
      "displayName": "Nagios Server host",
      "value": "",
      "defaultValue": "localhost",
      "description": "Nagios server host",
      "displayType": "masterHost",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "NAGIOS",
      "category": "Nagios Server"
    },
    {
      "id": "puppet var",
      "name": "nagios_principal_name",
      "displayName": "Principal name",
      "value": "",
      "defaultValue": "nagios",
      "description": "Primary name for Nagios server",
      "displayType": "advanced",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "NAGIOS",
      "category": "Nagios Server",
      "component": "NAGIOS_SERVER"
    },
    {
      "id": "puppet var",
      "name": "nagios_keytab_path",
      "displayName": " Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/nagios.service.keytab",
      "description": "Path to the Nagios server keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "NAGIOS",
      "category": "Nagios Server",
      "component": "NAGIOS_SERVER"
    }

  ]
};