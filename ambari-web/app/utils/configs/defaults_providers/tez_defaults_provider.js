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

App.TezDefaultsProvider = App.YARNDefaultsProvider.extend({

  configsTemplate: {
    'tez.am.resource.memory.mb': null,
    'tez.am.java.opts': null
  },

  getDefaults : function(localDB) {
    var configs = this._super(localDB);
    if (configs['yarn.app.mapreduce.am.resource.mb'] != null) {
      configs['tez.am.resource.memory.mb'] = configs['yarn.app.mapreduce.am.resource.mb'];
      configs['tez.am.java.opts'] = '-server -Xmx' + Math.round(0.8 * configs['tez.am.resource.memory.mb'])
          + 'm -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC';
    } else {
      jQuery.extend(configs, this.get('configsTemplate'));
    }
    return configs;
  }

});
