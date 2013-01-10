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
 * This is a view for showing host disk usage
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsDisk = App.ChartLinearTimeView.extend({
  id: "host-metrics-disk",
  title: "Disk Usage",
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  renderer: 'line',
  url: function () {
    return App.formatUrl(App.apiPrefix + "/clusters/{clusterName}/hosts/{hostName}?fields=metrics/disk/disk_total[{fromSeconds},{toSeconds},{stepSeconds}],metrics/disk/disk_free[{fromSeconds},{toSeconds},{stepSeconds}]", {
      clusterName: App.router.get('clusterController.clusterName'),
      hostName: this.get('content').get('hostName')
    }, "/data/hosts/metrics/disk.json");
  }.property('App.router.clusterController.clusterName'),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.disk) {
      if(jsonData.metrics.part_max_used){
        jsonData.metrics.disk.part_max_used = jsonData.metrics.part_max_used;
      }
      for ( var name in jsonData.metrics.disk) {
        var displayName;
        var seriesData = jsonData.metrics.disk[name];
        switch (name) {
          case "disk_total":
            displayName = "Total";
            break;
          case "disk_free":
            displayName = "Available";
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
          var GB = Math.pow(2, 30);
          var series = {};
          series.name = displayName;
          series.data = [];
          for ( var index = 0; index < seriesData.length; index++) {
            series.data.push({
              x: seriesData[index][1],
              y: seriesData[index][0] * GB
            });
          }
          seriesArray.push(series);
        }
      }
    }
    return seriesArray;
  }
});