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
 * This is a view for showing Host CPU metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsCPU = App.ChartLinearTimeView.extend({
  id: "host-metrics-cpu",
  title: "CPU Usage",
  yAxisFormatter: App.ChartLinearTimeView.PercentageFormatter,


  url: function () {
    return App.formatUrl(App.apiPrefix + "/clusters/{clusterName}/hosts/{hostName}?fields=metrics/cpu/cpu_user[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_wio[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_nice[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_aidle[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_system[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_idle[{fromSeconds},{toSeconds},{stepSeconds}]", {
      clusterName: App.router.get('clusterController.clusterName'),
      hostName: this.get('content').get('hostName')
    }, "/data/hosts/metrics/cpu.json");
  }.property('App.router.clusterController.clusterName'),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.cpu) {
      var cpu_idle;
      for ( var name in jsonData.metrics.cpu) {
        var displayName;
        var seriesData = jsonData.metrics.cpu[name];
        switch (name) {
          case "cpu_wio":
            displayName = "CPU I/O Idle";
            break;
          case "cpu_idle":
            displayName = "CPU Idle";
            break;
          case "cpu_nice":
            displayName = "CPU Nice";
            break;
          case "cpu_aidle":
            displayName = "CPU Boot Idle";
            break;
          case "cpu_system":
            displayName = "CPU System";
            break;
          case "cpu_user":
            displayName = "CPU User";
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
          if (name != 'cpu_idle') {
            seriesArray.push(series);
          }
          else {
            cpu_idle = series;
          }
        }
      }
      seriesArray.push(cpu_idle);
    }
    return seriesArray;
  },

  colorForSeries: function (series) {
    if ("CPU Idle" == series.name) {
      return '#CFECEC';
    }
    return null;
  }
});