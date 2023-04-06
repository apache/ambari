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
 *
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsFlume_IncommingMMA = App.ChartLinearTimeView.extend({
  id: "service-metrics-flume-incoming_mma",
  renderer: 'line',
  title: Em.I18n.t('services.service.info.metrics.flume.incoming.mma'),

  ajaxIndex: 'service.metrics.flume.incoming_event_put_successCount.mma',
  displayUnit: '/s',

  seriesTemplate: {
    path: 'metrics.flume.flume.CHANNEL.EventPutSuccessCount.rate',
    displayName: function (name) {
      return Em.I18n.t('services.service.info.metrics.flume.incoming_mma').format(name);
    }
  },

  colorForSeries: function (series) {
    var name = Em.I18n.t('services.service.info.metrics.flume.incoming_mma');
    if (name.format("avg") == series.name){
      return '#0066b3';
    }else if (name.format("min") == series.name){
      return '#00CC00';
    }else if (name.format("max") == series.name){
      return '#FF8000';
    }
    return null;
  }
});
