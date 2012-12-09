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
 * This is a view for showing host process counts
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsProcesses = App.ChartLinearTimeView.extend({
  id: "host-metrics-processes",
  title: "Processes",
  renderer: 'line',
  url: function () {
    return App.formatUrl(App.apiPrefix + "/clusters/{clusterName}/hosts/{hostName}?fields=metrics/process/proc_total[{fromSeconds},{toSeconds},{stepSeconds}],metrics/process/proc_run[{fromSeconds},{toSeconds},{stepSeconds}]", {
      clusterName: App.router.get('clusterController.clusterName'),
      hostName: this.get('content').get('hostName')
    }, "/data/hosts/metrics/processes.json");
  }.property('App.router.clusterController.clusterName'),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.process) {
      for ( var name in jsonData.metrics.process) {
        var displayName;
        var seriesData = jsonData.metrics.process[name];
        switch (name) {
          case "proc_total":
            displayName = "Total Processes";
            break;
          case "proc_run":
            displayName = "Processes Run";
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
  }
});