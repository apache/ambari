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
 * This is a view for showing cluster CPU metrics
 *
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsYARN_AllocatedMemory = App.ChartLinearTimeView.extend({
  id: "service-metrics-yarn-queue-allocated",
  title: Em.I18n.t('services.service.info.metrics.yarn.allocated.memory'),
  renderer: 'line',
  ajaxIndex: 'service.metrics.yarn.queue.allocated',
  yAxisFormatter: App.ChartLinearTimeView.BytesFormatter,

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    var MB = Math.pow(2, 20);
    if (jsonData && jsonData.metrics && jsonData.metrics.yarn.Queue && jsonData.metrics.yarn.Queue.root) {
      for (var name in jsonData.metrics.yarn.Queue.root) {
        var displayName = null;
        var seriesData = jsonData.metrics.yarn.Queue.root[name];
        switch (name) {
          case "AvailableMB":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.allocated.memory.displayNames.available');
            break;
          case "PendingMB":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.allocated.memory.displayNames.pending');
            break;
          case "AllocatedMB":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.allocated.memory.displayNames.allocated');
            break;
          default:
            break;
        }
        if (seriesData && displayName) {
          var s = this.transformData(seriesData, displayName);
          for (var i = 0; i < s.data.length; i++) {
            s.data[i].y *= MB;
          }
          seriesArray.push(s);
        }
      }
    }
    return seriesArray;
  }
});