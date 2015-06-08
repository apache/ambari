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
 * This is a view for showing HBase master avereage load
 *
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsAMS_MasterAverageLoad = App.ChartLinearTimeView.extend({
  id: "service-metrics-ambari-metrics-master-average-load",
  title: Em.I18n.t('services.service.info.metrics.ambariMetrics.master.averageLoad'),
  ajaxIndex: 'service.metrics.ambari_metrics.master.average_load',

  loadGroup: {
    name: 'service.metrics.ambari_metrics.aggregated',
    fields: ['metrics/hbase/master/AverageLoad']
  },

  transformToSeries: function (jsonData) {
    var seriesArray = [];
    if (jsonData && jsonData.metrics && jsonData.metrics.hbase && jsonData.metrics.hbase.master) {
      for ( var name in jsonData.metrics.hbase.master) {
        var displayName;
        var seriesData = jsonData.metrics.hbase.master[name];
        switch (name) {
          case "AverageLoad":
            displayName = Em.I18n.t('services.service.info.metrics.ambariMetrics.master.displayNames.averageLoad');
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