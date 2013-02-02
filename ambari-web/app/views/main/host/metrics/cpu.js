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
  title: Em.I18n.t('hosts.host.metrics.cpu'),
  yAxisFormatter: App.ChartLinearTimeView.PercentageFormatter,


  url: function () {
    return App.formatUrl(
      this.get('urlPrefix') + "/hosts/{hostName}?fields=metrics/cpu/cpu_user[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_wio[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_nice[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_aidle[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_system[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_idle[{fromSeconds},{toSeconds},{stepSeconds}]",
      {
        hostName: this.get('content').get('hostName')
      },
      "/data/hosts/metrics/cpu.json"
    );
  }.property('clusterName').volatile(),

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
          var s = this.transformData(seriesData, displayName);
          if ('CPU Idle' == s.name) {
            cpu_idle = s;
          }
          else {
            seriesArray.push(s);
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