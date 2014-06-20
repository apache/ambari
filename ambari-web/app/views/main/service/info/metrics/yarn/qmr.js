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
var objUtils = require('utils/object_utils');

/**
 * @class
 * 
 * This is a view for showing cluster CPU metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsYARN_QMR = App.ChartLinearTimeView.extend({
  id: "service-metrics-yarn-queue-memory-resource",
  title: Em.I18n.t('services.service.info.metrics.yarn.queueMemoryResource'),
  renderer: 'line',
  ajaxIndex: 'service.metrics.yarn.queue.memory.resource',
  yAxisFormatter: App.ChartLinearTimeView.PercentageFormatter,

  getDataForAjaxRequest: function () {
    var data = this._super();
    var svc = App.YARNService.find().objectAt(0);
    var queueNames = [];
    if (svc != null) {
      queueNames = svc.get('childQueueNames');
    }
    data.queueNames = queueNames;
    return data;
  },

  transformToSeries: function (jsonData) {
    var self = this;
    var seriesArray = [];
    var MB = Math.pow(2, 20);
    var svc = App.YARNService.find().objectAt(0);
    var queueNames = [];
    if (svc != null) {
      queueNames = svc.get('childQueueNames');
    }
    if (jsonData && jsonData.metrics && jsonData.metrics.yarn.Queue) {
      queueNames.forEach(function (qName) {
        var qPath = qName.replace(/\//g, '.');
        var displayName;
        var allocatedData = Em.get(jsonData.metrics.yarn.Queue, qPath + '.AllocatedMB');
        var availableData = Em.get(jsonData.metrics.yarn.Queue, qPath + '.AvailableMB');
        displayName = Em.I18n.t('services.service.info.metrics.yarn.queueMemoryResource.displayName').format(qName);
        var seriesData = null;
        if (allocatedData != null && availableData != null) {
          if (typeof allocatedData == "number" && typeof availableData == "number") {
            seriesData = (availableData == 0 && allocatedData == 0) ? 0 : (allocatedData * 100) / (allocatedData + availableData);
          } else if (allocatedData.length > 0 && availableData.length > 0) {
            seriesData = [];
            for ( var c = 0; c < Math.min(availableData.length, allocatedData.length); c++) {
              var allocDivAvail = (availableData[c][0] == 0 && allocatedData[c][0] == 0) ? 0 : (allocatedData[c][0] * 100) / (availableData[c][0] + allocatedData[c][0]);
              seriesData.push([allocDivAvail, allocatedData[c][1] ]);
            }
          } else {
            console.log("Skipping data series for Queue " + qName);
          }
        }
        if (seriesData != null) {
          seriesArray.push(self.transformData(seriesData, displayName));
        }
      });
    }
    return seriesArray;
  }
});
