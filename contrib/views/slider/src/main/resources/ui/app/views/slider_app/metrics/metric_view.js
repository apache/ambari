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

/**
 * @class
 *
 * This is a view for showing cluster CPU metrics
 *
 * @extends App.ChartView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.MetricView = App.ChartView.extend({

  id: "service-metrics-hdfs-space-utilization",

  title: 'Space Utilization',

  yAxisFormatter: App.ChartView.BytesFormatter,

  renderer: 'line',

  ajaxIndex: 'metrics',

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    var GB = Math.pow(2, 30);
    if (jsonData && jsonData.metrics && jsonData.metrics.dfs && jsonData.metrics.dfs.FSNamesystem) {
      for ( var name in jsonData.metrics.dfs.FSNamesystem) {
        var displayName;
        var seriesData = jsonData.metrics.dfs.FSNamesystem[name];
        switch (name) {
          case "CapacityRemainingGB":
            displayName = 'Capacity Remaining GB';
            break;
          case "CapacityUsedGB":
            displayName = 'Capacity Used GB';
            break;
          case "CapacityTotalGB":
            displayName = 'Capacity Total GB';
            break;
          default:
            break;
        }
        if (seriesData) {
          var s = this.transformData(seriesData, displayName);
          for (var i = 0; i < s.data.length; i++) {
            s.data[i].y *= GB;
          }
          seriesArray.push(s);
        }
      }
    }
    return seriesArray;
  }
});