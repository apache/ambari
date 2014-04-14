/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/configs/defaults_providers/yarn_defaults_provider');

App.HiveDefaultsProvider = App.YARNDefaultsProvider.extend({

  configsTemplate: {
    'hive.tez.container.size': null,
    'hive.tez.java.opts': null,
    'hive.auto.convert.join.noconditionaltask.size': null
  },

  getDefaults: function (localDB) {
    var configs = this._super(localDB);
    if (configs['yarn.scheduler.maximum-allocation-mb'] != null && configs['mapreduce.map.memory.mb'] != null
      && configs['mapreduce.reduce.memory.mb'] != null) {
      var containerSize = configs['mapreduce.map.memory.mb'] > 2048 ? configs['mapreduce.map.memory.mb'] : configs['mapreduce.reduce.memory.mb'];
      containerSize = Math.min(configs['yarn.scheduler.maximum-allocation-mb'], containerSize);
      configs['hive.auto.convert.join.noconditionaltask.size'] = Math.round(containerSize / 3) * 1048576; // MB to Bytes
      configs['hive.tez.java.opts'] = "-server -Xmx" + Math.round(0.8 * containerSize) + "m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC";
      configs['hive.tez.container.size'] = containerSize;
    } else {
      jQuery.extend(configs, this.get('configsTemplate'));
    }
    return configs;
  }

});
