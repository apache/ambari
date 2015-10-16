/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');
var date = require('utils/date/date');

App.MainDashboardServiceStormView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/storm'),
  serviceName: 'STORM',

  /**
   * this parameter is used to fiter hosts by component name
   * used in mainHostController.filterByComponent() method
   */
  filterComponent: function() {
    return Em.Object.create({componentName: 'SUPERVISOR'});
  }.property(),

  freeSlotsPercentage: function() {
    return Math.round(this.get('service.freeSlots')/this.get('service.totalSlots')*100);
  }.property('service.freeSlots', 'service.totalSlots'),

  superVisorsLive: function () {
    return this.get('service.superVisorsStarted');
  }.property('service.superVisorsStarted'),

  superVisorsTotal: function() {
    return this.get('service.superVisorsTotal');
  }.property('service.superVisorsTotal'),

  nimbusUptimeFormatted: function() {
    return this.get('service.nimbusUptime') || Em.I18n.t('services.service.summary.notRunning');
  }.property('service.nimbusUptime')
});
