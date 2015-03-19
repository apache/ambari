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
var configs = [
/**********************************************core-site***************************************/
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hive_master_hosts"],
    "foreignKey": ["hive_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": [],
    "foreignKey": ["oozie_user"],
    "value": "*",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["oozieserver_host"],
    "foreignKey": ["oozie_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hive_master_hosts"],
    "foreignKey": ["webhcat_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["falcon_user"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": [],
    "foreignKey": ["falcon_user"],
    "value": "*",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": [],
    "foreignKey": ["hdfs_user"],
    "value": "*",
    "filename": "core-site.xml",
    "isOverridable": true
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": [],
    "foreignKey": ["hdfs_user"],
    "value": "*",
    "filename": "core-site.xml",
    "isOverridable": true
  },
/**********************************Oozie******************************/

  {
    "name": "oozie.service.ProxyUserService.proxyuser.<foreignKey[0]>.groups",
    "templateName": [],
    "foreignKey": ["falcon_user"],
    "value": "*",
    "filename": "oozie-site.xml",
    "isOverridable": true,
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.service.ProxyUserService.proxyuser.<foreignKey[0]>.hosts",
    "templateName": [],
    "foreignKey": ["falcon_user"],
    "value": "*",
    "filename": "oozie-site.xml",
    "isOverridable": true,
    "serviceName": "OOZIE"
  }
];

/**
 * Configs consists of 2 types: Computed values, which cannot be modified by user
 * and overridable values, which user can modify. We provide interface how to get all of this
 * configs separately
 * @type {Object}
 */
module.exports = {

  checkPrecondition: function () {
    return configs.filter(function (config) {
      return ((!config.precondition) || (config.precondition()));
    });
  },
  all: function (skipPreconditionCheck) {
    if (skipPreconditionCheck) {
      return configs.slice(0);
    } else {
      return this.checkPrecondition().slice(0);
    }
  },
  overridable: function (skipPreconditionCheck) {
    if (skipPreconditionCheck) {
      return configs.filterProperty('foreignKey');
    } else {
      return this.checkPrecondition().filterProperty('foreignKey');
    }
  },
  computed: function (skipPreconditionCheck) {
    if (skipPreconditionCheck) {
      return configs.filterProperty('foreignKey', null);
    } else {
      return this.checkPrecondition().filterProperty('foreignKey', null);
    }
  }
};
