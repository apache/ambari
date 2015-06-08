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

  ajaxIndex: 'host.metrics.cpu',

  loadGroup: {
    name: 'host.metrics.aggregated',
    fields: ['metrics/cpu/cpu_user', 'metrics/cpu/cpu_wio', 'metrics/cpu/cpu_nice', 'metrics/cpu/cpu_aidle', 'metrics/cpu/cpu_system', 'metrics/cpu/cpu_idle']
  },

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.cpu) {
      var cpu_idle;
      for ( var name in jsonData.metrics.cpu) {
        var displayName;
        var seriesData = jsonData.metrics.cpu[name];
        switch (name) {
          case "cpu_wio":
            displayName = Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_wio');
            break;
          case "cpu_idle":
            displayName = Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_idle');
            break;
          case "cpu_nice":
            displayName = Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_nice');
            break;
          case "cpu_aidle":
            displayName = Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_aidle');
            break;
          case "cpu_system":
            displayName = Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_system');
            break;
          case "cpu_user":
            displayName = Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_user');
            break;
          default:
            break;
        }
        if (seriesData) {
          var s = this.transformData(seriesData, displayName);
          if (Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_idle') == s.name) {
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
    if (Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_idle') == series.name) {
      return '#CFECEC';
    }
    return null;
  }
});