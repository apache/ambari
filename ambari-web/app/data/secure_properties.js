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
    //GENERAL
    {
      "id": "puppet var",
      "name": "security_enabled",
      "displayName": "Enable security",
      "value": "",
      "defaultValue": "true",
      "description": "Enable kerberos security for the cluster",
      "displayType": "principal",
      "isVisible": false,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "realm_name",
      "displayName": "Realm name",
      "value": "",
      "defaultValue": "EXAMPLE",
      "description": "Realm name to be used for all principal names",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    {
      "id": "puppet var",
      "name": "instance_name",
      "displayName": "Instance name",
      "value": "",
      "defaultValue": "EXAMPLE",
      "description": "Whether to use instance name for creating principals across cluster",
      "displayType": "checkbox",
      "isVisible": true,
      "serviceName": "GENERAL",
      "category": "KERBEROS"
    },
    //HDFS
    {
      "id": "puppet var",
      "name": "namenode_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "nn",
      "description": "Primary name for NameNode",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "id": "puppet var",
      "name": "namenode_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for NameNode",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "NameNode"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_primary_name",
      "displayName": "HTTP primary name",
      "value": "",
      "defaultValue": "HTTP",
      "isReconfigurable": false,
      "description": "Primary name for spnego access for NameNode",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "hadoop_http_keytab",
      "displayName": "HTTP Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for http NameNode and SNameNode",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "snamenode_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "sn",
      "description": "Primary name for SecondaryNameNode",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "SNameNode"
    },
    {
      "id": "puppet var",
      "name": "snamenode_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for SecondaryNameNode",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "SNameNode"
    },
    {
      "id": "puppet var",
      "name": "datanode_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "dn",
      "description": "Primary name for DataNode",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    {
      "id": "puppet var",
      "name": "datanode_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for DataNode",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "HDFS",
      "category": "DataNode"
    },
    //MAPREDUCE
    {
      "id": "puppet var",
      "name": "jobtracker_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "jt",
      "description": "Primary name for JobTracker",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker"
    },
    {
      "id": "puppet var",
      "name": "jobtracker_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "keytab for JobTracker",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "JobTracker"
    },
    {
      "id": "puppet var",
      "name": "tasktracker_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "tt",
      "description": "Primary name for TaskTracker",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker"
    },
    {
      "id": "puppet var",
      "name": "tasktracker_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "keytab for TaskTracker",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "MAPREDUCE",
      "category": "TaskTracker"
    },

    //HBASE
    {
      "id": "puppet var",
      "name": "hbase_master_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "hm",
      "description": "Primary name for HBase master",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "HBase Master"
    },
    {
      "id": "puppet var",
      "name": "hbase_master_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "keytab for HBase master",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "HBase Master"
    },
    {
      "id": "puppet var",
      "name": "regionserver_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "rs",
      "description": "Primary name for regionServer",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "RegionServer"
    },
    {
      "id": "puppet var",
      "name": "regionserver_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "keytab for RegionServer",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "HBASE",
      "category": "RegionServer"
    },

    //HIVE
    {
      "id": "puppet var",
      "name": "hive_metastore_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "hive",
      "description": "Primary name for Hive Metastore",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore"
    },
    {
      "id": "puppet var",
      "name": "hive_metastore__keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "keytab for Hive Metastore",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Hive Metastore"

    },

    //OOZIE
    {
      "id": "puppet var",
      "name": "oozie_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "oozie",
      "description": "Primary name for Oozie server",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for Oozie server",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_http_primary_name",
      "displayName": "HTTP primary name",
      "value": "",
      "defaultValue": "HTTP",
      "description": "Primary name for spnego access for Oozie server",
      "isReconfigurable": false,
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },
    {
      "id": "puppet var",
      "name": "oozie_http_keytab",
      "displayName": "HTTP Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for http Oozie server",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "OOZIE",
      "category": "Oozie Server"
    },


    //WEBHCAT
    {
      "id": "puppet var",
      "name": "webhcat_http_primary_name",
      "displayName": "HTTP primary name",
      "value": "",
      "defaultValue": "HTTP",
      "description": "Primary name for spnego access for webHCat",
      "displayType": "principal",
      "isReconfigurable": false,
      "isVisible": true,
      "serviceName": "WEBHCAT",
      "category": "WebHCat"
    },
    {
      "id": "puppet var",
      "name": "webhcat_http_keytab",
      "displayName": "HTTP Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for http webHCat",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "WEBHCAT",
      "category": "WebHCat"
    },
    //HUE

    //NAGIOS
    {
      "id": "puppet var",
      "name": "nagios_primary_name",
      "displayName": "primary name",
      "value": "",
      "defaultValue": "nagios",
      "description": "Primary name for Nagios server",
      "displayType": "principal",
      "isVisible": true,
      "serviceName": "NAGIOS",
      "category": "General"
    },
    {
      "id": "puppet var",
      "name": "nagios_keytab",
      "displayName": "Keytab Path",
      "value": "",
      "defaultValue": "",
      "description": "Keytab for nagios",
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "NAGIOS",
      "category": "General"
    }

  ]
};