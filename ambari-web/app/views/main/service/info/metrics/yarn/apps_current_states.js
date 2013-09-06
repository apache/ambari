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
App.ChartServiceMetricsYARN_ApplicationCurrentStates = App.ChartLinearTimeView.extend({
  id: "service-metrics-yarn-apps-current-states",
  title: Em.I18n.t('services.service.info.metrics.yarn.apps.states.current.title'),
  renderer: 'line',
  ajaxIndex: 'service.metrics.yarn.queue.apps.states.current',
      
  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.yarn.Queue && jsonData.metrics.yarn.Queue.root) {
      for (var name in jsonData.metrics.yarn.Queue.root) {
        var displayName = null;
        var seriesData = jsonData.metrics.yarn.Queue.root[name];
        switch (name) {
          case "AppsPending":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.apps.states.pending');
            break;
          case "AppsRunning":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.apps.states.running');
            break;
          default:
            break;
        }
        if (seriesData != null && displayName) {
          seriesArray.push(this.transformData(seriesData, displayName));
        }
      }
    }
    return seriesArray;
  }
});