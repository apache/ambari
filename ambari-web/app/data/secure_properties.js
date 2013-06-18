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
      "name": "kinit_path_local",
      "displayName": "Path to kinit",
      "value": "",
      "defaultValue": "/usr/bin/kinit",
      "description": "Path to installed kinit command",
      "displayType": "principal",
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
      "name": "instance_name",
      "displayName": "Use Instance name",
      "value": "",
      "defaultValue": true,
      "description": "Whether to use instance name for creating principals across cluster",
      "displayType": "checkbox",
      "isVisible": false,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_primary_name",
      "displayName": "HTTP Primary name",
      "value": "",
      "defaultValue": "HTTP",
      "isReconfigurable": false,
      "description": "Primary name for spnego access for NameNode, SNameNode, Oozie and WebHCat",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_keytab",
      "displayName": "Path to HTTP keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Path to HTTP keytab file for NameNode, SNameNode, Oozie and WebHCat",
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
      "name": "namenode_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "nn",
      "description": "Primary name for NameNode and SNameNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "NameNode"
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
      "category": "NameNode"
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
      "name": "datanode_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "dn",
      "description": "Primary name for DataNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HDFS",
      "category": "DataNode"
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
      "category": "DataNode"
    },
    //MAPREDUCE
    {
      "id": "puppet var",
      "name": "jobtracker_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "jt",
      "description": "Primary name for JobTracker",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker"
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
      "category": "JobTracker"
    },
    {
      "id": "puppet var",
      "name": "tasktracker_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "tt",
      "description": "Primary name for TaskTracker",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker"
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
      "category": "TaskTracker"
    },

    //HBASE
    {
      "id": "puppet var",
      "name": "hbase_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "hbase",
      "description": "Primary name for HBase master and RegionServer",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HBASE",
      "category": "HBase"
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
      "category": "HBase"
    },

    //HIVE
    {
      "id": "puppet var",
      "name": "hive_metastore_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "hive",
      "description": "Primary name for Hive Metastore",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore"
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
      "category": "Hive Metastore"

    },

    //OOZIE
    {
      "id": "puppet var",
      "name": "oozie_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "oozie",
      "description": "Primary name for Oozie server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
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
      "category": "Oozie Server"
    },

    //NAGIOS
    {
      "id": "puppet var",
      "name": "nagios_server_name",
      "displayName": "Nagios server host",
      "value": "",
      "defaultValue": "",
      "description": "Nagios server host",
      "displayType": "masterHosts",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "NAGIOS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "nagios_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "nagios",
      "description": "Primary name for Nagios server",
      "displayType": "principal",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "NAGIOS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "nagios_keytab",
      "displayName": " Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs",
      "description": "Path to the directory that contains nagios keytab",
      "displayType": "directory",
      "isVisible": true,
      "isOverridable": false,
      "serviceName": "NAGIOS",
      "category": "General"
    },

    //ZooKeeper
    {
      "id": "puppet var",
      "name": "zookeeper_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "zookeeper",
      "description": "Primary name for ZooKeeper",
      "displayType": "principal",
      "isVisible": true,
      "isReconfigurable": false,
      "isOverridable": false,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
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
      "category": "ZooKeeper Server"
    }

  ]
};