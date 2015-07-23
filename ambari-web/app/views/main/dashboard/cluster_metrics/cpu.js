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
App.ChartClusterMetricsCPU = App.ChartLinearTimeView.extend({
  id: "cluster-metrics-cpu",

  ajaxIndex: 'dashboard.cluster_metrics.cpu',

  title: Em.I18n.t('dashboard.clusterMetrics.cpu'),
  yAxisFormatter: App.ChartLinearTimeView.PercentageFormatter,
  isTimePagingDisable: false,
  transformToSeries: function (jsonData) {
    var seriesArray = [];
    var idle = null;

    if (jsonData && jsonData.metrics && jsonData.metrics.cpu) {
      for (var name in jsonData.metrics.cpu) {
        var seriesData = jsonData.metrics.cpu[name];
        if (seriesData) {
          var s = this.transformData(seriesData, name);
          if (name.indexOf("Idle") > -1) {
            //CPU idle metric should be the last in series array
            idle = s;
            continue;
          }
          seriesArray.push(s);
        }
      }
      if (idle) {
        seriesArray.push(idle);
      }
    }
    return seriesArray;
  },
  
  colorForSeries: function (series) {
    if (Em.I18n.t('dashboard.clusterMetrics.cpu.displayNames.idle') == series.name){
      return '#CFECEC';
    }
    return null;
  }
});