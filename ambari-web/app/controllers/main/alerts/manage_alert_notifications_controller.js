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
   * Create/Edit modal popup object
   * used to hide popup
   * @type {App.ModalPopup}
   */
  createEditPopup: null,

  /**
   * Map of edit inputs shown in Create/Edit Notification popup
   * @type {Object}
   */
  inputFields: Em.Object.create({
    name: {
      label: Em.I18n.t('common.name'),
      value: '',
      defaultValue: ''
    },
    groups: {
      label: Em.I18n.t('common.groups'),
      value: '',
      defaultValue: ''
    },
    method: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.method'),
      value: '',
      defaultValue: ''
    },
    email: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.email'),
      value: '',
      defaultValue: ''
    },
    severityFilter: {
      label: Em.I18n.t('alerts.actions.manage_alert_notifications_popup.severityFilter'),
      value: [true, true, true, true],
      defaultValue: [true, true, true, true]
    },
    description: {
      label: Em.I18n.t('common.description'),
      value: '',
      defaultValue: ''
    }
  }),

  /**
   * List of available Notification types
   * used in Type combobox
   * @type {Array}
   */
  methods: ['EMAIL', 'SNMP'],

  /**
   * List of all Alert Notifications
   * @type {App.AlertNotification[]}
   */
  alertNotifications: function () {
    if (this.get('isLoaded')) {
      return App.AlertNotification.find().toArray();
    }
    return [];
  }.property('isLoaded'),

  /**
   * Selected Alert Notification
   * @type {App.AlertNotification}
   */
  selectedAlertNotification: null,

  /**
   * Load all Alert Notifications from server
   * Don't do anything if controller not isLoaded
   * @returns {$.ajax|null}
   */
  loadAlertNotifications: function () {
    this.set('isLoaded', false);
    return App.ajax.send({
      name: 'alerts.notifications',
      sender: this,
      success: 'getAlertNotificationsSuccessCallback',
      error: 'getAlertNotificationsErrorCallback'
    });
  },

  /**
   * Success-callback for load alert notifications request
   * @param {object} json
   * @method getAlertNotificationsSuccessCallback
   */
  getAlertNotificationsSuccessCallback: function (json) {
    App.alertNotificationMapper.map(json);
    this.set('isLoaded', true);
  },

  /**
   * Error-callback for load alert notifications request
   * @method getAlertNotificationsErrorCallback
   */
  getAlertNotificationsErrorCallback: function () {
    this.set('isLoaded', true);
  },

  /**
   * Add Notification button handler
   */
  addAlertNotification: function () {
    var inputFields = this.get('inputFields');
    Em.keys(inputFields).forEach(function (key) {
      inputFields.set(key + '.value', inputFields.get(key + '.defaultValue'));
    });
    this.showCreateEditPopup(false);
  },

  /**
   * Edit Notification button handler
   */
  editAlertNotification: function () {
    this.fillEditCreateInputs();
    this.showCreateEditPopup(true);
  },

  /**
   * Fill inputs of Create/Edit popup form
   * @param addCopyToName define whether add 'Copy of ' to name
   */
  fillEditCreateInputs: function (addCopyToName) {
    var inputFields = this.get('inputFields');
    var selectedAlertNotification = this.get('selectedAlertNotification');
    inputFields.set('name.value', (addCopyToName ? 'Copy of ' : '') + selectedAlertNotification.get('name'));
    inputFields.set('email.value', selectedAlertNotification.get('properties')['ambari.dispatch.recipients'] ?
        selectedAlertNotification.get('properties')['ambari.dispatch.recipients'].join(', ') : '');
    inputFields.set('severityFilter.value', [
      selectedAlertNotification.get('alertStates').contains('OK'),
      selectedAlertNotification.get('alertStates').contains('WARNING'),
      selectedAlertNotification.get('alertStates').contains('CRITICAL'),
      selectedAlertNotification.get('alertStates').contains('UNKNOWN')
    ]);
    inputFields.set('description.value', selectedAlertNotification.get('description'));
    inputFields.set('method.value', selectedAlertNotification.get('type'));
  },

  /**
   * Show Edit or Create Notification popup
   * @param isEdit
   * @returns {App.ModalPopup}
   */
  showCreateEditPopup: function (isEdit) {
    var self = this;
    var createEditPopup = App.ModalPopup.show({
      header: isEdit ? Em.I18n.t('alerts.actions.manage_alert_notifications_popup.editHeader') : Em.I18n.t('alerts.actions.manage_alert_notifications_popup.addHeader'),
      bodyClass: Em.View.extend({
        controller: this,
        templateName: require('templates/main/alerts/create_alert_notification'),
        isEmailMethodSelected: function () {
          return this.get('controller.inputFields.method.value') === 'EMAIL';
        }.property('controller.inputFields.method.value')
      }),
      primary: Em.I18n.t('common.save'),
      onPrimary: function () {
        this.set('disablePrimary', true);
        var apiObject = self.formatNotificationAPIObject();
        if (isEdit) {
          self.updateAlertNotification(apiObject);
        } else {
          self.createAlertNotification(apiObject);
        }
      }
    });
    this.set('createEditPopup', createEditPopup);
    return createEditPopup;
  },

  /**
   * Create API-formatted object from data populate by user
   * @returns {Object}
   */
  formatNotificationAPIObject: function () {
    var inputFields = this.get('inputFields');
    var alertStates = [];
    var properties = {};
    if (inputFields.severityFilter.value[0]) {
      alertStates.push('OK');
    }
    if (inputFields.severityFilter.value[1]) {
      alertStates.push('WARNING');
    }
    if (inputFields.severityFilter.value[2]) {
      alertStates.push('CRITICAL');
    }
    if (inputFields.severityFilter.value[3]) {
      alertStates.push('UNKNOWN');
    }
    if (inputFields.method.value === 'EMAIL') {
      properties['ambari.dispatch.recipients'] = inputFields.email.value.replace(/\s/g, '').split(',');
    }
    return {
      AlertTarget: {
        name: inputFields.name.value,
        description: inputFields.description.value,
        notification_type: inputFields.method.value,
        alert_states: alertStates,
        properties: properties
      }
    };
  },

  /**
   * Send request to server to create Alert Notification
   * @param apiObject
   * @returns {$.ajax}
   */
  createAlertNotification: function (apiObject) {
    return App.ajax.send({
      name: 'alerts.create_alert_notification',
      sender: this,
      data: {
        data: apiObject
      },
      success: 'createAlertNotificationSuccessCallback'
    });
  },

  /**
   * Success callback for <code>createAlertNotification</code>
   */
  createAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var createEditPopup = this.get('createEditPopup');
    if (createEditPopup) {
      createEditPopup.hide();
    }
  },

  /**
   * Send request to server to update Alert Notification
   * @param apiObject
   * @returns {$.ajax}
   */
  updateAlertNotification: function (apiObject) {
    return App.ajax.send({
      name: 'alerts.update_alert_notification',
      sender: this,
      data: {
        data: apiObject,
        id: this.get('selectedAlertNotification.id')
      },
      success: 'updateAlertNotificationSuccessCallback'
    });
  },

  /**
   * Success callback for <code>updateAlertNotification</code>
   */
  updateAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var createEditPopup = this.get('createEditPopup');
    if (createEditPopup) {
      createEditPopup.hide();
    }
  },

  /**
   * Delete Notification button handler
   */
  deleteAlertNotification: function () {
    var self = this;
    return App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'alerts.delete_alert_notification',
        sender: self,
        data: {
          id: self.get('selectedAlertNotification.id')
        },
        success: 'deleteAlertNotificationSuccessCallback'
      });
    });
  },

  /**
   * Success callback for <code>deleteAlertNotification</code>
   */
  deleteAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var selectedAlertNotification = this.get('selectedAlertNotification');
    selectedAlertNotification.deleteRecord();
    this.set('selectedAlertNotification', null);
  },

  /**
   * Duplicate Notification button handler
   */
  duplicateAlertNotification: function () {
    this.fillEditCreateInputs(true);
    this.showCreateEditPopup();
  }

});
