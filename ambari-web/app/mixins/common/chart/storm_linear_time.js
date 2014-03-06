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
    var currentTime = Math.round(App.dateTime() / 1000);
    var metricTemplate = [];
    this.get('stormChartDefinition').forEach(function(chartInfo) {
      metricTemplate.push(
        this.get('metricsTemplate').format(chartInfo.field, currentTime - this.get('timeUnitSeconds'), currentTime, 15)
      );
    }, this);
    return {
      metricsTemplate: metricTemplate.join(',')
    };
  },

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    var pathKeys = ['metrics','storm','nimbus'];
    var validPath = true;
    pathKeys.forEach(function(key) {
      if (!jsonData[key]) {
        validPath = false;
      } else {
        jsonData = jsonData[key];
      }
    });
    if (!validPath) {
      return seriesArray;
    }
    this.get('stormChartDefinition').forEach(function(chart){
      seriesArray.push(this.transformData(jsonData[chart.field], chart.name));
    }, this);
    return seriesArray;
  }
});
