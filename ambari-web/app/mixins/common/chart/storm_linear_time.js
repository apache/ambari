/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


var App = require('app');

App.StormLinearTimeChartMixin = Em.Mixin.create({
  ajaxIndex: 'service.metrics.storm.nimbus',
  metricsTemplate: 'metrics/storm/nimbus/{0}[{1},{2},{3}]',

  getDataForAjaxRequest: function() {
    var fromSeconds,
      toSeconds,
      index = this.get('isPopup') ? this.get('currentTimeIndex') : this.get('parentView.currentTimeRangeIndex'),
      customStartTime = this.get('isPopup') ? this.get('customStartTime') : this.get('parentView.customStartTime'),
      customEndTime = this.get('isPopup') ? this.get('customEndTime') : this.get('parentView.customEndTime');
    if (index === 8 && !Em.isNone(customStartTime) && !Em.isNone(customEndTime)) {
      // Custom start and end time is specified by user
      fromSeconds = customStartTime / 1000;
      toSeconds = customEndTime / 1000;
    } else {
      // Preset time range is specified by user
      toSeconds = Math.round(App.dateTime() / 1000);
      fromSeconds = toSeconds - this.get('timeUnitSeconds')
    }
    var metricTemplate = [];
    this.get('stormChartDefinition').forEach(function(chartInfo) {
      metricTemplate.push(
        this.get('metricsTemplate').format(chartInfo.field, fromSeconds, toSeconds, 15)
      );
    }, this);
    return {
      metricsTemplate: metricTemplate.join(',')
    };
  },

  getData: function (jsonData) {
    var dataArray = [],
      pathKeys = ['metrics','storm','nimbus'],
      validPath = true;
    pathKeys.forEach(function(key) {
      if (!jsonData[key]) {
        validPath = false;
      } else {
        jsonData = jsonData[key];
      }
    });
    if (!validPath) {
      return dataArray;
    }
    this.get('stormChartDefinition').forEach(function(chart){
      dataArray.push({
        name: chart.name,
        data: jsonData[chart.field]
      });
    }, this);
    return dataArray;
  }
});
