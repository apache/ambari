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
    var self = this;
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
          value = self.diskUsageFormatted(value.disk_total - value.disk_free, value.disk_total);
          var hostName = item.Hosts.host_name;
          hostToValueMap[hostName] = value;
        }
      });
    }
    return hostToValueMap;
  },

  /**
   * Format percent disk usage to float with 2 digits
   */
  diskUsageFormatted: function(diskUsed, diskTotal) {
    var diskUsage = (diskUsed) / diskTotal * 100;
    if (isNaN(diskUsage) || diskUsage < 0 || diskUsage > 100) {
      return Em.I18n.t('charts.heatmap.unknown');
    }
    var s = Math.round(diskUsage * Math.pow(10, 2)) / Math.pow(10, 2);
    return isNaN(s) ? 0 : s;
  }
});