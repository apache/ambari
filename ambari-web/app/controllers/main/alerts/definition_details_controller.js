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
    return App.AlertInstance.find().toArray()
      .filterProperty('definitionId', this.get('content.id'));
  }.property('App.router.mainAlertInstancesController.isLoaded'),

  // stores object with editing form data (label, description, thresholds)
  editing: Em.Object.create({
    label: Em.Object.create({
      name: 'label',
      isEditing: false,
      value: '',
      originalValue: '',
      isError: false,
      bindingValue: 'content.label'
    }),
    description: Em.Object.create({
      name: 'description',
      isEditing: false,
      value: '',
      originalValue: '',
      isError: false,
      bindingValue: 'content.description'
    })
  }),

  /**
   * Validation function to define if label field populated correctly
   * @method labelValidation
   */
  labelValidation: function () {
    this.set('editing.label.isError', !this.get('editing.label.value').trim());
  }.observes('editing.label.value'),

  /**
   * Validation function to define if description field populated correctly
   * @method descriptionValidation
   */
  descriptionValidation: function () {
    this.set('editing.description.isError', !this.get('editing.description.value').trim());
  }.observes('editing.description.value'),

  /**
   * Load alert instances for current alertDefinition
   * Start updating loaded data
   * @method loadAlertInstances
   */
  loadAlertInstances: function() {
    App.router.get('mainAlertInstancesController').loadAlertInstancesByAlertDefinition(this.get('content.id'));
    App.router.set('mainAlertInstancesController.isUpdating', true);
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
   * Save button handler, could save label/description/thresholds of alert definition
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
    data.set(property_name,  element.get('value'));
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
   * "Delete" button handler
   * @param {object} event
   * @method deleteAlertDefinition
   */
  deleteAlertDefinition: function(event) {
    var alertDefinition = this.get('content');
    var self = this;
    App.showConfirmationPopup(function() {
      App.ajax.send({
        name : 'alerts.delete_alert_definition',
        sender : self,
        success : 'deleteAlertDefinitionSuccess',
        error : 'deleteAlertDefinitionError',
        data : {
          id : alertDefinition.get('id')
        }
      });
    }, null, function() {
    });
  },

  /**
   * Success-callback for <code>deleteAlertDefinition</code>
   * @method deleteAlertDefinitionSuccess
   */
  deleteAlertDefinitionSuccess : function() {
    App.router.transitionTo('main.alerts.index');
  },

  /**
   * Error-callback for <code>deleteAlertDefinition</code>
   * @method deleteAlertDefinitionError
   */
  deleteAlertDefinitionError : function(xhr, textStatus, errorThrown, opt) {
    console.log(textStatus);
    console.log(errorThrown);
    xhr.responseText = "{\"message\": \"" + xhr.statusText + "\"}";
    App.ajax.defaultErrorHandler(xhr, opt.url, 'DELETE', xhr.status);
  },

  /**
   * "Disable / Enable" button handler
   * @returns {$.ajax}
   * @method toggleState
   */
  toggleState: function() {
    var alertDefinition = this.get('content');
    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: alertDefinition.get('id'),
        data: {
          "AlertDefinition/enabled": !alertDefinition.get('enabled')
        }
      }
    });
  },

  /**
   * Router transition to host level alerts page
   * @param {object} event
   * @method goToHostAlerts
   */
  goToHostAlerts: function (event) {
    if (event && event.context) {
      App.router.transitionTo('main.hosts.hostDetails.alerts', event.context);
    }
  }
});