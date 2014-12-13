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

App.AlertDefinitionConfigsView = Em.View.extend({

  controllerBinding: 'App.router.mainAlertDefinitionConfigsController',

  templateName: require('templates/main/alerts/configs'),

  /**
   * Define whether configs are editable
   * is set in template
   * @type {Boolean}
   */
  canEdit: true,

  /**
   * List of classes applied to all inputs
   * @type {String}
   */
  basicClass: 'span9',

  init: function () {
    this.set('controller.canEdit', this.get('canEdit'));
    this.set('controller.isWizard', this.get('isWizard'));
    this.set('controller.alertDefinitionType', this.get('alertDefinitionType'));
    this.set('controller.content', this.get('content'));
    this.get('controller').renderConfigs();
    this._super();
  },

  errorMessage: Em.I18n.t('alerts.definition.details.configs.thresholdsErrorMsg')

});

App.AlertConfigTextFieldView = Em.View.extend({
  templateName: require('templates/main/alerts/configs/alert_config_text_field'),
  classNameBindings: ['property.classNames', 'parentView.basicClass']
});

App.AlertConfigTextAreaView = Em.TextArea.extend({
  valueBinding: 'property.value',
  disabledBinding: 'property.isDisabled',
  classNameBindings: ['property.classNames', 'parentView.basicClass']
});

App.AlertConfigSelectView = Em.Select.extend({
  attributeBindings: ['disabled'],
  selectionBinding: 'property.value',
  disabledBinding: 'property.isDisabled',
  contentBinding: 'property.options',
  classNameBindings: ['property.classNames', 'parentView.basicClass']
});

App.AlertConfigThresholdView = Em.View.extend({
  templateName: require('templates/main/alerts/configs/alert_config_threshold'),
  classNameBindings: ['property.classNames', 'parentView.basicClass']
});

App.AlertConfigRadioButtonView = Em.Checkbox.extend({
  attributeBindings: ['type', 'name', 'value', 'checked', 'disabled'],
  type: 'radio',
  nameBinding: 'property.group',
  checkedBinding: 'property.value',

  change: function () {
    this.set('property.value', true);
    this.get('parentView.controller.configs').filterProperty('group', this.get('name')).without(this.get('property')).setEach('value', false);
    this.get('parentView.controller').changeType(this.get('property.name'));
  },

  classNameBindings: ['property.classNames']
});
