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

App.MainAlertInstancesController = Em.Controller.extend({
  name: 'mainAlertInstancesController',
  isLoaded: false,
  /**
   * Causes automatic updates of content if set to true
   */
  isUpdating: false,
  updateTimer: null,
  updateInterval: App.alertInstancesUpdateInterval,
  /**
   * "HOST" or "ALERT_DEFINITION"
   */
  sourceType: null,
  sourceName: null,

  fetchAlertInstances: function () {
    switch (this.get('sourceType')) {
      case 'HOST':
        App.ajax.send({
          name: 'alerts.instances.by_host',
          sender: this,
          data: {
            clusterName: App.router.get('clusterName'),
            hostName: this.get('sourceName')
          },
          success: 'getAlertInstancesSuccessCallback',
          error: 'getAlertInstancesErrorCallback'
        });
        break;
      case 'ALERT_DEFINITION':
        App.ajax.send({
          name: 'alerts.instances.by_definition',
          sender: this,
          data: {
            clusterName: App.router.get('clusterName'),
            definitionId: this.get('sourceName')
          },
          success: 'getAlertInstancesSuccessCallback',
          error: 'getAlertInstancesErrorCallback'
        });
        break;
      default:
        App.ajax.send({
          name: 'alerts.instances',
          sender: this,
          data: {
            clusterName: App.router.get('clusterName')
          },
          success: 'getAlertInstancesSuccessCallback',
          error: 'getAlertInstancesErrorCallback'
        });
        break;
    }
  },

  loadAlertInstances: function () {
    this.set('isLoaded', false);
    this.set('sourceType', null);
    this.set('sourceName', null);
    this.fetchAlertInstances();
  },

  loadAlertInstancesByHost: function (hostName) {
    this.set('isLoaded', false);
    this.set('sourceType', 'HOST');
    this.set('sourceName', hostName);
    this.fetchAlertInstances();
  },

  loadAlertInstancesByAlertDefinition: function (definitionName) {
    this.set('isLoaded', false);
    this.set('sourceType', 'ALERT_DEFINITION');
    this.set('sourceName', definitionName);
    this.fetchAlertInstances();
  },

  scheduleUpdate: function () {
    var self = this;
    if (this.get('isUpdating')) {
      this.set('updateTimer', setTimeout(function () {
        self.fetchAlertInstances();
        self.scheduleUpdate();
      }, this.get('updateInterval')));
    } else {
      clearTimeout(this.get('updateTimer'));
    }
  }.observes('isUpdating'),

  getAlertInstancesSuccessCallback: function (json) {
    App.alertInstanceMapper.map(json);
    this.set('isLoaded', true);
  },

  getAlertInstancesErrorCallback: function () {
    this.set('isLoaded', true);
  }
});
