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
 * This is a view for showing HBase Cluster Requests
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsHBASE_RegionServerQueueSize = App.ChartLinearTimeView.extend({
  id: "service-metrics-hbase-regionserver-queuesize",
  title: "RegionServer Queue Size",
  renderer: 'line',
  url: function () {
    return App.formatUrl(App.apiPrefix + "/clusters/{clusterName}/services/HBASE/components/HBASE_REGIONSERVER?fields=metrics/hbase/regionserver/flushQueueSize[{fromSeconds},{toSeconds},{stepSeconds}],metrics/hbase/regionserver/compactionQueueSize[{fromSeconds},{toSeconds},{stepSeconds}]", {
      clusterName: App.router.get('clusterController.clusterName')
    }, "/data/services/metrics/hbase/regionserver_queuesize.json");
  }.property('App.router.clusterController.clusterName'),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.hbase && jsonData.metrics.hbase.regionserver) {
      for ( var name in jsonData.metrics.hbase.regionserver) {
        var displayName;
        var seriesData = jsonData.metrics.hbase.regionserver[name];
        switch (name) {
          case "compactionQueueSize":
            displayName = "Compaction Queue Size";
            break;
          case "flushQueueSize":
            displayName = "Flush Queue Size";
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