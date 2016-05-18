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
  /**************************************** RANGER - HDFS Plugin ***************************************/

    "name": "ranger-yarn-plugin-enabled",
    "filename": "ranger-yarn-plugin-properties.xml",
    "serviceName": "YARN",
    "index": 1
  },
  {
    "name": "ranger-kafka-plugin-enabled",
    "filename": "ranger-kafka-plugin-properties.xml",
    "serviceName": "KAFKA",
    "index": 1
  },
  {
    "name": "nimbus.seeds",
    "serviceName": "STORM",
    "filename": "storm-site.xml",
    "category": "NIMBUS"
  },
  {
    "name": "ranger.externalurl",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "RangerSettings"
  },
  {
    "name": "ranger.service.http.enabled",
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
        foreignKeys: ['ranger.ldap.ad.domain','ranger.ldap.ad.url','ranger.ldap.ad.base.dn','ranger.ldap.ad.bind.dn','ranger.ldap.ad.bind.password','ranger.ldap.ad.referral','ranger.ldap.ad.user.searchfilter']
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
    "name": "ranger.sso.providerurl",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "KnoxSSOSettings"
  },
  {
    "name": "ranger.sso.publicKey",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "KnoxSSOSettings"
  },
  {
    "name": "ranger.sso.cookiename",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "KnoxSSOSettings"
  },
  {
    "name": "ranger.sso.enabled",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "KnoxSSOSettings"
  },
  {
    "name": "ranger.sso.query.param.originalurl",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "KnoxSSOSettings"
  },
  {
    "name": "ranger.sso.browser.useragent",
    "serviceName": "RANGER",
    "filename": "ranger-admin-site.xml",
    "category": "KnoxSSOSettings"
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
  /*********************************************** HAWQ **********************************************/
  {
    "name": "hawq_master_address_host",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 0
  },
  {
    "name": "hawq_standby_address_host",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 1
  },
  {
    "name": "hawq_master_address_port",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 2
  },
  {
    "name": "hawq_segment_address_port",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 3
  },
  {
    "name": "hawq_dfs_url",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 4
  },
  {
    "name": "hawq_master_directory",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 5
  },
  {
    "name": "hawq_master_temp_directory",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 6
  },
  {
    "name": "hawq_segment_directory",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 7
  },
  {
    "name": "hawq_segment_temp_directory",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 8
  },
  {
    "name": "default_segment_num",
    "filename": "hawq-site.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 9
  },
  {
    "name": "hawq_ssh_exkeys",
    "filename": "hawq-env.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 10
  },
  {
    "name": "hawq_password",
    "filename": "hawq-env.xml",
    "category": "General",
    "serviceName": "HAWQ",
    "index": 11
  },
  {
    "name": "content",
    "serviceName": "HAWQ",
    "filename": "hawq-check-env.xml",
    "category": "AdvancedHawqCheck"
  }
);

module.exports =
{
  "configProperties": hdp23properties
};
