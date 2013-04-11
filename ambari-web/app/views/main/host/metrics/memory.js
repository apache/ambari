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
 * @class
 * 
 * This is a view for showing host memory metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsMemory = App.ChartLinearTimeView.extend({
  id: "host-metrics-memory",
  title: Em.I18n.t('hosts.host.metrics.memory'),
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  renderer: 'line',

  ajaxIndex: 'host.metrics.memory',

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    var KB = Math.pow(2, 10);
    if (jsonData && jsonData.metrics && jsonData.metrics.memory) {
      for ( var name in jsonData.metrics.memory) {
        var displayName;
        var seriesData = jsonData.metrics.memory[name];
        switch (name) {
          case "mem_shared":
            displayName = Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_shared');
            break;
          case "swap_free":
            displayName = Em.I18n.t('hosts.host.metrics.memory.displayNames.swap_free');
            break;
          case "mem_buffers":
            displayName = Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_buffers');
            break;
          case "mem_free":
            displayName = Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_free');
            break;
          case "mem_cached":
            displayName = Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_cached');
            break;
          default:
            break;
        }
        if (seriesData) {
          var s = this.transformData(seriesData, displayName);
          for (var i = 0; i < s.data.length; i++) {
            s.data[i].y *= KB;
          }
          seriesArray.push(s);
        }
      }
    }
    return seriesArray;
  }
});