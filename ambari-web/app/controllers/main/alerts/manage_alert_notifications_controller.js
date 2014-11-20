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

App.ManageAlertNotificationsController = Em.Controller.extend({
  name: 'manageAlertNotificationsController',
  isLoaded: false,

  /**
   * @type {App.AlertNotification[]}
   */
  alertNotifications: function () {
    if (this.get('isLoaded')) {
      return App.AlertNotification.find().toArray();
    }
    return [];
  }.property('isLoaded'),

  /**
   * @type {App.AlertNotification}
   */
  selectedAlertNotification: null,

  loadAlertNotifications: function () {
    if (this.get('isLoaded')) {
      return;
    }
    App.ajax.send({
      name: 'alerts.notifications',
      sender: this,
      data: {},
      success: 'getAlertNotificationsSuccessCallback',
      error: 'getAlertNotificationsErrorCallback'
    });
  },

  getAlertNotificationsSuccessCallback: function (json) {
    App.alertNotificationMapper.map(json);
    this.set('isLoaded', true);
  },

  getAlertNotificationsErrorCallback: function () {
    this.set('isLoaded', true);
  },

  addAlertNotification: Em.K,
  deleteAlertNotification: Em.K,
  editAlertNotification: Em.K,
  duplicateAlertNotification: Em.K
});
