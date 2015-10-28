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
 * This is a view for showing Kafka_ControllerStatus
 *
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartServiceMetricsKafka_ControllerStatus = App.ChartLinearTimeView.extend({
  id: "service-metrics-kafka-controler-status-metrics",
  title: Em.I18n.t('services.service.info.metrics.kafka.controller.ControllerStats.title'),
  renderer: 'line',
  ajaxIndex: 'service.metrics.kafka.controller.ControllerStats',

  seriesTemplate: {
    path: 'metrics.kafka.controller.ControllerStats',
    displayName: function (name) {
      var displayNameMap = {
        LeaderElectionRateAndTimeMs: Em.I18n.t('services.service.info.metrics.kafka.controller.ControllerStats.displayNames.LeaderElectionRateAndTimeMs'),
        UncleanLeaderElectionsPerSec: Em.I18n.t('services.service.info.metrics.kafka.controller.ControllerStats.displayNames.UncleanLeaderElectionsPerSec')
      };
      return displayNameMap[name];
    }
  },

  getData: function (jsonData) {
    return this.getKafkaData(jsonData);
  }
});