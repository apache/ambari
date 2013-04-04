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
      "defaultValue": "true",
      "description": "Enable kerberos security for the cluster",
      "isVisible": false,
      "isOverrideable": false,
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
      "isOverrideable": false,
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
      "isOverrideable": false,
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
      "isOverrideable": false,
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
      "isOverrideable": false,
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
      "isVisible": true,
      "isOverrideable": false,
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
      "description": "Primary name for NameNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "id": "puppet var",
      "name": "namenode_keytab",
      "displayName": "Path to Keytab File",
      "value": "",
      "defaultValue": "/etc/security/keytabs/nn.service.keytab",
      "description": "Keytab for NameNode",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_primary_name",
      "displayName": "HTTP Primary name",
      "value": "",
      "defaultValue": "HTTP",
      "isReconfigurable": false,
      "description": "Primary name for spnego access for NameNode",
      "displayType": "principal",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HDFS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_keytab",
      "displayName": "Path to HTTP keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Keytab for http NameNode and SNameNode",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HDFS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "snamenode_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/sn.service.keytab",
      "description": "path to SecondaryNameNode keytab file",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HDFS",
      "category": "SNameNode"
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
      "isOverrideable": false,
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
      "isOverrideable": false,
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
      "isOverrideable": false,
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
      "isOverrideable": false,
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
      "isOverrideable": false,
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker"
    },
    {
      "id": "puppet var",
      "name": "tasktracker_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/tt.service.keytab",
      "description": "keytab for TaskTracker",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
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
      "description": "Primary name for HBase master",
      "displayType": "principal",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HBASE",
      "category": "HBase Master"
    },
    {
      "id": "puppet var",
      "name": "hbase_master_keytab",
      "displayName": "Path to Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/hm.service.keytab",
      "description": "keytab for HBase master",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HBASE",
      "category": "HBase Master"
    },
    {
      "id": "puppet var",
      "name": "regionserver_keytab",
      "displayName": "Path to Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/rs.service.keytab",
      "description": "keytab for RegionServer",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HBASE",
      "category": "RegionServer"
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
      "isOverrideable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "id": "puppet var",
      "name": "hive_metastore__keytab",
      "displayName": "Path to Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/hive.service.keytab",
      "description": "keytab for Hive Metastore",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "HIVE",
      "category": "Hive Metastore"

    },

    //OOZIE
    {
      "id": "puppet var",
      "name": "oozie_server_name",
      "displayName": "Oozie server host",
      "value": "",
      "defaultValue": "",
      "description": "Oozie server host",
      "displayType": "masterHosts",
      "isVisible": false,
      "isOverrideable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "oozie",
      "description": "Primary name for Oozie server",
      "displayType": "principal",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_keytab",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/oozie.service.keytab",
      "description": "Keytab for Oozie server",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_http_primary_name",
      "displayName": "HTTP Primary name",
      "value": "",
      "defaultValue": "HTTP",
      "description": "Primary name for spnego access for Oozie server",
      "isReconfigurable": false,
      "displayType": "principal",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_http_keytab",
      "displayName": "Path to HTTP Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Keytab for http Oozie server",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },


    //WEBHCAT
    {
      "id": "puppet var",
      "name": "webhcat_http_primary_name",
      "displayName": "HTTP Primary name",
      "value": "",
      "defaultValue": "HTTP",
      "description": "Primary name for spnego access for webHCat",
      "displayType": "principal",
      "isReconfigurable": false,
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "WEBHCAT",
      "category": "WebHCat"
    },
    {
      "id": "puppet var",
      "name": "webhcat_http_keytab",
      "displayName": "Path to HTTP Keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/spnego.service.keytab",
      "description": "Keytab for http webHCat",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "WEBHCAT",
      "category": "WebHCat"
    },
    //HUE


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
      "isOverrideable": false,
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
      "isOverrideable": false,
      "serviceName": "NAGIOS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "nagios_keytab",
      "displayName": " Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs",
      "description": "Keytab for nagios",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "NAGIOS",
      "category": "General"
    },

    //ZooKeeper
    {
      "id": "puppet var",
      "name": "zookeeper_primary_name",
      "displayName": "Primary name",
      "value": "",
      "defaultValue": "zk",
      "description": "Primary name for ZooKeeper",
      "displayType": "principal",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    },
    {
      "id": "puppet var",
      "name": "zookeeper_keytab_path",
      "displayName": "Path to keytab file",
      "value": "",
      "defaultValue": "/etc/security/keytabs/zk.service.keytab",
      "description": "Keytab for ZooKeeper",
      "displayType": "directory",
      "isVisible": true,
      "isOverrideable": false,
      "serviceName": "ZOOKEEPER",
      "category": "ZooKeeper Server"
    }

  ]
};