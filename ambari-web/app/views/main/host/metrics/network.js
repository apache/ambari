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
 * This is a view for showing host network metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsNetwork = App.ChartLinearTimeView.extend({
  id: "host-metrics-network",
  title: Em.I18n.t('hosts.host.metrics.network'),
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  renderer: 'line',

  ajaxIndex: 'host.metrics.network',

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.network) {
      for ( var name in jsonData.metrics.network) {
        var displayName;
        var seriesData = jsonData.metrics.network[name];
        switch (name) {
          case "pkts_out":
            displayName = Em.I18n.t('hosts.host.metrics.network.displayNames.pkts_out');
            break;
          case "bytes_in":
            displayName = Em.I18n.t('hosts.host.metrics.network.displayNames.bytes_in');
            break;
          case "bytes_out":
            displayName = Em.I18n.t('hosts.host.metrics.network.displayNames.bytes_out');
            break;
          case "pkts_in":
            displayName = Em.I18n.t('hosts.host.metrics.network.displayNames.pkts_in');
            break;
          default:
            break;
        }
        if (seriesData) {
          seriesArray.push(this.transformData(seriesData, displayName));
        }
      }
    }
    return seriesArray;
  }
});