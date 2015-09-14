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
  'hive.heapsize'
];
var hdp22properties = hdp2properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp22properties.push(
  {
    "name": "hive.zookeeper.quorum",
    "displayType": "multiLine",
    "serviceName": "HIVE",
    "filename": "hive-site.xml",
    "category": "Advanced hive-site"
  },
  {
    "name": "hadoop.registry.rm.enabled",
    "displayType": "checkbox",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "Advanced yarn-site"
  },
  {
    "name": "hadoop.registry.zk.quorum",
    "serviceName": "YARN",
    "filename": "yarn-site.xml",
    "category": "Advanced yarn-site"
  },
  {
    "name": "yarn.timeline-service.leveldb-state-store.path",
    "category": "APP_TIMELINE_SERVER",
    "displayType": "directory",
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
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  {
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  {
    "name": "ranger-hdfs-plugin-enabled",
    "displayType": "checkbox",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS",
    "index": 1
  },
  {
    "name": "policy_user",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  {
    "name": "REPOSITORY_CONFIG_USERNAME",
    "filename": "ranger-hdfs-plugin-properties.xml",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS"
  },
  /*********RANGER FOR HIVE************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayType": "checkbox",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "name": "policy_user",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "name": "REPOSITORY_CONFIG_USERNAME",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  {
    "name": "UPDATE_XAPOLICIES_ON_GRANT_REVOKE",
    "displayType": "checkbox",
    "filename": "ranger-hive-plugin-properties.xml",
    "category": "Advanced ranger-hive-plugin-properties",
    "serviceName": "HIVE"
  },
  /*********RANGER FOR HBASE************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayType": "checkbox",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "name": "ranger-hbase-plugin-enabled",
    "displayType": "checkbox",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE",
    "index": 1
  },
  {
    "name": "policy_user",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "name": "REPOSITORY_CONFIG_USERNAME",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  {
    "name": "UPDATE_XAPOLICIES_ON_GRANT_REVOKE",
    "displayType": "checkbox",
    "filename": "ranger-hbase-plugin-properties.xml",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE"
  },
  /*********RANGER FOR STORM************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  {
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayType": "checkbox",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  {
    "name": "ranger-storm-plugin-enabled",
    "displayType": "checkbox",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM",
    "index": 1
  },
  {
    "name": "policy_user",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  {
    "name": "REPOSITORY_CONFIG_USERNAME",
    "filename": "ranger-storm-plugin-properties.xml",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM"
  },
  /*********RANGER FOR KNOX************/
  {
    "name": "XAAUDIT.HDFS.IS_ENABLED",
    "displayType": "checkbox",
    "dependentConfigPattern": "^XAAUDIT.HDFS",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  {
    "name": "XAAUDIT.DB.IS_ENABLED",
    "displayType": "checkbox",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  {
    "name": "ranger-knox-plugin-enabled",
    "displayType": "checkbox",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX",
    "index": 1
  },
  {
    "name": "policy_user",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  {
    "name": "REPOSITORY_CONFIG_USERNAME",
    "filename": "ranger-knox-plugin-properties.xml",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX"
  },
  /**********************************************SPARK***************************************/
  {
    "name": "spark.driver.extraJavaOptions",
    "category": "Advanced spark-defaults",
    "serviceName": "SPARK",
    "filename": "spark-defaults.xml"
  },
  {
    "name": "spark.yarn.am.extraJavaOptions",
    "category": "Advanced spark-defaults",
    "serviceName": "SPARK",
    "filename": "spark-defaults.xml"
  },
  /**********************************************RANGER***************************************/
  {
    "name": "ranger_admin_password",
    "serviceName": "RANGER",
    "filename": "ranger-env.xml",
    "category": "AdminSettings"
  },
  {
    "name": "SQL_CONNECTOR_JAR",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "AdminSettings"
  },
  {
    "name": "DB_FLAVOR",
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
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 1
  },
  {
    "name": "SQL_COMMAND_INVOKER",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings"
  },
  {
    "name": "db_host",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 2
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
    "name": "db_user",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 3
  },
  {
    "name": "db_password",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 4
  },
  {
    "name": "audit_db_name",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 11
  },
  {
    "name": "audit_db_user",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 12
  },
  {
    "name": "audit_db_password",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "DBSettings",
    "index": 13
  },
  {
    "name": "policymgr_external_url",
    "serviceName": "RANGER",
    "filename": "admin-properties.xml",
    "category": "RangerSettings"
  },
  {
    "name": "policymgr_http_enabled",
    "displayType": "checkbox",
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
    "displayType": "checkbox",
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
    "displayType": "int",
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
  },
  {
    "name": "common.name.for.certificate",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml"
  },
  {
    "name": "hadoop.rpc.protection",
    "category": "Advanced ranger-hdfs-plugin-properties",
    "serviceName": "HDFS",
    "filename": "ranger-hdfs-plugin-properties.xml"
  },  
  {
    "name": "common.name.for.certificate",
    "serviceName": "HIVE",
    "filename": "ranger-hive-plugin-properties.xml"
  },
  {
    "name": "common.name.for.certificate",
    "category": "Advanced ranger-hbase-plugin-properties",
    "serviceName": "HBASE",
    "filename": "ranger-hbase-plugin-properties.xml"
  },
  {
    "name": "common.name.for.certificate",
    "category": "Advanced ranger-knox-plugin-properties",
    "serviceName": "KNOX",
    "filename": "ranger-knox-plugin-properties.xml"
  },
  {
    "name": "common.name.for.certificate",
    "category": "Advanced ranger-storm-plugin-properties",
    "serviceName": "STORM",
    "filename": "ranger-storm-plugin-properties.xml"
  },
  {
    "name": "SYNC_LDAP_USER_SEARCH_FILTER",
    "category": "Advanced usersync-properties",
    "serviceName": "RANGER",
    "filename": "usersync-properties.xml"
  },
  {
    "name": "hbase.bucketcache.ioengine",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "name": "hbase.bucketcache.size",
    "displayType": "int",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "name": "hbase.bucketcache.percentage.in.combinedcache",
    "displayType": "float",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml",
    "category": "Advanced hbase-site"
  },
  {
    "name": "hbase_max_direct_memory_size",
    "displayType": "int",
    "serviceName": "HBASE",
    "filename": "hbase-env.xml",
    "category": "Advanced hbase-env"
  },
  {
    "name": "hbase.region.server.rpc.scheduler.factory.class",
    "category": "Advanced hbase-site",
    "serviceName": "HBASE",
    "filename": "hbase-site.xml"
  },
  {
    "name": "hbase.rpc.controllerfactory.class",
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
