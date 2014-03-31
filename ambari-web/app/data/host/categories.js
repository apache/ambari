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
module.exports = [
  {
    value: Em.I18n.t('common.all'),
    isHealthStatus: true,
    healthStatusValue: '',
    isActive: true,
    isVisible: false
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.green'),
    isHealthStatus: true,
    class: App.healthIconClassGreen,
    healthStatusValue: 'health-status-LIVE',
    observes: 'view.content.@each.healthClass'
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.red'),
    isHealthStatus: true,
    class: App.healthIconClassRed,
    healthStatusValue: 'health-status-DEAD-RED',
    observes: 'view.content.@each.healthClass'
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.orange'),
    isHealthStatus: true,
    class: App.healthIconClassOrange,
    healthStatusValue: 'health-status-DEAD-ORANGE',
    observes: 'view.content.@each.healthClass'
  },
  {
    value: Em.I18n.t('hosts.host.healthStatusCategory.yellow'),
    isHealthStatus: true,
    class: App.healthIconClassYellow,
    healthStatusValue: 'health-status-DEAD-YELLOW',
    observes: 'view.content.@each.healthClass'
  },
  {
    value: Em.I18n.t('hosts.host.alerts.label'),
    hostProperty: 'criticalAlertsCount',
    class: 'icon-exclamation-sign',
    isHealthStatus: false,
    healthStatusValue: 'health-status-WITH-ALERTS',
    column: 7,
    type: 'number',
    filterValue: '>0',
    observes: 'view.content.@each.criticalAlertsCount'
  },
  {
    value: Em.I18n.t('common.restart'),
    hostProperty: 'componentsWithStaleConfigsCount',
    class: 'icon-refresh',
    isHealthStatus: false,
    healthStatusValue: 'health-status-RESTART',
    column: 8,
    type: 'number',
    filterValue: '>0',
    observes: 'view.content.@each.componentsWithStaleConfigsCount'
  },
  {
    value: Em.I18n.t('common.selected'),
    hostProperty: 'selected',
    class: '',
    isHealthStatus: false,
    healthStatusValue: 'health-status-SELECTED',
    selected: true,
    column: 10,
    type: 'boolean',
    filterValue: true,
    isVisible: false,
    observes: 'view.content.@each.selected'
  },
  {
    value: Em.I18n.t('common.passive_state'),
    hostProperty: 'componentsInPassiveStateCount',
    class: 'passive-state icon-medkit',
    isHealthStatus: false,
    healthStatusValue: 'health-status-PASSIVE_STATE',
    column: 9,
    type: 'number',
    filterValue: '>0',
    observes: 'view.content.@each.componentsInPassiveStateCount'
  }
];
