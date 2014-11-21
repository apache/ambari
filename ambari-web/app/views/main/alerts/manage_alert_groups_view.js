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

App.MainAlertsManageAlertGroupView = Em.View.extend({

  templateName: require('templates/main/alerts/manage_alert_groups_popup'),

  selectedAlertGroup: null,

  isRemoveButtonDisabled: true,

  isRenameButtonDisabled: true,

  isDuplicateButtonDisabled: true,

  buttonObserver: function () {
    var selectedAlertGroup = this.get('controller.selectedAlertGroup');
    if(selectedAlertGroup && selectedAlertGroup.default){
      this.set('isRemoveButtonDisabled', true);
      this.set('isRenameButtonDisabled', true);
      this.set('isDuplicateButtonDisabled', false);
    }else{
      this.set('isRemoveButtonDisabled', false);
      this.set('isRenameButtonDisabled', false);
      this.set('isDuplicateButtonDisabled', false);
    }
  }.observes('controller.selectedAlertGroup'),

  onGroupSelect: function () {
    var selectedAlertGroup = this.get('selectedAlertGroup');
    // to unable user select more than one alert group at a time
    if (selectedAlertGroup && selectedAlertGroup.length) {
      this.set('controller.selectedAlertGroup', selectedAlertGroup[selectedAlertGroup.length - 1]);
    }
    if (selectedAlertGroup && selectedAlertGroup.length > 1) {
      this.set('selectedConfigGroup', selectedAlertGroup[selectedAlertGroup.length - 1]);
    }
    this.set('controller.selectedDefinitions', []);
  }.observes('selectedAlertGroup'),

  onLoad: function () {
    if (this.get('controller.isLoaded')) {
      this.set('selectedAlertGroup', this.get('controller.alertGroups')[0]);
    }
  }.observes('controller.isLoaded', 'controller.alertGroups'),

  willInsertElement: function() {
    this.get('controller').loadAlertGroups();
    this.get('controller').loadAlertDefinitions();
    this.get('controller').loadAlertNotifications();
  },

  didInsertElement: function () {
    this.onLoad();
    App.tooltip($("[rel='button-info']"));
    App.tooltip($("[rel='button-info-dropdown']"), {placement: 'left'});
  },

  addButtonTooltip: function () {
    return  Em.I18n.t('alerts.actions.manage_alert_groups_popup.addButton');
  }.property(),
  removeButtonTooltip: function () {
    return  Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeButton');
  }.property(),
  renameButtonTooltip: function () {
    return  Em.I18n.t('alerts.actions.manage_alert_groups_popup.renameButton');
  }.property(),
  duplicateButtonTooltip: function () {
    return  Em.I18n.t('alerts.actions.manage_alert_groups_popup.duplicateButton');
  }.property(),
  addDefinitionTooltip: function () {
    if (this.get('controller.selectedAlertGroup.default')) {
      return Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinitionToDefault');
    } else if (this.get('controller.selectedAlertGroup.isAddDefinitionsDisabled')) {
      return Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinitionDisabled');
    } else {
      return  Em.I18n.t('alerts.actions.manage_alert_groups_popup.addDefinition');
    }
  }.property('controller.selectedConfigGroup.isDefault', 'controller.selectedConfigGroup.isAddHostsDisabled'),
  removeDefinitionTooltip: function () {
    return  Em.I18n.t('alerts.actions.manage_alert_groups_popup.removeDefinition');
  }.property(),

  errorMessage: function () {
    return  this.get('controller.errorMessage');
  }.property('controller.errorMessage')

});
