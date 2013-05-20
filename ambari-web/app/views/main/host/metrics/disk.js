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
 * This is a view for showing host disk usage
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsDisk = App.ChartLinearTimeView.extend({
  id: "host-metrics-disk",
  title: Em.I18n.t('hosts.host.metrics.disk'),
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,
  renderer: 'line',

  ajaxIndex: 'host.metrics.disk',

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    var GB = Math.pow(2, 30);
    if (jsonData && jsonData.metrics && jsonData.metrics.disk) {
      if(jsonData.metrics.part_max_used){
        jsonData.metrics.disk.part_max_used = jsonData.metrics.part_max_used;
      }
      for ( var name in jsonData.metrics.disk) {
        var displayName;
        var seriesData = jsonData.metrics.disk[name];
        switch (name) {
          case "disk_total":
            displayName = Em.I18n.t('hosts.host.metrics.disk.displayNames.disk_total');
            break;
          case "disk_free":
            displayName = Em.I18n.t('hosts.host.metrics.disk.displayNames.disk_free');
            break;
          default:
            break;
        }
        if (seriesData) {
          var s = this.transformData(seriesData, displayName);
          for (var i = 0; i < s.data.length; i++) {
            s.data[i].y *= GB;
          }
          seriesArray.push(s);
        }
      }
    }
    return seriesArray;
  }
});