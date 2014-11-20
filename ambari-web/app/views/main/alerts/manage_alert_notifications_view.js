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

App.ManageAlertNotificationsView = Em.View.extend({

  templateName: require('templates/main/alerts/manage_alert_notifications_popup'),

  selectedAlertNotification: null,

  selectedAlertNotificationGroups: function () {
    //TODO: Implement binding to AlertGroups
    return ['Group1', 'Group2'].join(', ');
  }.property('controller.selectedAlertNotification'),

  isEditButtonDisabled: true,

  isRemoveButtonDisabled: true,

  isDuplicateButtonDisabled: true,

  showEmailDetails: function () {
    return this.get('controller.selectedAlertNotification.type') === 'EMAIL';
  }.property('controller.selectedAlertNotification.type'),

  showSNMPDetails: function () {
    return this.get('controller.selectedAlertNotification.type') === 'SNMP';
  }.property('controller.selectedAlertNotification.type'),

  buttonObserver: function () {
    var selectedAlertNotification = this.get('controller.selectedAlertNotification');
    if (selectedAlertNotification) {
      this.set('isEditButtonDisabled', false);
      this.set('isRemoveButtonDisabled', false);
      this.set('isDuplicateButtonDisabled', false);
    } else {
      this.set('isEditButtonDisabled', true);
      this.set('isRemoveButtonDisabled', true);
      this.set('isDuplicateButtonDisabled', true);
    }
  }.observes('controller.selectedAlertNotification'),

  onAlertNotificationSelect: function () {
    var selectedAlertNotification = this.get('selectedAlertNotification');
    var length = selectedAlertNotification.length;
    if (selectedAlertNotification && length) {
      this.set('controller.selectedAlertNotification', selectedAlertNotification[length - 1]);
    }
    if (selectedAlertNotification && length > 1) {
      this.set('selectedAlertNotification', selectedAlertNotification[length - 1]);
    }
  }.observes('selectedAlertNotification'),

  onLoad: function () {
    if (this.get('controller.isLoaded')) {
      var notifications = this.get('controller.alertNotifications');
      if (notifications && notifications.length) {
        this.set('selectedAlertNotification', notifications[0]);
      }  else {
        this.set('selectedAlertNotification', null);
      }
      Em.run.later(this, function () {
        App.tooltip(this.$("[rel='button-info']"));
        App.tooltip(this.$("[rel='button-info-dropdown']"), {placement: 'left'});
      }, 50) ;
    }
  }.observes('controller.isLoaded'),

  willInsertElement: function () {
    this.get('controller').loadAlertNotifications();
  },

  didInsertElement: function () {
    this.onLoad();
  },

  errorMessage: function () {
    return this.get('controller.errorMessage');
  }.property('controller.errorMessage')

});
