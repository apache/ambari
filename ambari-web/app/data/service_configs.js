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
require('models/service_config');
//TODO after moving validation/recommendation to BE belov requirement must be deleted
require('utils/configs/defaults_providers/user_defaults_provider');
require('utils/configs/validators/user_configs_validator');

/**
 * This
 * @type {Array} Array of non-service tabs that will appears on  Customizes services page
 */
module.exports = [
  Em.Object.create({
    serviceName: 'MISC',
    displayName: 'Misc',
    //TODO after moving validation/recommendation to BE configsValidator and defaultsProviders must be deleted
    configsValidator: App.userConfigsValidator,
    defaultsProviders: [App.userDefaultsProvider.create()],
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Users and Groups', displayName : 'Users and Groups'})
    ],
    configTypes: {
      "cluster-env": {supports: {final: false}},
      "hadoop-env": {supports: {final: false}},
      "mapred-env": {supports: {final: false}},
      "yarn-env": {supports: {final: false}},
      "hbase-env": {supports: {final: false}},
      "ganglia-env": {supports: {final: false}},
      "nagios-env": {supports: {final: false}},
      "oozie-env": {supports: {final: false}},
      "zookeeper-env": {supports: {final: false}},
      "hive-env": {supports: {final: false}},
      "tez-env": {supports: {final: false}},
      "storm-env": {supports: {final: false}},
      "falcon-env": {supports: {final: false}},
      "webhcat-env": {supports: {final: false}},
      "pig-env": {supports: {final: false}},
      "sqoop-env": {supports: {final: false}},
      "knox-env" : {supports: {final: false}},
      "kafka-env": {supports: {final: false}}
    },
    configs: []
  })
];
