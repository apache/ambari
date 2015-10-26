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
  'tez.am.container.session.delay-allocation-millis',
  'tez.am.grouping.max-size',
  'tez.am.grouping.min-size',
  'tez.am.grouping.split-waves',
  'tez.am.java.opts',
  'tez.runtime.intermediate-input.compress.codec',
  'tez.runtime.intermediate-input.is-compressed',
  'tez.runtime.intermediate-output.compress.codec',
  'tez.runtime.intermediate-output.should-compress',
  'hive.heapsize'
];
var hdp22properties = hdp2properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp22properties.push(
  {
    "name": "yarn.timeline-service.leveldb-state-store.path",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "yarn.timeline-service.state-store-class",
    "category": "APP_TIMELINE_SERVER",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": "*.falcon.graph.blueprints.graph",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  {
    "name": "*.falcon.graph.storage.backend",
    "category": "FalconStartupSite",
    "serviceName": "FALCON",
    "filename": "falcon-startup.properties.xml"
  },
  /*********RANGER FOR HDFS************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "ranger-hdfs-plugin-enabled",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "serviceName": "HDFS",
    "index": 1
  },
  /*********RANGER FOR HIVE************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hive-plugin-properties.xml",
    "serviceName": "HIVE"
  },
  /*********RANGER FOR HBASE************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hbase-plugin-properties.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "ranger-hbase-plugin-enabled",
    "filename": "ranger-hbase-plugin-properties.xml",
    "serviceName": "HBASE",
    "index": 1
  },
  /*********RANGER FOR STORM************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-storm-plugin-properties.xml",
    "serviceName": "STORM"
  },
  {
    "name": "ranger-storm-plugin-enabled",
    "filename": "ranger-storm-plugin-properties.xml",
    "serviceName": "STORM",
    "index": 1
  },
  /*********RANGER FOR KNOX************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-knox-plugin-properties.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "ranger-knox-plugin-enabled",
    "filename": "ranger-knox-plugin-properties.xml",
    "serviceName": "KNOX",
    "index": 1
  },
  {
    "name": "ranger_admin_username",
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "RANGER_ADMIN",
    "index": 0
  },
  {
    "name": "ranger_admin_username",
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "RANGER_ADMIN",
    "index": 0
  },
  {
    "name": "ranger_admin_password",
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "RANGER_ADMIN",
    "index": 1
  },
  {
    "name": "SQL_CONNECTOR_JAR",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RANGER_ADMIN",
    "index": 2
  },
  {
    "name": "SQL_COMMAND_INVOKER",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings"
  },
  {
    "name": "policymgr_external_url",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "name": "policymgr_http_enabled",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "name": "ranger_user",
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "RangerSettings"
  },
  {
    "name": "ranger_group",
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "RangerSettings"
  },
  {
    "name": "authentication_method",
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
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "name": "remoteLoginEnabled",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "name": "authServiceHostName",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "name": "authServicePort",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "UnixAuthenticationSettings"
  },
  {
    "name": "xa_ldap_url",
    "isOverridable": false,
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "xa_ldap_userDNpattern",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "xa_ldap_groupRoleAttribute",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "LDAPSettings"
  },
  {
    "name": "xa_ldap_ad_domain",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "ADSettings"
  },
  {
    "name": "xa_ldap_ad_url",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "ADSettings"
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
