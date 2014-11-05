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

App.MainAlertDefinitionDetailsView = App.TableView.extend({

  templateName: require('templates/main/alerts/definition_details'),

  didInsertElement: function () {
    this.filter();
  },

  content: function () {
    // todo: replace with this.get('controller.content.alerts') when this relationship will be provided
    return App.AlertInstance.find().toArray();
  }.property(),

  // stores object with editing form data (label, description, thresholds)
  editing: Em.Object.create({
    label: Em.Object.create({
      isEditing: false,
      value: '',
      originalValue: '',
      isError: false,
      bindingValue: 'controller.content.label'
    }),
    description: Em.Object.create({
      isEditing: false,
      value: '',
      originalValue: '',
      isError: false,
      bindingValue: 'controller.content.description'
    }),
    thresholds: Em.Object.create({
      isEditing: false,
      value: '',
      originalValue: '',
      isError: false,
      bindingValue: 'controller.content.thresholds'
    })
  }),

  /**
   * Validation function to define if label field populated correctly
   */
  labelValidation: function () {
    this.set('editing.label.isError', !this.get('editing.label.value').trim());
  }.observes('editing.label.value'),

  /**
   * Validation function to define if description field populated correctly
   */
  descriptionValidation: function () {
    this.set('editing.description.isError', !this.get('editing.description.value').trim());
  }.observes('editing.description.value'),

  /**
   * Validation function to define if thresholds field populated correctly
   */
  thresholsdValidation: function () {
    this.set('editing.thresholds.isError', !this.get('editing.thresholds.value').trim());
  }.observes('editing.thresholds.value'),

  /**
   * Edit button handler
   * @param event
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
   * @param event
   */
  cancelEdit: function (event) {
    var element = event.context;
    element.set('value', element.get('originalValue'));
    element.set('isEditing', false);
  },

  /**
   * Save button handler
   * @param event
   */
  saveEdit: function (event) {
    var element = event.context;
    // todo: add request to the server to save new value
    this.set(element.get('bindingValue'), element.get('value'));
    element.set('isEditing', false);
  },

  /**
   * "Delete" button handler
   * @param event
   */
  deleteAlertDefinition: function (event) {
    // todo: provide deleting of alert definition
  },

  /**
   * "Disable / Enable" button handler
   * @param event
   */
  disableEnableAlertDefinition: function (event) {
    // todo: provide disabling/enabling of alert definition
  },

  /**
   * Router transition to host level alerts page
   * @param event
   */
  goToHostDetails: function (event) {
    // todo: provide transition to host level alert details
  }
});
