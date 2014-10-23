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

module.exports = [
  {
    "name": "hadoop.security.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "core-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "hadoop.security.authorization",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "core-site.xml",
    "serviceName": "HDFS"
  },

  {
    "name": "hadoop.security.auth_to_local",
    "templateName": ["jobtracker_primary_name", "kerberos_domain", "mapred_user", "tasktracker_primary_name", "namenode_primary_name", "hdfs_user", "datanode_primary_name", "hbase_master_primary_name", "hbase_user","hbase_regionserver_primary_name","oozie_primary_name","oozie_user","jobhistory_primary_name"],
    "foreignKey": null,
    "value": "RULE:[2:$1@$0](<templateName[0]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[3]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[4]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[6]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[10]>@.*<templateName[1]>)s/.*/<templateName[11]>/\nRULE:[2:$1@$0](<templateName[12]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nDEFAULT",
    "filename": "core-site.xml",
    "serviceName": "HDFS",
    "dependedServiceName": [{name: "HBASE", replace: "\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/"},{name: "OOZIE",replace: "\nRULE:[2:$1@$0](<templateName[10]>@.*<templateName[1]>)s/.*/<templateName[11]>/"}]
  },
  {
    "name": "dfs.namenode.kerberos.principal",
    "templateName": ["namenode_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.namenode.keytab.file",
    "templateName": ["namenode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.principal",
    "templateName": ["snamenode_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.secondary.namenode.keytab.file",
    "templateName": ["snamenode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.web.authentication.kerberos.principal",
    "templateName": ["hadoop_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.web.authentication.kerberos.keytab",
    "templateName": ["hadoop_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.kerberos.principal",
    "templateName": ["datanode_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.keytab.file",
    "templateName": ["datanode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.namenode.kerberos.internal.spnego.principal",
    "templateName": [],
    "foreignKey": null,
    "value": "${dfs.web.authentication.kerberos.principal}",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.internal.spnego.principal",
    "templateName": [],
    "foreignKey": null,
    "value": "${dfs.web.authentication.kerberos.principal}",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.address",
    "templateName": ["dfs_datanode_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "nonSecureValue": "0.0.0.0:50010",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.http.address",
    "templateName": ["dfs_datanode_http_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "nonSecureValue": "0.0.0.0:50075",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "mapreduce.jobtracker.kerberos.principal",
    "templateName": ["jobtracker_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE"
  },
  {
    "name": "mapreduce.jobtracker.keytab.file",
    "templateName": ["jobtracker_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE"
  },
  {
    "name": "mapreduce.jobhistory.kerberos.principal",
    "templateName": ["jobhistory_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE"
  },
  {
    "name": "mapreduce.jobhistory.keytab.file",
    "templateName": ["jobhistory_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE"
  },
  {
    "name": "mapreduce.tasktracker.kerberos.principal",
    "templateName": ["tasktracker_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE"
  },
  {
    "name": "mapreduce.tasktracker.keytab.file",
    "templateName": ["tasktracker_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE"
  },
  {
    "name": "mapred.task.tracker.task-controller",
    "templateName": ["tasktracker_task_controller"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "nonSecureValue": "org.apache.hadoop.mapred.DefaultTaskController",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE"
  },
  {
    "name": "hbase.master.kerberos.principal",
    "templateName": ["hbase_master_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.master.keytab.file",
    "templateName": ["hbase_master_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.regionserver.kerberos.principal",
    "templateName": ["hbase_regionserver_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.regionserver.keytab.file",
    "templateName": ["hbase_regionserver_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hive.metastore.sasl.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.security.authorization.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "KERBEROS",
    "nonSecureValue": "NONE",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.metastore.kerberos.principal",
    "templateName": ["hive_metastore_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.metastore.kerberos.keytab.file",
    "templateName": ["hive_metastore_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication.kerberos.principal",
    "templateName": ["hive_metastore_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication.kerberos.keytab",
    "templateName": ["hive_metastore_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "oozie.service.AuthorizationService.authorization.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.service.HadoopAccessorService.kerberos.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "local.realm",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.service.HadoopAccessorService.keytab.file",
    "templateName": ["oozie_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.service.HadoopAccessorService.kerberos.principal",
    "templateName": ["oozie_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.type",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.kerberos.principal",
    "templateName": ["oozie_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.kerberos.keytab",
    "templateName": ["oozie_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.kerberos.name.rules",
    "templateName": ["jobtracker_primary_name", "kerberos_domain", "mapred_user", "tasktracker_primary_name", "namenode_primary_name", "hdfs_user", "datanode_primary_name", "hbase_master_primary_name", "hbase_user","hbase_regionserver_primary_name", "jobhistory_primary_name"],
    "foreignKey": null,
    "value": "RULE:[2:$1@$0](<templateName[0]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[3]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[4]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[6]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[10]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nDEFAULT",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE",
    "dependedServiceName": [{name: "HBASE", replace: "\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/"}]
  },
  {
    "name": "templeton.kerberos.principal",
    "templateName": ["webHCat_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "templeton.kerberos.keytab",
    "templateName": ["webhcat_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "templeton.kerberos.secret",
    "templateName": [""],
    "foreignKey": null,
    "value": "secret",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "templeton.hive.properties",
    "templateName": ["hivemetastore_host","hive_metastore_principal_name","kerberos_domain"],
    "foreignKey": null,
    "value": "hive.metastore.local=false,hive.metastore.uris=thrift://<templateName[0]>:9083,hive." +
      "metastore.sasl.enabled=true,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse,hive.exec.mode.local.auto=false,hive.metastore.kerberos.principal=<templateName[1]>@<templateName[2]>",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hbase.coprocessor.master.classes",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.security.access.AccessController",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.coprocessor.region.classes",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.hadoop.hbase.security.access.AccessController",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.security.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.rpc.engine",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.ipc.SecureRpcEngine",
    "nonSecureValue": "org.apache.hadoop.hbase.ipc.WritableRpcEngine",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.security.authorization",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.coprocessor.region.classes",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.hadoop.hbase.security.access.AccessController",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.bulkload.staging.dir",
    "templateName": [],
    "foreignKey": null,
    "value": "/apps/hbase/staging",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "zookeeper.znode.parent",
    "templateName": [],
    "foreignKey": null,
    "value": "/hbase-secure",
    "nonSecureValue": "/hbase-unsecure",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["hive_metastore_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hivemetastore_host"],
    "foreignKey": ["hive_metastore_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["oozie_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["oozieserver_host"],
    "foreignKey": ["oozie_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["webHCat_http_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["webhcat_server"],
    "foreignKey": ["webHCat_http_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  }
];

