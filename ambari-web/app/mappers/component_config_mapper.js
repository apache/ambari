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

/**
 * The usage of previousResponse is due to detect
 * which exactly components has stale_configs changed in comparison to previous response
 */
var previousResponse = {};

App.componentConfigMapper = App.QuickDataMapper.create({
  map: function (json) {
    console.time('App.componentConfigMapper execution time');
    if (json.items) {
      var hostComponentRecordsMap = App.cache['hostComponentRecordsMap'];
      var staleConfigsTrue = [];
      var currentResponse = {};
      json.items.forEach(function (component) {
        var id = component.HostRoles.component_name + "_" + component.HostRoles.host_name;
        if (previousResponse[id]) {
          delete previousResponse[id];
        } else {
          staleConfigsTrue.push(id);
        }
        currentResponse[id] = true;
      });

      /**
       * if stale_configs of components became
       * true:
       *  then they will be in "staleConfigsTrue" object
       * false:
       *  then they will be in "previousResponse" object
       * if stale_configs haven't changed then both objects will be empty and components stay the same
       */
      staleConfigsTrue.forEach(function (id) {
        hostComponentRecordsMap[id].set('staleConfigs', true);
      });
      for (var id in previousResponse) {
        hostComponentRecordsMap[id].set('staleConfigs', false)
      }
      previousResponse = currentResponse;
    }
    console.timeEnd('App.componentConfigMapper execution time');
  }
});
