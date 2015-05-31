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

var hdp22SepcificProperties = [
  require('data/HDP2.2/yarn_properties'),
  require('data/HDP2.2/tez_properties'),
  require('data/HDP2.2/hive_properties')
].reduce(function(p, c) { return c.concat(p); });

var hdp2properties = require('data/HDP2/site_properties').configProperties;
var excludedConfigs = [
  'storm.thrift.transport', //In HDP2.2 storm.thrift.transport property is computed on server
  'storm_rest_api_host',
  'tez.am.container.session.delay-allocation-millis',
  'tez.am.grouping.max-size',
  'tez.am.grouping.min-size',
  'tez.am.grouping.split-waves',
  'tez.am.java.opts',
  'tez.runtime.intermediate-input.compress.codec',
  'tez.runtime.intermediate-input.is-compressed',
  'tez.runtime.intermediate-output.compress.codec',
  'tez.runtime.intermediate-output.should-compress',
  'dfs.datanode.data.dir',
  'hive.heapsize'
];
var hdp22properties = hdp2properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp22properties.push(
  {
    "id": "site property",
    "name": "hive.zookeeper.quorum",
    "displayName": "hive.zookeeper.quorum",
    "recommendedValue": "localhost:2181",
    "displayType": "multiLine",
    "isRequired": false,
    "isVisible": true,
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "category": "Advanced hive-site"
  },
  {
    "id": "site property",
    "name": "hadoop.registry.rm.enabled",
    "displayName": "hadoop.registry.rm.enabled",
    "recommendedValue": "false",
    "displayType": "checkbox",
    "isVisible": true,
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "Advanced yarn-site"
  },
  {
    "id": "site property",
    "name": "hadoop.registry.zk.quorum",
    "displayName": "hadoop.registry.zk.quorum",
    "recommendedValue": "localhost:2181",
    "isVisible": true,
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "Advanced yarn-site"
  },
  {
    "id": "site property",
    "name": "yarn.timeline-service.leveldb-state-store.path",
    "displayName": "yarn.timeline-service.leveldb-state-store.path",
    "defaultDirectory": "/hadoop/yarn/timeline",
    "category": "APP_TIMELINE_SERVER",
    "displayType": "directory",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "id": "site property",
    "name": "yarn.timeline-service.state-store-class",
    "displayName": "yarn.timeline-service.state-store-class",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "id": "site property",
    "name": "dfs.datanode.data.dir",
    "displayName": "DataNode directories",
    "defaultDirectory": "/hadoop/hdfs/data",
    "displayType": "datanodedirs",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "index": 1
  },
  {
    "id": "site property",
    "name": "hadoop.security.key.provider.path",
    "displayName": "hadoop.security.key.provider.path",
    "defaultValue": "",
    "isRequired": false,
    "category": "Advanced core-site",
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "id": "site property",
    "name": "dfs.encryption.key.provider.uri",
    "displayName": "dfs.encryption.key.provider.uri",
    "defaultValue": "",
    "isRequired": false,
    "category": "Advanced hdfs-site",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml"
  },
  {
    "id": "site property",
    "name": "*.falcon.graph.blueprints.graph",
    "displayName": "*.falcon.graph.blueprints.graph",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "id": "site property",
    "name": "*.falcon.graph.storage.backend",
    "displayName": "*.falcon.graph.storage.backend",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  /*********RANGER FOR HDFS************/
  {
    "id": "site property",
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "ranger-hdfs-plugin-enabled",
    "displayType": "checkbox",
    "displayName": "Enable Ranger for HDFS",
    "isOverridable": false,
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS",
    "index": 1
  },
  {
    "id": "site property",
    "name": "policy_user",
    "value": "ambari-qa",
    "recommendedValue": "ambari-qa",
    "displayName": "policy User for HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "REPOSITORY_CONFIG_USERNAME",
    "displayName": "Ranger repository config user",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  /*********RANGER FOR HIVE************/
  {
    "id": "site property",
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "policy_user",
    "value": "ambari-qa",
    "recommendedValue": "ambari-qa",
    "displayName": "policy User for HIVE",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "REPOSITORY_CONFIG_USERNAME",
    "displayName": "Ranger repository config user",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "UPDATE_XAPOLICIES_ON_GRANT_REVOKE",
    "recommendedValue": true,
    "displayType": "checkbox",
    "displayName": "Should Hive GRANT/REVOKE update XA policies?",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  /*********RANGER FOR HBASE************/
  {
    "id": "site property",
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "ranger-hbase-plugin-enabled",
    "displayType": "checkbox",
    "displayName": "Enable Ranger for HBASE",
    "isOverridable": false,
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE",
    "index": 1
  },
  {
    "id": "site property",
    "name": "policy_user",
    "value": "ambari-qa",
    "recommendedValue": "ambari-qa",
    "displayName": "policy User for HBASE",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "REPOSITORY_CONFIG_USERNAME",
    "displayName": "Ranger repository config user",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "UPDATE_XAPOLICIES_ON_GRANT_REVOKE",
    "recommendedValue": true,
    "displayName": "Should HBase GRANT/REVOKE update XA policies?",
    "displayType": "checkbox",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  /*********RANGER FOR STORM************/
  {
    "id": "site property",
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "ranger-storm-plugin-enabled",
    "displayType": "checkbox",
    "displayName": "Enable Ranger for STORM",
    "isOverridable": false,
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM",
    "index": 1
  },
  {
    "id": "site property",
    "name": "policy_user",
    "value": "ambari-qa",
    "recommendedValue": "ambari-qa",
    "displayName": "policy User for STORM",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "REPOSITORY_CONFIG_USERNAME",
    "displayName": "Ranger repository config user",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  /*********RANGER FOR KNOX************/
  {
    "id": "site property",
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "ranger-knox-plugin-enabled",
    "displayType": "checkbox",
    "displayName": "Enable Ranger for KNOX",
    "isOverridable": false,
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX",
    "index": 1
  },
  {
    "id": "site property",
    "name": "policy_user",
    "value": "ambari-qa",
    "recommendedValue": "ambari-qa",
    "displayName": "policy User for KNOX",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "REPOSITORY_CONFIG_USERNAME",
    "displayName": "Ranger repository config user",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  /**********************************************SPARK***************************************/
  {
    "id": "site property",
    "name": "spark.driver.extraJavaOptions",
    "displayName": "spark.driver.extraJavaOptions",
    "recommendedValue": "",
    "isRequired": false,
    "category": "Advanced spark-defaults",
    "serviceName": "SPARK",
    "filename": "spark-defaults.xml"
  },
  {
    "id": "site property",
    "name": "spark.yarn.am.extraJavaOptions",
    "displayName": "spark.yarn.am.extraJavaOptions",
    "recommendedValue": "",
    "isRequired": false,
    "category": "Advanced spark-defaults",
    "serviceName": "SPARK",
    "filename": "spark-defaults.xml"
  },
  /**********************************************RANGER***************************************/
  {
    "id": "site property",
    "name": "ranger_admin_password",
    "displayName": "Ranger Admin user's password for Ambari",
    "recommendedValue": "ambari",
    "isReconfigurable": true,
    "displayType": "password",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "AdminSettings"
  },
  {
    "id": "site property",
    "name": "SQL_CONNECTOR_JAR",
    "displayName": "Location of Sql Connector Jar",
    "recommendedValue": "/usr/share/java/mysql-connector-java.jar",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "AdminSettings"
  },
  {
    "id": "site property",
    "name": "DB_FLAVOR",
    "displayName": "DB FLAVOR",
    "value": "MYSQL",
    "recommendedValue": "MYSQL",
    "isReconfigurable": true,
    "options": [
      {
        displayName: 'MYSQL'
      },
      {
        displayName: 'ORACLE'
      }
    ],
    "displayType": "radio button",
    "radioName": "RANGER DB_FLAVOR",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 1
  },
  {
    "id": "site property",
    "name": "SQL_COMMAND_INVOKER",
    "displayName": "SQL Command Invoker",
    "recommendedValue": "mysql",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings"
  },
  {
    "id": "site property",
    "name": "db_host",
    "displayName": "Ranger DB host",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 2
  },
  {
    "id": "site property",
    "name": "db_root_user",
    "displayName": "Ranger DB root user",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 5
  },
  {
    "id": "site property",
    "name": "db_root_password",
    "displayName": "Ranger DB root password",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "password",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 6
  },
  {
    "id": "site property",
    "name": "db_name",
    "displayName": "Ranger DB name",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 7
  },

  {
    "id": "site property",
    "name": "db_user",
    "displayName": "Ranger DB username",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 3
  },
  {
    "id": "site property",
    "name": "db_password",
    "displayName": "Ranger DB password",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "password",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 4
  },
  {
    "id": "site property",
    "name": "audit_db_name",
    "displayName": "Ranger Audit DB name",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 11
  },
  {
    "id": "site property",
    "name": "audit_db_user",
    "displayName": "Ranger Audit DB username",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 12
  },
  {
    "id": "site property",
    "name": "audit_db_password",
    "displayName": "Ranger Audit DB password",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "password",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 13
  },
  {
    "id": "puppet var",
    "name": "policymgr_external_url",
    "displayName": "External URL",
    "recommendedValue": "http://localhost:6080",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "id": "puppet var",
    "name": "policymgr_http_enabled",
    "displayName": "HTTP enabled",
    "recommendedValue": true,
    "isReconfigurable": true,
    "displayType": "checkbox",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "id": "puppet var",
    "name": "ranger_user",
    "displayName": "Used to create user and assign permission",
    "recommendedValue": "ranger",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "RangerSettings"
  },
  {
    "id": "puppet var",
    "name": "ranger_group",
    "displayName": "Used to create group and assign permission",
    "recommendedValue": "ranger",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "RangerSettings"
  },
  {
    "id": "site property",
    "name": "authentication_method",
    "displayName": "Authentication method",
    "recommendedValue": "NONE",
    "options": [
      {
        displayName: 'LDAP',
        foreignKeys: ['xa_ldap_userDNpattern', 'xa_ldap_groupRoleAttribute', 'xa_ldap_url', 'xa_ldap_groupSearchBase', 'xa_ldap_groupSearchFilter']
      },
      {
        displayName: 'ACTIVE_DIRECTORY',
        foreignKeys: ['xa_ldap_ad_domain', 'xa_ldap_ad_url']
      },
      {
        displayName: 'UNIX',
        foreignKeys: ['remoteLoginEnabled', 'authServiceHostName', 'authServicePort']
      },
      {
        displayName: 'NONE'
      }
    ],
    "displayType": "radio button",
    "radioName": "authentication-method",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "id": "site property",
    "name": "remoteLoginEnabled",
    "displayName": "Allow remote Login",
    "recommendedValue": true,
    "isReconfigurable": true,
    "displayType": "checkbox",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "id": "site property",
    "name": "authServiceHostName",
    "displayName": "authServiceHostName",
    "recommendedValue": 'localhost',
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "id": "site property",
    "name": "authServicePort",
    "displayName": "authServicePort",
    "recommendedValue": '5151',
    "isReconfigurable": true,
    "displayType": "int",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "id": "site property",
    "name": "xa_ldap_url",
    "displayName": "xa_ldap_url",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "LDAPSettings"
  },
  {
    "id": "site property",
    "name": "xa_ldap_userDNpattern",
    "displayName": "xa_ldap_userDNpattern",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "LDAPSettings"
  },
  {
    "id": "site property",
    "name": "xa_ldap_groupRoleAttribute",
    "displayName": "xa_ldap_groupRoleAttribute",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "LDAPSettings"
  },
  {
    "id": "site property",
    "name": "xa_ldap_ad_domain",
    "displayName": "xa_ldap_ad_domain",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "ADSettings"
  },
  {
    "id": "site property",
    "name": "xa_ldap_ad_url",
    "displayName": "xa_ldap_ad_url",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "ADSettings"
  },
  {
    "id": "site property",
    "name": "common.name.for.certificate",
    "displayName": "common.name.for.certificate",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "isRequired": false,
    "serviceName": "HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml"
  },
  {
    "id": "site property",
    "name": "hadoop.rpc.protection",
    "displayName": "hadoop.rpc.protection",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "isRequired": false,
    "serviceName": "HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml"
  },  
  {
    "id": "site property",
    "name": "common.name.for.certificate",
    "displayName": "common.name.for.certificate",
    "category": "Advanced ranger-hive-plugin-properties",
    "isRequired": false,
    "serviceName": "HIVE",
    "filename": "ranger-hive-plugin-properties.xml"
  },
  {
    "id": "site property",
    "name": "common.name.for.certificate",
    "displayName": "common.name.for.certificate",
    "category": "Advanced ranger-hbase-plugin-properties",
    "isRequired": false,
    "serviceName": "HBASE",
    "filename": "ranger-hbase-plugin-properties.xml"
  },
  {
    "id": "site property",
    "name": "common.name.for.certificate",
    "displayName": "common.name.for.certificate",
    "category": "Advanced ranger-knox-plugin-properties",
    "isRequired": false,
    "serviceName": "KNOX",
    "filename": "ranger-knox-plugin-properties.xml"
  },
  {
    "id": "site property",
    "name": "common.name.for.certificate",
    "displayName": "common.name.for.certificate",
    "category": "Advanced ranger-storm-plugin-properties",
    "isRequired": false,
    "serviceName": "STORM",
    "filename": "ranger-storm-plugin-properties.xml"
  },
  {
    "id": "site property",
    "name": "SYNC_LDAP_USER_SEARCH_FILTER",
    "displayName": "SYNC_LDAP_USER_SEARCH_FILTER",
    "category": "Advanced usersync-properties",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "usersync-properties.xml"
  },
  {
    "id": "site property",
    "name": "hbase.bucketcache.ioengine",
    "displayName": "hbase.bucketcache.ioengine",
    "isRequired": false,
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "id": "site property",
    "name": "hbase.bucketcache.size",
    "displayName": "hbase.bucketcache.size",
    "displayType": "int",
    "isRequired": false,
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "id": "site property",
    "name": "hbase.bucketcache.percentage.in.combinedcache",
    "displayName": "hbase.bucketcache.percentage.in.combinedcache",
    "displayType": "float",
    "isRequired": false,
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "id": "site property",
    "name": "hbase_max_direct_memory_size",
    "displayName": "HBase off-heap MaxDirectMemorySize",
    "displayType": "int",
    "isRequired": false,
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "Advanced hbase-env"
  },
  {
    "id": "site property",
    "name": "hbase.regionserver.global.memstore.size",
    "displayName": "hbase.regionserver.global.memstore.size",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
  "id": "site property",
    "name": "hbase.region.server.rpc.scheduler.factory.class",
    "displayName": "hbase.region.server.rpc.scheduler.factory.class",
    "defaultValue": "",
    "isRequired": false,
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "id": "site property",
    "name": "hbase.rpc.controllerfactory.class",
    "displayName": "hbase.rpc.controllerfactory.class",
    "defaultValue": "",
    "isRequired": false,
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  }
);

var additionalProperties = [];

hdp22SepcificProperties.forEach(function (config) {
  if (!hdp22properties.findProperty('name', config.name)) additionalProperties.push(config);
  else {
    hdp22properties.findProperty('name', config.name).category = config.category;
  }
});

module.exports =
{
  "configProperties": hdp22properties.concat(additionalProperties)
};
