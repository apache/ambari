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
 *
 */
App.MainChartHeatmapCpuWaitIOMetric = App.MainChartHeatmapMetric.extend({
  name: Em.I18n.t('charts.heatmap.metrics.cpuWaitIO'),
  maximumValue: 100,
  defaultMetric: 'metrics.cpu.cpu_wio',
  units: '%',
  slotDefinitionLabelSuffix: '%',
  metricMapper: function (json) {
    var map = this._super(json);
    for ( var host in map) {
      if (host in map) {
        var val = map[host];
        map[host] = (val * 100).toFixed(1);
      }
    }
    return map;
  }
});