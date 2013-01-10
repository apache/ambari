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
 * This is a view for showing cluster CPU metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsHDFS_SpaceUtilization = App.ChartLinearTimeView.extend({
  id: "service-metrics-hdfs-space-utilization",
  title: "Total Space Utilization",
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  renderer: 'line',
  url: function () {
    var hdfsService = App.HDFSService.find().objectAt(0);
    var nameNodeHostName = hdfsService.get('nameNode').get('hostName');
    return App.formatUrl(App.apiPrefix + "/clusters/{clusterName}/hosts/{hostName}/host_components/NAMENODE?fields=metrics/dfs/FSNamesystem/CapacityRemainingGB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/CapacityUsedGB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/CapacityTotalGB[{fromSeconds},{toSeconds},{stepSeconds}]", {
      clusterName: App.router.get('clusterController.clusterName'),
      hostName: nameNodeHostName
    }, "/data/services/metrics/hdfs/space_utilization.json");
  }.property('App.router.clusterController.clusterName'),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.dfs && jsonData.metrics.dfs.FSNamesystem) {
      for ( var name in jsonData.metrics.dfs.FSNamesystem) {
        var displayName;
        var seriesData = jsonData.metrics.dfs.FSNamesystem[name];
        switch (name) {
          case "CapacityRemainingGB":
            displayName = "Capacity Remaining";
            break;
          case "CapacityUsedGB":
            displayName = "Capacity Used";
            break;
          case "CapacityTotalGB":
            displayName = "Capacity Total";
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
          var GB = Math.pow(2,30);
          var series = {};
          series.name = displayName;
          series.data = [];
          for ( var index = 0; index < seriesData.length; index++) {
            series.data.push({
              x: seriesData[index][1],
              y: seriesData[index][0]*GB
            });
          }
          seriesArray.push(series);
        }
      }
    }
    return seriesArray;
  }
});