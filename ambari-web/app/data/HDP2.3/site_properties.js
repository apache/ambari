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

var hdp22properties = require('data/HDP2.2/site_properties').configProperties;

var excludedConfigs = [
  'DB_FLAVOR',
  'db_name',
  'db_root_user',
  'db_root_password',
  'nimbus.host',
  'XAAUDIT.DB.IS_ENABLED',
  'XAAUDIT.HDFS.IS_ENABLED',
  'UPDATE_XAPOLICIES_ON_GRANT_REVOKE',
  'authServiceHostName',
  'authServicePort',
  'authentication_method',
  'remoteLoginEnabled',
  'xa_ldap_url',
  'xa_ldap_userDNpattern',
  'xa_ldap_groupSearchBase',
  'xa_ldap_groupSearchFilter',
  'xa_ldap_groupRoleAttribute',
  'xa_ldap_ad_domain',
  'xa_ldap_ad_url',
  'policymgr_http_enabled',
  'policymgr_external_url'
];

var hdp23properties = hdp22properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp23properties.push({
    "id": "site property",
    "name": "DB_FLAVOR",
    "displayName": "DB FLAVOR",
    "value": "",
    "recommendedValue": "",
    "isReconfigurable": true,
    "options": [
      {
        displayName: 'MYSQL',
        foreignKeys: ['ranger_mysql_database', 'ranger_mysql_host']
      },
      {
        displayName: 'ORACLE',
        foreignKeys: ['ranger_oracle_database', 'ranger_oracle_host']
      },
      {
        displayName: 'POSTGRES',
        foreignKeys: ['ranger_postgres_database', 'ranger_postgres_host']
      },
      {
        displayName: 'MSSQL',
        foreignKeys: ['ranger_mssql_database', 'ranger_mssql_host']
      }
    ],
    "displayType": "radio button",
    "radioName": "RANGER DB_FLAVOR",
    "isOverridable": false,
    "isVisible": true,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 1
  },
  {
    "name": "ranger_mysql_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "recommendedValue": "MySQL",
    "description": "Using a MySQL database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 1
  },
  {
    "name": "ranger_oracle_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "recommendedValue": "ORACLE",
    "description": "Using an Oracle database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 2
  },
  {
    "name": "ranger_postgres_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "recommendedValue": "Postgres",
    "description": "Using a Postgres database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 3
  },
  {
    "name": "ranger_mssql_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "recommendedValue": "MSSQL",
    "description": "Using a MS SQL database for Ranger",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": false,
    "isReconfigurable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 4
  },
  {
    "name": "rangerserver_host",
    "id": "puppet var",
    "displayName": "Ranger Server host",
    "value": "",
    "recommendedValue": "",
    "description": "The host that has been assigned to run Ranger Server",
    "displayType": "masterHost",
    "isOverridable": false,
    "isVisible": true,
    "isRequiredByAgent": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 0
  },
  {
    "id": "site property",
    "name": "create_db_dbuser",
    "displayName": "Setup DB and DB user",
    "displayType": "checkbox",
    "filename": "ranger-env.xml",
    "category": "Advanced ranger-env",
    "serviceName": "RANGER"
  },
  /**************************************** RANGER - HDFS Plugin ***************************************/
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },        
  {
    "id": "site property",
    "name": "ranger-yarn-plugin-enabled",
    "displayType": "checkbox",
    "displayName": "Enable Ranger for YARN",
    "isOverridable": false,
    "filename": "ranger-yarn-plugin-properties.xml",
    "category": "Advanced ranger-yarn-plugin-properties",
    "serviceName": "YARN",
    "index": 1
  },
  {
    "id": "site property",
    "name": "ranger-kafka-plugin-enabled",
    "displayType": "checkbox",
    "displayName": "Enable Ranger for KAFKA",
    "isOverridable": false,
    "filename": "ranger-kafka-plugin-properties.xml",
    "category": "Advanced ranger-kafka-plugin-properties",
    "serviceName": "KAFKA",
    "index": 1
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.db",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.hdfs",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.destination.solr",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "id": "site property",
    "name": "hadoop.rpc.protection",
    "displayName": "hadoop.rpc.protection",
    "isRequired": false,
    "filename": "ranger-kafka-plugin-properties.xml",
    "category": "Advanced ranger-kafka-plugin-properties",
    "serviceName": "KAFKA"
  },
  {
    "id": "site property",
    "name": "common.name.for.certificate",
    "displayName": "common.name.for.certificate",
    "isRequired": false,
    "filename": "ranger-kafka-plugin-properties.xml",
    "category": "Advanced ranger-kafka-plugin-properties",
    "serviceName": "KAFKA"
  },
  {
    "id": "site property",
    "name": "hadoop.rpc.protection",
    "displayName": "hadoop.rpc.protection",
    "isRequired": false,
    "filename": "ranger-yarn-plugin-properties.xml",
    "category": "Advanced ranger-yarn-plugin-properties",
    "serviceName": "YARN"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.provider.summary.enabled",
    "displayName": "Audit provider summary enabled",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "name": "ranger_mysql_host",
    "id": "puppet var",
    "displayName": "MYSQL database Host",
    "description": "Specify the host on which the existing database is hosted",
    "recommendedValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 6
  },
  {
    "name": "ranger_oracle_host",
    "id": "puppet var",
    "displayName": "Oracle database Host",
    "description": "Specify the host on which the existing database is hosted",
    "recommendedValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 7
  },
  {
    "name": "ranger_postgres_host",
    "id": "puppet var",
    "displayName": "Database Host",
    "description": "Specify the host on which the existing database is hosted",
    "recommendedValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 8
  },
  {
    "name": "ranger_mssql_host",
    "id": "puppet var",
    "displayName": "Database Host",
    "description": "Specify the host on which the existing database is hosted",
    "recommendedValue": "",
    "value": "",
    "displayType": "host",
    "isOverridable": false,
    "isVisible": false,
    "isObserved": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 9
  },
  {
    "name": "ranger.jpa.jdbc.url",
    "id": "site property",
    "displayName": "JDBC connect string for a Ranger database",
    "recommendedValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isObserved": true,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "DBSettings",
    "index": 9
  },
  {
    "name": "ranger.jpa.jdbc.driver",
    "id": "site property",
    "displayName": "Driver class name for a JDBC Ranger database",
    "recommendedValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "DBSettings",
    "index": 8
  },
  {
    "name": "db_root_user",
    "id": "site property",
    "displayName": "Ranger DB root user",
    "recommendedValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 5
  },
  {
    "name": "db_root_password",
    "id": "site property",
    "displayName": "Ranger DB root password",
    "recommendedValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "password",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 6
  },
  {
    "name": "db_name",
    "id": "site property",
    "displayName": "Ranger DB name",
    "recommendedValue": "",
    "isReconfigurable": true,
    "isObserved": true,
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
    "name": "tez.am.view-acls",
    "displayName": "tez.am.view-acls",
    "isRequired": false,
    "serviceName": "TEZ",
    "filename": "tez-site.xml",
    "category": "Advanced tez-site"
  },
  {
    "id": "puppet var",
    "name": "ranger.external.url",
    "displayName": "External URL",
    "recommendedValue": "http://localhost:6080",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
  },
  {
    "id": "puppet var",
    "name": "ranger.externalurl",
    "displayName": "External URL",
    "recommendedValue": "http://localhost:6080",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": false,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
  },
  {
    "id": "puppet var",
    "name": "ranger.service.http.enabled",
    "displayName": "HTTP enabled",
    "recommendedValue": true,
    "isReconfigurable": true,
    "displayType": "checkbox",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
  },
  {
    "id": "site property",
    "name": "ranger.authentication.method",
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
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
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
    "id": "site property",
    "name": "ranger.unixauth.remote.login.enabled",
    "displayName": "Allow remote Login",
    "recommendedValue": true,
    "isReconfigurable": true,
    "displayType": "checkbox",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "id": "site property",
    "name": "ranger.unixauth.service.hostname",
    "displayName": "ranger.unixauth.service.hostname",
    "recommendedValue": 'localhost',
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "id": "site property",
    "name": "ranger.unixauth.service.port",
    "displayName": "ranger.unixauth.service.port",
    "recommendedValue": '5151',
    "isReconfigurable": true,
    "displayType": "int",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "id": "site property",
    "name": "ranger.ldap.url",
    "displayName": "ranger.ldap.url",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "id": "site property",
    "name": "ranger.ldap.user.dnpattern",
    "displayName": "ranger.ldap.user.dnpattern",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "id": "site property",
    "name": "ranger.ldap.group.roleattribute",
    "displayName": "ranger.ldap.group.roleattribute",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "id": "site property",
    "name": "ranger.ldap.ad.domain",
    "displayName": "ranger.ldap.ad.domain",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "id": "site property",
    "name": "ranger.ldap.ad.url",
    "displayName": "ranger.ldap.ad.url",
    "isReconfigurable": true,
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.ldap.bindkeystore",
    "displayName": "ranger.usersync.ldap.bindkeystore",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.ldap.ldapbindpassword",
    "displayName": "ranger.usersync.ldap.ldapbindpassword",
    "displayType": "password",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.memberattributename",
    "displayName": "ranger.usersync.group.memberattributename",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.nameattribute",
    "displayName": "ranger.usersync.group.nameattribute",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.objectclass",
    "displayName": "ranger.usersync.group.objectclass",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.searchbase",
    "displayName": "ranger.usersync.group.searchbase",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.searchenabled",
    "displayName": "ranger.usersync.group.searchenabled",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.searchfilter",
    "displayName": "ranger.usersync.group.searchfilter",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.searchscope",
    "displayName": "ranger.usersync.group.searchscope",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.group.usermapsyncenabled",
    "displayName": "ranger.usersync.group.usermapsyncenabled",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.ldap.searchBase",
    "displayName": "ranger.usersync.ldap.searchBase",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "ranger.usersync.source.impl.class",
    "displayName": "ranger.usersync.source.impl.class",
    "category": "Advanced ranger-ugsync-site",
    "isRequired": false,
    "serviceName": "RANGER",
    "filename": "ranger-ugsync-site.xml"
  },
  {
    "id": "site property",
    "name": "common.name.for.certificate",
    "displayName": "common.name.for.certificate",
    "category": "Advanced ranger-yarn-plugin-properties",
    "isRequired": false,
    "serviceName": "YARN",
    "filename": "ranger-yarn-plugin-properties.xml"
  },

  /*********RANGER FOR HBASE************/
  {
    "id": "site property",
    "name": "xasecure.hbase.update.xapolicies.on.grant.revoke",
    "recommendedValue": true,
    "displayName": "Should HBase GRANT/REVOKE update XA policies?",
    "displayType": "checkbox",
    "filename": "ranger-hbase-security.xml",
    "category": "Advanced ranger-hbase-security",
    "serviceName": "HBASE"
  },
  /*********RANGER FOR HIVE************/
  {
    "id": "site property",
    "name": "xasecure.hive.update.xapolicies.on.grant.revoke",
    "recommendedValue": true,
    "displayName": "Should Hive GRANT/REVOKE update XA policies?",
    "displayType": "checkbox",
    "filename": "ranger-hive-security.xml",
    "category": "Advanced ranger-hive-security",
    "serviceName": "HIVE"
  }
);

module.exports =
{
  "configProperties": hdp23properties
};
