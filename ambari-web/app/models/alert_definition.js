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
var dateUtils = require('utils/date');

App.AlertDefinition = DS.Model.extend({

  name: DS.attr('string'),
  label: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  componentName: DS.attr('string'),
  enabled: DS.attr('boolean'),
  scope: DS.attr('string'),
  interval: DS.attr('number'),
  type: DS.attr('string'),
  reporting: DS.hasMany('App.AlertReportDefinition'),
  lastTriggered: DS.attr('number'),

  /**
   * Counts of alert grouped by their status
   * Format:
   * <code>
   *   {
   *    "CRITICAL": 1,
   *    "OK": 1,
   *    "UNKNOWN": 0,
   *    "WARN": 0
   *   }
   * </code>
   * @type {object}
   */
  summary: {},

  /**
   * Formatted timestamp for latest alert triggering for current alertDefinition
   * @type {string}
   */
  lastTriggeredFormatted: function () {
    return dateUtils.dateFormat(this.get('lastTriggered'));
  }.property('lastTriggered'),

  /**
   * Status generates from child-alerts
   * Format: 1 OK / 2 WARN / 1 CRIT / 1 UNKNOWN
   * If some there are no alerts with some state, this state isn't shown
   * Order is equal to example
   * @type {string}
   */
  status: function () {
    var typeIcons = this.get('typeIcons'),
        order = this.get('order'),
        summary = this.get('summary');
    return order.map(function (state) {
      if (summary[state]) {
        return summary[state] + ' <span class="' + typeIcons[state] + ' alert-state-' + state + '"></span>';
      }
      return null;
    }).compact().join(' / ');
  }.property('summary'),

  /**
   * List of css-classes for alert types
   * @type {object}
   */
  typeIcons: {
    'OK': 'icon-ok-sign',
    'WARNING': 'icon-warning-sign',
    'CRITICAL': 'icon-remove',
    'DISABLED': 'icon-off',
    'UNKNOWN': 'icon-question-sign'
  },

  order: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN'],

  // todo: in future be mapped from server response
  description: 'Description for the Alert Definition.',
  // todo: in future be mapped from server response
  thresholds: '5-10'
});

App.AlertReportDefinition = DS.Model.extend({
  type: DS.attr('string'),
  text: DS.attr('string'),
  value: DS.attr('number')
});

App.AlertMetricsSourceDefinition = DS.Model.extend({
  propertyList: [],
  value: DS.attr('string')
});

App.AlertMetricsUriDefinition = DS.Model.extend({
  http: DS.attr('string'),
  https: DS.attr('string'),
  httpsProperty: DS.attr('string'),
  httpsPropertyValue: DS.attr('string')
});

App.PortAlertDefinition = App.AlertDefinition.extend({
  defaultPort: DS.attr('number'),
  uri: DS.attr('string')
});

App.MetricsAlertDefinition = App.AlertDefinition.extend({
  jmx: DS.belongsTo('App.AlertMetricsSourceDefinition'),
  ganglia: DS.belongsTo('App.AlertMetricsSourceDefinition'),
  uri: DS.belongsTo('App.AlertMetricsUriDefinition')
});

App.WebAlertDefinition = App.AlertDefinition.extend({
  uri: DS.belongsTo('App.AlertMetricsUriDefinition')
});

App.AggregateAlertDefinition = App.AlertDefinition.extend({
  alertName: DS.attr('string')
});

App.ScriptAlertDefinition = App.AlertDefinition.extend({
  location: DS.attr('string')
});

App.AlertDefinition.FIXTURES = [];
App.AlertReportDefinition.FIXTURES = [];
App.AlertMetricsSourceDefinition.FIXTURES = [];
App.PortAlertDefinition.FIXTURES = [];
App.AlertMetricsUriDefinition.FIXTURES = [];
App.MetricsAlertDefinition.FIXTURES = [];
App.WebAlertDefinition.FIXTURES = [];
App.AggregateAlertDefinition.FIXTURES = [];
App.ScriptAlertDefinition.FIXTURES = [];
