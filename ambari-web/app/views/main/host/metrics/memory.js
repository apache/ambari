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
  url: "/data/hosts/metrics/memory.json",
  title: "Memory Usage",
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  
  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.memory) {
      for ( var name in jsonData.metrics.memory) {
        var displayName;
        var seriesData = jsonData.metrics.memory[name];
        switch (name) {
          case "mem_total":
            displayName = "Total";
            break;
          case "swap_free":
            displayName = "Swap";
            break;
          case "mem_buffers":
            displayName = "Buffers";
            break;
          case "mem_free":
            displayName = "Free";
            break;
          case "mem_cached":
            displayName = "Cached";
            break;
          default:
            break;
        }
        if (seriesData) {
          // Is it a string?
          if ("string" == typeof seriesData) {
            seriesData = JSON.parse(seriesData);
          }
          // We have valid data
          var series = {};
          series.name = displayName;
          series.data = [];
          for ( var index = 0; index < seriesData.length; index++) {
            series.data.push({
              x: seriesData[index][1],
              y: seriesData[index][0]
            });
          }
          seriesArray.push(series);
        }
      }
    }
    return seriesArray;
  },
  
  colorForSeries: function (series) {
    if("Total"==series.name){
      return 'rgba(255,255,255,1)';
    }
    return null;
  }
});