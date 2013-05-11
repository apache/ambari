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
App.MainChartHeatmapDiskSpaceUsedMetric = App.MainChartHeatmapMetric.extend({
  name: Em.I18n.t('charts.heatmap.metrics.diskSpaceUsed'),
  maximumValue: 100,
  defaultMetric: 'metrics.disk',
  units: '%',
  slotDefinitionLabelSuffix: '%',
  metricMapper: function (json) {
    var hostToValueMap = {};
    var metricName = this.get('defaultMetric');
    if (json.items) {
      var props = metricName.split('.');
      json.items.forEach(function (item) {
        var value = item;
        props.forEach(function (prop) {
          if (value != null && prop in value) {
            value = value[prop];
          } else {
            value = null;
          }
        });
        if (value != null) {
          var total = value.disk_total;
          var free = value.disk_free;
          value = (((total - free) * 100) / total).toFixed(1);
          var hostName = item.Hosts.host_name;
          hostToValueMap[hostName] = value;
        }
      });
    }
    return hostToValueMap;
  }
});