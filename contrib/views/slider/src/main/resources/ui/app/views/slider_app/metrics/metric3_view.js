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

/**
 * @class
 *
 * This is a view for showing cluster CPU metrics
 *
 * @extends App.ChartView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.Metric3View = App.ChartView.extend({
  id: "service-metrics-hdfs-file-operations",
  title: 'File Operations',
  renderer: 'line',

  ajaxIndex: 'metrics3',
  yAxisFormatter: App.ChartView.CreateRateFormatter('ops', App.ChartView.DefaultFormatter),

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.dfs && jsonData.metrics.dfs.namenode) {
      for ( var name in jsonData.metrics.dfs.namenode) {
        var displayName;
        var seriesData = jsonData.metrics.dfs.namenode[name];
        switch (name) {
          case "FileInfoOps":
            displayName = 'File Info Ops';
            break;
          case "DeleteFileOps":
            displayName = 'Delete File Ops';
            break;
          case "CreateFileOps":
            displayName = 'Create File Ops';
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