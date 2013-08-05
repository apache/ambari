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
App.ChartServiceMetricsYARN_NMS = App.ChartLinearTimeView.extend({
  id: "service-metrics-yarn-node-manager-statuses",
  title: Em.I18n.t('services.service.info.metrics.yarn.nodemanager.statuses'),
  renderer: 'line',
  ajaxIndex: 'service.metrics.yarn.node.manager.statuses',

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.yarn && jsonData.metrics.yarn.ClusterMetrics) {
      for (var name in jsonData.metrics.yarn.ClusterMetrics) {
        var displayName;
        var seriesData = jsonData.metrics.yarn.ClusterMetrics[name];
        switch (name) {
          case "NumActiveNMs":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.nodemanager.statuses.displayNames.active');
            break;
          case "NumDecommissionedNMs":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.nodemanager.statuses.displayNames.decommissioned');
            break;
          case "NumLostNMs":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.nodemanager.statuses.displayNames.lost');
            break;
          case "NumRebootedNMs":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.nodemanager.statuses.displayNames.rebooted');
            break;
          case "NumUnhealthyNMs":
            displayName = Em.I18n.t('services.service.info.metrics.yarn.nodemanager.statuses.displayNames.unhealthy');
            break;
          default:
            break;
        }
        if (seriesData != null) {
          seriesArray.push(this.transformData(seriesData, displayName));
        }
      }
    }
    return seriesArray;
  }
});