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
  'SQL_COMMAND_INVOKER',
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
    "defaultValue": "",
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
    "index": 0
  },
  {
    "name": "ranger_mysql_database",
    "id": "puppet var",
    "displayName": "Database Type",
    "value": "",
    "defaultValue": "MySQL",
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
    "defaultValue": "ORACLE",
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
    "defaultValue": "Postgres",
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
    "defaultValue": "MSSQL",
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
    "defaultValue": "",
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
  /**************************************** RANGER - HDFS Plugin ***************************************/
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.config.encoding",
    "displayName": "xasecure.audit.hdfs.config.encoding",
    "defaultValue": "",
    "isRequired": false,
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.db.is.enabled",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.is.enabled",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.solr.is.enabled",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.config.encoding",
    "displayName": "xasecure.audit.hdfs.config.encoding",
    "defaultValue": "",
    "isRequired": false,
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.config.encoding",
    "displayName": "xasecure.audit.hdfs.config.encoding",
    "defaultValue": "",
    "isRequired": false,
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.config.encoding",
    "displayName": "xasecure.audit.hdfs.config.encoding",
    "defaultValue": "",
    "isRequired": false,
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.config.encoding",
    "displayName": "xasecure.audit.hdfs.config.encoding",
    "defaultValue": "",
    "isRequired": false,
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
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
    "name": "xasecure.audit.db.is.enabled",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.is.enabled",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.solr.is.enabled",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.config.encoding",
    "displayName": "xasecure.audit.hdfs.config.encoding",
    "defaultValue": "",
    "isRequired": false,
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.db.is.enabled",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.is.enabled",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.solr.is.enabled",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.db.is.enabled",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.is.enabled",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.solr.is.enabled",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.db.is.enabled",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.is.enabled",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.solr.is.enabled",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.db.is.enabled",
    "displayName": "Audit to DB",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.hdfs.is.enabled",
    "displayName": "Audit to HDFS",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "id": "site property",
    "name": "xasecure.audit.solr.is.enabled",
    "displayName": "Audit to SOLR",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "name": "ranger_mysql_host",
    "id": "puppet var",
    "displayName": "MYSQL database Host",
    "description": "Specify the host on which the existing database is hosted",
    "defaultValue": "",
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
    "defaultValue": "",
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
    "defaultValue": "",
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
    "defaultValue": "",
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
    "name": "SQL_COMMAND_INVOKER",
    "id": "site property",
    "displayName": "SQL Command Invoker",
    "defaultValue": "mysql",
    "isObserved": true,
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 10
  },
  {
    "name": "ranger_jdbc_connection_url",
    "id": "site property",
    "displayName": "JDBC connect string for a Ranger database",
    "defaultValue": "",
    "isReconfigurable": true,
    "displayType": "",
    "isOverridable": false,
    "isObserved": true,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 11
  },
  {
    "name": "ranger_jdbc_driver",
    "id": "site property",
    "displayName": "Driver class name for a JDBC Ranger database",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 12
  },
  {
    "name": "db_root_user",
    "id": "site property",
    "displayName": "Ranger DB root user",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 13
  },
  {
    "name": "db_root_password",
    "id": "site property",
    "displayName": "Ranger DB root password",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "password",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 14
  },
  {
    "name": "db_name",
    "id": "site property",
    "displayName": "Ranger DB name",
    "defaultValue": "",
    "isReconfigurable": true,
    "isObserved": true,
    "displayType": "",
    "isOverridable": false,
    "isVisible": true,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 15
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
    "defaultValue": "http://localhost:6080",
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
    "name": "ranger.service.http.enabled",
    "displayName": "HTTP enabled",
    "defaultValue": true,
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
    "defaultValue": "NONE",
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
    "id": "site property",
    "name": "ranger.unixauth.remote.login.enabled",
    "displayName": "Allow remote Login",
    "defaultValue": true,
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
    "defaultValue": 'localhost',
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
    "defaultValue": '5151',
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

  /*********RANGER FOR HBASE************/
  {
    "id": "site property",
    "name": "xasecure.hbase.update.xapolicies.on.grant.revoke",
    "defaultValue": true,
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
    "defaultValue": true,
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
