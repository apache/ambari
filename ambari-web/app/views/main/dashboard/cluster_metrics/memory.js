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
 * This is a view for showing cluster memory metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartClusterMetricsMemory = App.ChartLinearTimeView.extend({
  id: "cluster-metrics-memory",

  ajaxIndex: 'dashboard.cluster_metrics.memory',

  isTimePagingDisable: false,
  title: Em.I18n.t('dashboard.clusterMetrics.memory'),
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  renderer: 'line',
  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.memory) {
      var isAmbariMetricsAvailable = App.StackService.find().someProperty('serviceName', 'AMBARI_METRICS');
      var isAmbariMetricsInstalled = App.Service.find().someProperty('serviceName', 'AMBARI_METRICS');
      var isGangliaInstalled = App.Service.find().someProperty('serviceName', 'GANGLIA');
      var shouldConvertToBytes = isAmbariMetricsInstalled || isAmbariMetricsAvailable && !isGangliaInstalled;
      var KB = Math.pow(2, 10);
      for ( var name in jsonData.metrics.memory) {
        var displayName = name;
        var seriesData = jsonData.metrics.memory[name];
        if (seriesData) {
          var s = this.transformData(seriesData, displayName);
          if (shouldConvertToBytes) {
            for (var i = 0; i < s.data.length; i++) {
              s.data[i].y *= KB;
            }
          }
          seriesArray.push(s);
        }
      }
    }
    return seriesArray;
  }
});