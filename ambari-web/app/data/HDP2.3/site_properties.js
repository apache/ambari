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
  'db_host',
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
  'ranger.ldap.base.dn',
  'ranger.ldap.bind.dn',
  'ranger.ldap.bind.password',
  'ranger.ldap.referral',
  'xa_ldap_userSearchFilter',
  'xa_ldap_ad_domain',
  'xa_ldap_ad_url',
  'ranger.ldap.ad.base.dn',
  'ranger.ldap.ad.bind.dn',
  'ranger.ldap.ad.bind.password',
  'ranger.ldap.ad.referral',
  'xa_ldap_ad_userSearchFilter',
  'policymgr_http_enabled',
  'policymgr_external_url',
  'hbase.regionserver.global.memstore.lowerLimit',
  'hbase.regionserver.global.memstore.upperLimit',
  "port",
  "hive.metastore.heapsize",
  "hive.client.heapsize",
  "SQL_COMMAND_INVOKER",
  "SYNC_LDAP_USER_SEARCH_FILTER"
];

var hdp23properties = hdp22properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp23properties.push({
    "name": "DB_FLAVOR",
    "options": [
      {
        displayName: 'MYSQL'
      },
      {
        displayName: 'ORACLE'
      },
      {
        displayName: 'POSTGRES'
      },
      {
        displayName: 'MSSQL'
      },
      {
        displayName: 'SQLA',
        hidden: App.get('currentStackName') !== 'SAPHD' && App.get('currentStackName') !== 'HDP'
      }
    ],
    "displayType": "radio button",
    "radioName": "RANGER DB_FLAVOR",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 1
  },
  {
    "name": "db_host",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 2
  },
  {
    "name": "rangerserver_host",
    "displayName": "Ranger Server host",
    "value": "",
    "recommendedValue": "",
    "description": "The host that has been assigned to run Ranger Server",
    "displayType": "masterHost",
    "isOverridable": false,
    "isRequiredByAgent": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 0
  },
  {
    "name": "create_db_dbuser",
    "displayType": "checkbox",
    "filename": "ranger-env.xml",
    "category": "Advanced ranger-env",
    "serviceName": "RANGER"
  },
  /**************************************** RANGER - HDFS Plugin ***************************************/
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-kms-audit.xml",
    "category": "Advanced ranger-kms-audit",
    "serviceName": "RANGER_KMS"
  },        
  {
    "name": "ranger-yarn-plugin-enabled",
    "displayType": "checkbox",
    "filename": "ranger-yarn-plugin-properties.xml",
    "category": "Advanced ranger-yarn-plugin-properties",
    "serviceName": "YARN",
    "index": 1
  },
  {
    "name": "ranger-kafka-plugin-enabled",
    "displayType": "checkbox",
    "filename": "ranger-kafka-plugin-properties.xml",
    "category": "Advanced ranger-kafka-plugin-properties",
    "serviceName": "KAFKA",
    "index": 1
  },
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "name": "nimbus.seeds",
    "displayType": "masterHosts",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "xasecure.audit.destination.db",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "name": "xasecure.audit.destination.hdfs",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "name": "xasecure.audit.destination.solr",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-audit.xml",
    "category": "Advanced ranger-hdfs-audit",
    "serviceName": "HDFS"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-hbase-audit.xml",
    "category": "Advanced ranger-hbase-audit",
    "serviceName": "HBASE"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-hive-audit.xml",
    "category": "Advanced ranger-hive-audit",
    "serviceName": "HIVE"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-knox-audit.xml",
    "category": "Advanced ranger-knox-audit",
    "serviceName": "KNOX"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-yarn-audit.xml",
    "category": "Advanced ranger-yarn-audit",
    "serviceName": "YARN"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-storm-audit.xml",
    "category": "Advanced ranger-storm-audit",
    "serviceName": "STORM"
  },
  {
    "name": "xasecure.audit.provider.summary.enabled",
    "displayType": "checkbox",
    "filename": "ranger-kafka-audit.xml",
    "category": "Advanced ranger-kafka-audit",
    "serviceName": "KAFKA"
  },
  {
    "name": "ranger.jpa.jdbc.url",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "DBSettings",
    "index": 9
  },
  {
    "name": "ranger.jpa.jdbc.driver",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "DBSettings",
    "index": 8
  },
  {
    "name": "db_root_user",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 5
  },
  {
    "name": "db_root_password",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 6
  },
  {
    "name": "db_name",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 7
  },
  {
    "name": "ranger.externalurl",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
  },
  {
    "name": "ranger.service.http.enabled",
    "displayType": "checkbox",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
  },
  {
    "name": "ranger.authentication.method",
    "options": [
      {
        displayName: 'LDAP',
        foreignKeys: ['ranger.ldap.group.roleattribute', 'ranger.ldap.url', 'ranger.ldap.user.dnpattern','ranger.ldap.base.dn','ranger.ldap.bind.dn','ranger.ldap.bind.password','ranger.ldap.referral','ranger.ldap.user.searchfilter']
      },
      {
        displayName: 'ACTIVE_DIRECTORY',
        foreignKeys: ['ranger.ldap.ad.domain', 'ranger.ldap.ad.url','ranger.ldap.ad.base.dn','ranger.ldap.ad.bind.dn','ranger.ldap.ad.bind.password','ranger.ldap.ad.referral','ranger.ldap.ad.user.searchfilter']
      },
      {
        displayName: 'UNIX',
        foreignKeys: ['ranger.unixauth.service.port', 'ranger.unixauth.service.hostname', 'ranger.unixauth.remote.login.enabled']
      },
      {
        displayName: 'NONE'
      }
    ],
    "displayType": "radio button",
    "radioName": "authentication-method",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
  },
  {
    "name": "policymgr_external_url",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "name": "ranger.unixauth.remote.login.enabled",
    "displayType": "checkbox",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "name": "ranger.unixauth.service.hostname",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "name": "ranger.unixauth.service.port",
    "displayType": "int",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "name": "ranger.ldap.url",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.user.dnpattern",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.group.roleattribute",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.base.dn",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.bind.dn",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.bind.password",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.referral",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.user.searchfilter",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "ranger.ldap.ad.domain",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "name": "ranger.ldap.ad.url",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "name": "ranger.ldap.ad.base.dn",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "name": "ranger.ldap.ad.bind.dn",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "name": "ranger.ldap.ad.bind.password",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "name": "ranger.ldap.ad.referral",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  {
    "name": "ranger.ldap.ad.user.searchfilter",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "ADSettings"
  },
  /*********RANGER FOR HBASE************/
  {
    "name": "xasecure.hbase.update.xapolicies.on.grant.revoke",
    "displayType": "checkbox",
    "filename": "ranger-hbase-security.xml",
    "category": "Advanced ranger-hbase-security",
    "serviceName": "HBASE"
  },
  /*********RANGER FOR HIVE************/
  {
    "name": "xasecure.hive.update.xapolicies.on.grant.revoke",
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
