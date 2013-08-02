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
App.ChartServiceMetricsYARN_QMR = App.ChartLinearTimeView.extend({
  id: "service-metrics-yarn-queue-memory-resource",
  title: Em.I18n.t('services.service.info.metrics.yarn.queueMemoryResource'),
  renderer: 'line',
  ajaxIndex: 'service.metrics.yarn.queue.memory.resource',

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.yarn.Queue.root) {
      for (var name in jsonData.metrics.yarn.Queue.root) {
        var displayName;
        var seriesData = jsonData.metrics.yarn.Queue.root[name];
        switch (name) {
          case "AllocatedMB":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.queueMemoryResource.displayNames.allocated');
            break;
          case "AvailableMB":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.queueMemoryResource.displayNames.available');
            break;
          default:
            for (var name in jsonData.metrics.yarn.Queue.root.default) {
              seriesData = jsonData.metrics.yarn.Queue.root.default[name];
              switch (name) {
                case "AllocatedMB":
                  displayName = Em.I18n.t('services.service.info.metrics.yarn.queueMemoryResource.displayNames.allocated.default');
                  break;
                case "AvailableMB":
                  displayName = Em.I18n.t('services.service.info.metrics.yarn.queueMemoryResource.displayNames.available.default');
                  break;
                default:
                  break;
              }
            }
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
