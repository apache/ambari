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
require('utils/configs/defaults_providers/defaultsProvider');

App.userDefaultsProvider = App.DefaultsProvider.extend({

  clusterData: null,

  /**
   * List of the configs that should be calculated
   */
  configsTemplate: {
    'hdfs_user': null,
    'mapred_user': null,
    'yarn_user': null,
    'hbase_user': null,
    'hive_user': null,
    'hcat_user': null,
    'webhcat_user': null,
    'oozie_user': null,
    'falcon_user': null,
    'storm_user': null,
    'zk_user': null,
    'gmetad_user': null,
    'gmond_user': null,
    'nagios_user': null,
    'smokeuser': null
  },

  // @todo fill with correct values
  getDefaults: function (localDB) {
    this._super();
    var configs = {};
    jQuery.extend(configs, this.get('configsTemplate'));
    return configs;
  }
});

