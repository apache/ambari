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

  /**
   * @type {App.AlertNotification}
   */
  selectedAlertNotification: null,

  selectedAlertNotificationGroups: function () {
    return this.get('controller.selectedAlertNotification.groups').toArray().mapProperty('displayName').join(', ');
  }.property('controller.selectedAlertNotification', 'controller.selectedAlertNotification.groups.@each', 'controller.isLoaded'),

  /**
   * @type {boolean}
   */
  isAddButtonDisabled: true,

  /**
   * @type {boolean}
   */
  isEditButtonDisabled: true,

  /**
   * @type {boolean}
   */
  isRemoveButtonDisabled: true,

  /**
   * @type {boolean}
   */
  isDuplicateButtonDisabled: true,

  /**
   * Show EMAIL information if selected alert notification has type EMAIL
   * @type {boolean}
   */
  showEmailDetails: Em.computed.equal('controller.selectedAlertNotification.type', 'EMAIL'),

  /**
   * Show SNMP information if selected alert notification has type SNMP
   * @type {boolean}
   */
  showSNMPDetails: Em.computed.equal('controller.selectedAlertNotification.type', 'SNMP'),

  email: function () {
    return this.get('controller.selectedAlertNotification.properties')['ambari.dispatch.recipients'];
  }.property('controller.selectedAlertNotification.properties'),

  /**
   * @type {string}
   */
  severities: function () {
    return this.get('controller.selectedAlertNotification.alertStates').join(', ');
  }.property('controller.selectedAlertNotification.alertStates'),

  /**
   * Enable/disable "edit"/"remove"/"duplicate" buttons basing on <code>controller.selectedAlertNotification</code>
   * @method buttonObserver
   */
  buttonObserver: function () {
    var selectedAlertNotification = this.get('controller.selectedAlertNotification');
    this.set('isAddButtonDisabled', (!selectedAlertNotification || App.isOperator));
    this.set('isEditButtonDisabled', (!selectedAlertNotification || App.isOperator));
    this.set('isRemoveButtonDisabled', (!selectedAlertNotification || App.isOperator));
    this.set('isDuplicateButtonDisabled', (!selectedAlertNotification || App.isOperator));
  }.observes('controller.selectedAlertNotification'),

  /**
   * Prevent user select more than 1 alert notification
   * @method onAlertNotificationSelect
   */
  onAlertNotificationSelect: function () {
    var selectedAlertNotification = this.get('selectedAlertNotification');
    if (selectedAlertNotification && selectedAlertNotification.length) {
      this.set('controller.selectedAlertNotification', selectedAlertNotification[selectedAlertNotification.length - 1]);
    }
    if (selectedAlertNotification && selectedAlertNotification.length > 1) {
      this.set('selectedAlertNotification', selectedAlertNotification[selectedAlertNotification.length - 1]);
    }
  }.observes('selectedAlertNotification'),

  /**
   * Set first alert notification as selected (if they are already loaded)
   * Add some tooltips on manage buttons
   * @method onLoad
   */
  onLoad: function () {
    if (this.get('controller.isLoaded')) {
      var notifications = this.get('controller.alertNotifications');
      if (notifications && notifications.length) {
        this.set('selectedAlertNotification', this.get('controller.selectedAlertNotification') || notifications[0]);
        this.buttonObserver();
      } else {
        if (!App.isOperator) {
          this.set('isAddButtonDisabled', false);
        }
        this.set('selectedAlertNotification', null);
      }
      Em.run.later(this, function () {
        App.tooltip(this.$("[rel='button-info']"));
        App.tooltip(this.$("[rel='button-info-dropdown']"), {placement: 'left'});
      }, 50);
    }
  }.observes('controller.isLoaded'),

  willInsertElement: function () {
    this.get('controller').loadAlertNotifications();
  },

  didInsertElement: function () {
    this.onLoad();
  }

});
