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

App.MainAlertDefinitionDetailsController = Em.Controller.extend({

  name: 'mainAlertDefinitionDetailsController',

  alerts: function () {
    return App.AlertInstanceLocal.find().toArray()
        .filterProperty('definitionId', this.get('content.id'));
  }.property('App.router.mainAlertInstancesController.isLoaded', 'App.router.mainAlertInstancesController.reload'),

  // stores object with editing form data (label)
  editing: Em.Object.create({
    label: Em.Object.create({
      name: 'label',
      isEditing: false,
      value: '',
      originalValue: '',
      isError: false,
      bindingValue: 'content.label'
    })
  }),

  /**
   * Host to count of alerts on this host during last day map
   * @type {Object}
   */
  lastDayAlertsCount: null,

  /**
   * Define if let user leave the page
   * @type {Boolean}
   */
  forceTransition: false,

  /**
   * List of all group names related to alert definition
   * @type {Array}
   */
  groupsList: function () {
    return this.get('content.groups').mapProperty('displayName');
  }.property('content.groups.@each'),

  /**
   * Validation function to define if label field populated correctly
   * @method labelValidation
   */
  labelValidation: function () {
    this.set('editing.label.isError', !this.get('editing.label.value').trim());
  }.observes('editing.label.value'),

  /**
   * Set init values for variables
   */
  clearStep: function () {
    var editing = this.get('editing');
    Em.keys(editing).forEach(function (key) {
      editing.get(key).set('isEditing', false);
    });
  },

  /**
   * Load alert instances for current alertDefinition
   * Start updating loaded data
   * @method loadAlertInstances
   */
  loadAlertInstances: function () {
    App.router.get('mainAlertInstancesController').loadAlertInstancesByAlertDefinition(this.get('content.id'));
    App.router.set('mainAlertInstancesController.isUpdating', true);
    this.loadAlertInstancesHistory();
  },

  /**
   * Load alert instances history data
   * used to count instances number of the last 24 hour
   * @method loadAlertInstancesHistory
   */
  loadAlertInstancesHistory: function () {
    this.set('lastDayAlertsCount', null);
    return App.ajax.send({
      name: 'alerts.get_instances_history',
      sender: this,
      data: {
        definitionName: this.get('content.name'),
        timestamp: App.dateTime() - 86400000 // timestamp for time 24-hours ago
      },
      success: 'loadAlertInstancesHistorySuccess'
    });
  },

  /**
   * Success-callback for <code>loadAlertInstancesHistory</code>
   */
  loadAlertInstancesHistorySuccess: function (data) {
    var lastDayAlertsCount = {};
    data.items.forEach(function (alert) {
      if (!lastDayAlertsCount[alert.AlertHistory.host_name]) {
        lastDayAlertsCount[alert.AlertHistory.host_name] = 1;
      } else {
        lastDayAlertsCount[alert.AlertHistory.host_name] += 1;
      }
    });
    this.set('lastDayAlertsCount', lastDayAlertsCount);
  },

  /**
   * Edit button handler
   * @param {object} event
   * @method edit
   */
  edit: function (event) {
    var element = event.context;
    var value = this.get(element.get('bindingValue'));
    element.set('originalValue', value);
    element.set('value', value);
    element.set('isEditing', true);
  },

  /**
   * Cancel button handler
   * @param {object} event
   * @method cancelEdit
   */
  cancelEdit: function (event) {
    var element = event.context;
    element.set('value', element.get('originalValue'));
    element.set('isEditing', false);
  },

  /**
   * Save button handler, could save label of alert definition
   * @param {object} event
   * @returns {$.ajax}
   * @method saveEdit
   */
  saveEdit: function (event) {
    var element = event.context;
    this.set(element.get('bindingValue'), element.get('value'));
    element.set('isEditing', false);

    var data = Em.Object.create({});
    var property_name = "AlertDefinition/" + element.get('name');
    data.set(property_name, element.get('value'));
    var alertDefinition_id = this.get('content.id');
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinition_id,
        data: data
      }
    });
  },

  /**
   * Onclick handler for save button on Save popup
   * Save changes of label and configs
   */
  saveLabelAndConfigs: function () {
    var configsController = App.router.get('mainAlertDefinitionConfigsController');
    if (configsController.get('canEdit')) {
      configsController.saveConfigs();
    }
    if (this.get('editing.label.isEditing')) {
      this.saveEdit({
        context: this.get('editing.label')
      });
    }
  },

  /**
   * "Delete" button handler
   * @param {object} event
   * @method deleteAlertDefinition
   */
  deleteAlertDefinition: function (event) {
    var alertDefinition = this.get('content');
    var self = this;
    App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'alerts.delete_alert_definition',
        sender: self,
        success: 'deleteAlertDefinitionSuccess',
        error: 'deleteAlertDefinitionError',
        data: {
          id: alertDefinition.get('id')
        }
      });
    }, null, function () {
    });
  },

  /**
   * Success-callback for <code>deleteAlertDefinition</code>
   * @method deleteAlertDefinitionSuccess
   */
  deleteAlertDefinitionSuccess: function () {
    App.router.transitionTo('main.alerts.index');
  },

  /**
   * Error-callback for <code>deleteAlertDefinition</code>
   * @method deleteAlertDefinitionError
   */
  deleteAlertDefinitionError: function (xhr, textStatus, errorThrown, opt) {
    console.log(textStatus);
    console.log(errorThrown);
    xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
    App.ajax.defaultErrorHandler(xhr, opt.url, 'DELETE', xhr.status);
  },

  /**
   * "Disable / Enable" button handler
   * @method toggleState
   */
  toggleState: function () {
    var alertDefinition = this.get('content');
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.msg') : Em.I18n.t('alerts.table.state.disabled.confirm.msg'),
      confirmButton: alertDefinition.get('enabled') ? Em.I18n.t('alerts.table.state.enabled.confirm.btn') : Em.I18n.t('alerts.table.state.disabled.confirm.btn')
    });

    return App.showConfirmationFeedBackPopup(function (query) {
      self.toggleDefinitionState(alertDefinition);
    }, bodyMessage);
  },

  /**
   * Enable/disable alertDefinition
   * @param {object} alertDefinition
   * @returns {$.ajax}
   * @method toggleDefinitionState
   */
  toggleDefinitionState: function (alertDefinition) {
    var newState = !alertDefinition.get('enabled');
    alertDefinition.set('enabled', newState);
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinition.get('id'),
        data: {
          "AlertDefinition/enabled": newState
        }
      }
    });
  },

  /**
   * Define if label or configs are in edit mode
   * @type {Boolean}
   */
  isEditing: function () {
    return this.get('editing.label.isEditing') || App.router.get('mainAlertDefinitionConfigsController.canEdit');
  }.property('editing.label.isEditing', 'App.router.mainAlertDefinitionConfigsController.canEdit'),

  /**
   * If some configs or label are changed and user navigates away, show this popup with propose to save changes
   * @param {String} path
   * @method showSavePopup
   */
  showSavePopup: function (path) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      bodyClass: Em.View.extend({
        template: Ember.Handlebars.compile('{{t alerts.saveChanges}}')
      }),
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.discard'),
      third: Em.I18n.t('common.cancel'),
      disablePrimary: function () {
        return App.router.get('mainAlertDefinitionDetailsController.editing.label.isError') || App.router.get('mainAlertDefinitionConfigsController.hasErrors');
      }.property('App.router.mainAlertDefinitionDetailsController.editing.label.isError', 'App.router.mainAlertDefinitionConfigsController.hasErrors'),
      onPrimary: function () {
        self.saveLabelAndConfigs();
        self.set('forceTransition', true);
        App.router.route(path);
        this.hide();
      },
      onSecondary: function () {
        self.set('forceTransition', true);
        App.router.route(path);
        this.hide();
      },
      onThird: function () {
        this.hide();
      }
    });
  }

});