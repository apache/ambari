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
    "filename": "core-site.xml"
  },
  {
    "name": "hadoop.security.authorization",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "core-site.xml"
  },

  {
    "name": "hadoop.security.auth_to_local",
    "templateName": ["jobtracker_primary_name", "kerberos_domain", "mapred_user", "tasktracker_primary_name", "namenode_primary_name", "hdfs_user", "datanode_primary_name", "hbase_primary_name", "hbase_user"],
    "foreignKey": null,
    "value": "RULE:[2:$1@$0](<templateName[0]>@.*<templateName[1]>)s/.*/<templateName[2]>/ RULE:[2:$1@$0](<templateName[3]>@.*<templateName[1]>)s/.*/<templateName[2]>/ RULE:[2:$1@$0](<templateName[4]>@.*<templateName[1]>)s/.*/<templateName[5]>/ RULE:[2:$1@$0](<templateName[6]>@.*<templateName[1]>)s/.*/<templateName[5]>/ RULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/ DEFAULT",
    "filename": "core-site.xml"
  },
  {
    "name": "dfs.namenode.kerberos.principal",
    "templateName": ["namenode_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.namenode.keytab.file",
    "templateName": ["namenode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.principal",
    "templateName": ["namenode_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.secondary.namenode.keytab.file",
    "templateName": ["snamenode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.web.authentication.kerberos.principal",
    "templateName": ["hadoop_http_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.web.authentication.kerberos.keytab",
    "templateName": ["hadoop_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.kerberos.principal",
    "templateName": ["datanode_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.keytab.file",
    "templateName": ["datanode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.namenode.kerberos.internal.spnego.principal",
    "templateName": [],
    "foreignKey": null,
    "value": "${dfs.web.authentication.kerberos.principal}",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.internal.spnego.principal",
    "templateName": [],
    "foreignKey": null,
    "value": "${dfs.web.authentication.kerberos.principal}",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.address",
    "templateName": [],
    "foreignKey": null,
    "value": "0.0.0.0:1019",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "dfs.datanode.http.address",
    "templateName": [],
    "foreignKey": null,
    "value": "0.0.0.0:1022",
    "filename": "hdfs-site.xml"
  },
  {
    "name": "mapreduce.jobtracker.kerberos.principal",
    "templateName": ["jobtracker_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.jobtracker.keytab.file",
    "templateName": ["jobtracker_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.tasktracker.kerberos.principal",
    "templateName": ["tasktracker_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "mapreduce.tasktracker.keytab.file",
    "templateName": ["tasktracker_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml"
  },
  {
    "name": "hbase.master.kerberos.principal",
    "templateName": ["hbase_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.master.keytab.file",
    "templateName": ["hbase_master_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.regionserver.kerberos.principal",
    "templateName": ["hbase_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.regionserver.keytab.file",
    "templateName": ["regionserver_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hive.metastore.sasl.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.server2.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "KERBEROS",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.metastore.kerberos.principal",
    "templateName": ["hive_metastore_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.metastore.kerberos.keytab.file",
    "templateName": ["hive_metastore__keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.server2.authentication.kerberos.principal",
    "templateName": ["hive_metastore_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hive-site.xml"
  },
  {
    "name": "hive.server2.authentication.kerberos.keytab",
    "templateName": ["hive_metastore__keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml"
  },
  {
    "name": "oozie.service.AuthorizationService.authorization.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.HadoopAccessorService.kerberos.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "oozie-site.xml"
  },
  {
    "name": "local.realm",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.HadoopAccessorService.keytab.file",
    "templateName": ["oozie_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.service.HadoopAccessorService.kerberos.principal",
    "templateName": ["oozie_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.authentication.type",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.authentication.kerberos.principal",
    "templateName": ["oozie_http_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.authentication.kerberos.keytab",
    "templateName": ["oozie_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml"
  },
  {
    "name": "oozie.authentication.kerberos.name.rules",
    "templateName": ["jobtracker_primary_name", "kerberos_domain", "mapred_user", "tasktracker_primary_name", "namenode_primary_name", "hdfs_user", "datanode_primary_name", "hbase_primary_name", "hbase_user"],
    "foreignKey": null,
    "value": "RULE:[2:$1@$0](<templateName[0]>@.*<templateName[1]>)s/.*/<templateName[2]>/ RULE:[2:$1@$0](<templateName[3]>@.*<templateName[1]>)s/.*/<templateName[2]>/ RULE:[2:$1@$0](<templateName[4]>@.*<templateName[1]>)s/.*/<templateName[5]>/ RULE:[2:$1@$0](<templateName[6]>@.*<templateName[1]>)s/.*/<templateName[5]>/ RULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/ DEFAULT",
    "filename": "oozie-site.xml"
  },
  {
    "name": "templeton.kerberos.principal",
    "templateName": ["webhcat_http_primary_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "webhcat-site.xml"
  },
  {
    "name": "templeton.kerberos.keytab",
    "templateName": ["webhcat_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "webhcat-site.xml"
  },
  {
    "name": "templeton.kerberos.secret",
    "templateName": [""],
    "foreignKey": null,
    "value": "secret",
    "filename": "webhcat-site.xml"
  },
  {
    "name": "templeton.kerberos.properties",
    "templateName": ["hive_user"],
    "foreignKey": null,
    "value": "hive.metastore.local=false, hive.metastore.uris=thrift://MetastoreHost_FQDN:9083, hive.q" +
      "metastore.sasl.enabled=true,hive.metastore.execute.setugi= true, hive.exec.mode.local.auto=false, hive.metastore.kerberos.principal=<templateName[0]>/_HOST@EXAMPLE.COM",
    "filename": "webhcat-site.xml"
  },
  {
    "name": "hbase.coprocessor.master.classes",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.security.access.AccessController",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.coprocessor.region.classes",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.hadoop.hbase.security.access.AccessController",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.security.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.rpc.engine",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.ipc.SecureRpcEngine",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.security.authorization",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.coprocessor.region.classes",
    "templateName": [],
    "foreignKey": null,
    "value": "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.hadoop.hbase.security.access.AccessController",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.bulkload.staging.dir",
    "templateName": [],
    "foreignKey": null,
    "value": "/apps/hbase/staging",
    "filename": "hbase-site.xml"
  },
  {
    "name": "zookeeper.znode.parent",
    "templateName": [],
    "foreignKey": null,
    "value": "/hbase-secure",
    "filename": "hbase-site.xml"
  }
];

