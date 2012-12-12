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
  title: "Network Usage",
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  renderer: 'line',
  url: function () {
    return App.formatUrl(App.apiPrefix + "/clusters/{clusterName}/hosts/{hostName}?fields=metrics/network/bytes_in[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/bytes_out[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/pkts_in[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/pkts_out[{fromSeconds},{toSeconds},{stepSeconds}]", {
      clusterName: App.router.get('clusterController.clusterName'),
      hostName: this.get('content').get('hostName')
    }, "/data/hosts/metrics/network.json");
  }.property('App.router.clusterController.clusterName'),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.network) {
      for ( var name in jsonData.metrics.network) {
        var displayName;
        var seriesData = jsonData.metrics.network[name];
        switch (name) {
          case "pkts_out":
            displayName = "Packets Out";
            break;
          case "bytes_in":
            displayName = "Bytes In";
            break;
          case "bytes_out":
            displayName = "Bytes Out";
            break;
          case "pkts_in":
            displayName = "Packets In";
            break;
          default:
            break;
        }
        if (seriesData) {
          // Is it a string?
          if ("string" == typeof seriesData) {
            seriesData = JSON.parse(seriesData);
          }
          // We have valid data
          var series = {};
          series.name = displayName;
          series.data = [];
          for ( var index = 0; index < seriesData.length; index++) {
            series.data.push({
              x: seriesData[index][1],
              y: seriesData[index][0]
            });
          }
          seriesArray.push(series);
        }
      }
    }
    return seriesArray;
  }
});