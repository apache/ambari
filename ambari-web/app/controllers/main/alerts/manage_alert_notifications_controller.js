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
var validator = require('utils/validator');

App.ManageAlertNotificationsController = Em.Controller.extend({

  name: 'manageAlertNotificationsController',

  /**
   * Are alert notifications loaded
   * @type {boolean}
   */
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
    },
    customProperties: Em.A([])
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
    return this.get('isLoaded') ? App.AlertNotification.find().toArray() : [];
  }.property('isLoaded'),

  /**
   * Selected Alert Notification
   * @type {App.AlertNotification}
   */
  selectedAlertNotification: null,

  /**
   * Addable to <code>selectedAlertNotification.properties</code> custom property
   * @type {{name: string, value: string}}
   */
  newCustomProperty: {name: '', value: ''},

  /**
   * List custom property names that shouldn't be displayed on Edit page
   * @type {string[]}
   */
  ignoredCustomProperties: ['ambari.dispatch.recipients'],

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
   * @method addAlertNotification
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
   * @method editAlertNotification
   */
  editAlertNotification: function () {
    this.fillEditCreateInputs();
    this.showCreateEditPopup(true);
  },

  /**
   * Fill inputs of Create/Edit popup form
   * @param addCopyToName define whether add 'Copy of ' to name
   * @method fillEditCreateInputs
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
    inputFields.get('customProperties').clear();
    var properties = selectedAlertNotification.get('properties');
    var ignoredCustomProperties = this.get('ignoredCustomProperties');
    Em.keys(properties).forEach(function (k) {
      if (ignoredCustomProperties.contains(k)) return;
      inputFields.get('customProperties').pushObject({
        name: k,
        value: properties[k],
        defaultValue: properties[k]
      });
    });
  },

  /**
   * Show Edit or Create Notification popup
   * @param {boolean} isEdit true - edit, false - create
   * @returns {App.ModalPopup}
   * @method showCreateEditPopup
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
   * @method formatNotificationAPIObject
   */
  formatNotificationAPIObject: function () {
    var inputFields = this.get('inputFields');
    var alertStates = [];
    var properties = {};
    if (inputFields.get('severityFilter.value')[0]) {
      alertStates.push('OK');
    }
    if (inputFields.get('severityFilter.value')[1]) {
      alertStates.push('WARNING');
    }
    if (inputFields.get('severityFilter.value')[2]) {
      alertStates.push('CRITICAL');
    }
    if (inputFields.get('severityFilter.value')[3]) {
      alertStates.push('UNKNOWN');
    }
    if (inputFields.get('method.value') === 'EMAIL') {
      properties['ambari.dispatch.recipients'] = inputFields.get('email.value').replace(/\s/g, '').split(',');
    }
    inputFields.get('customProperties').forEach(function (customProperty) {
      properties[customProperty.name] = customProperty.value;
    });
    return {
      AlertTarget: {
        name: inputFields.get('name.value'),
        description: inputFields.get('description.value'),
        notification_type: inputFields.get('method.value'),
        alert_states: alertStates,
        properties: properties
      }
    };
  },

  /**
   * Send request to server to create Alert Notification
   * @param {object} apiObject (@see formatNotificationAPIObject)
   * @returns {$.ajax}
   * @method createAlertNotification
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
   * @method createAlertNotificationSuccessCallback
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
   * @param {object} apiObject (@see formatNotificationAPIObject)
   * @returns {$.ajax}
   * @method updateAlertNotification
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
   * @method updateAlertNotificationSuccessCallback
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
   * @return {App.ModalPopup}
   * @method deleteAlertNotification
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
   * @method deleteAlertNotificationSuccessCallback
   */
  deleteAlertNotificationSuccessCallback: function () {
    this.loadAlertNotifications();
    var selectedAlertNotification = this.get('selectedAlertNotification');
    selectedAlertNotification.deleteRecord();
    this.set('selectedAlertNotification', null);
  },

  /**
   * Duplicate Notification button handler
   * @method duplicateAlertNotification
   */
  duplicateAlertNotification: function () {
    this.fillEditCreateInputs(true);
    this.showCreateEditPopup();
  },

  /**
   * Show popup with form for new custom property
   * @method addCustomPropertyHandler
   * @return {App.ModalPopup}
   */
  addCustomPropertyHandler: function () {
    var self = this;

    return App.ModalPopup.show({

      header: Em.I18n.t('alerts.notifications.addCustomPropertyPopup.header'),

      primary: Em.I18n.t('common.add'),

      bodyClass: Em.View.extend({

        /**
         * If some error with new custom property
         * @type {boolean}
         */
        isError: false,

        controller: this,

        /**
         * Error message for new custom property (invalid name, existed name etc)
         * @type {string}
         */
        errorMessage: '',

        /**
         * Check new custom property for errors with its name
         * @method errorHandler
         */
        errorsHandler: function () {
          var name = this.get('controller.newCustomProperty.name');
          var flag = validator.isValidConfigKey(name);
          if (flag) {
            if (this.get('controller.inputFields.customProperties').mapProperty('name').contains(name) ||
              this.get('controller.ignoredCustomProperties').contains(name)) {
              this.set('errorMessage', Em.I18n.t('alerts.notifications.addCustomPropertyPopup.error.propertyExists'));
              flag = false;
            }
          }
          else {
            this.set('errorMessage', Em.I18n.t('alerts.notifications.addCustomPropertyPopup.error.invalidPropertyName'));
          }
          this.set('isError', !flag);
          this.set('parentView.disablePrimary', !flag);
        }.observes('controller.newCustomProperty.name'),

        templateName: require('templates/main/alerts/add_custom_config_to_alert_notification_popup')
      }),

      onPrimary: function () {
        self.addCustomProperty();
        self.set('newCustomProperty', {name: '', value: ''}); // cleanup
        this.hide();
      }

    });
  },

  /**
   * Add Custom Property to <code>selectedAlertNotification</code> (push it to <code>properties</code>-field)
   * @method addCustomProperty
   */
  addCustomProperty: function () {
    var newCustomProperty = this.get('newCustomProperty');
    this.get('inputFields.customProperties').pushObject({
      name: newCustomProperty.name,
      value: newCustomProperty.value,
      defaultValue: newCustomProperty.value
    });
  },

  /**
   * "Remove"-button click handler
   * Delete customProperty from <code>inputFields.customProperties</code>
   * @param {object} e
   * @method removeCustomProperty
   */
  removeCustomPropertyHandler: function (e) {
    var customProperties = this.get('inputFields.customProperties');
    this.set('inputFields.customProperties', customProperties.without(e.context));
  }

});
