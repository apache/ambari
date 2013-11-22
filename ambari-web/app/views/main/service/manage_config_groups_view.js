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

App.MainServiceManageConfigGroupView = Em.View.extend({

  templateName: require('templates/main/service/manage_configuration_groups_popup'),

  selectedConfigGroup: null,

  isRemoveButtonDisabled: true,

  isRenameButtonDisabled: true,

  //Disable actions remove and rename for Default config group
  buttonObserver: function () {
    var selectedConfigGroup = this.get('controller.selectedConfigGroup');
    if(selectedConfigGroup.isDefault){
      this.set('isRemoveButtonDisabled', true);
      this.set('isRenameButtonDisabled', true);
    }else{
      this.set('isRemoveButtonDisabled', false);
      this.set('isRenameButtonDisabled', false);
    }
  }.observes('controller.selectedConfigGroup', 'controller.isHostsModified'),

  onGroupSelect: function () {
    var selectedConfigGroup = this.get('selectedConfigGroup');
    // to unable user select more than one config group at a time
    if (selectedConfigGroup.length) {
      this.set('controller.selectedConfigGroup', selectedConfigGroup[selectedConfigGroup.length - 1]);
    }
    if (selectedConfigGroup.length > 1) {
      this.set('selectedConfigGroup', selectedConfigGroup[selectedConfigGroup.length - 1]);
    }
    this.set('controller.selectedHosts', []);
  }.observes('selectedConfigGroup'),

  onLoad: function () {
    if (this.get('controller.isLoaded')) {
      this.set('selectedConfigGroup', this.get('controller.configGroups')[0])
    }
  }.observes('controller.isLoaded', 'controller.configGroups'),

  didInsertElement: function () {
    this.get('controller').loadConfigGroups(this.get('serviceName'));
    $('.properties-link').tooltip();
    $("[rel='button-info']").tooltip();
  },

  isDeleteHostsDisabled: function () {
    var selectedConfigGroup = this.get('controller.selectedConfigGroup');
    if(selectedConfigGroup){
      if(selectedConfigGroup.isDefault || this.get('controller.selectedHosts').length === 0){
        return true;
      }else{
        return false;
      }
    }
    return true;
  }.property('controller.selectedConfigGroup', 'controller.selectedConfigGroup.hosts.length', 'controller.selectedHosts.length'),

  addButtonTooltip: function () {
    return  Em.I18n.t('services.service.config_groups_popup.addButton');
  }.property(),
  removeButtonTooltip: function () {
    return  Em.I18n.t('services.service.config_groups_popup.removeButton');
  }.property(),
  renameButtonTooltip: function () {
    return  Em.I18n.t('services.service.config_groups_popup.renameButton');
  }.property(),
  duplicateButtonTooltip: function () {
    return  Em.I18n.t('services.service.config_groups_popup.duplicateButton');
  }.property(),
  addHostTooltip: function () {
    return  Em.I18n.t('services.service.config_groups_popup.addHost');
  }.property(),
  removeHostTooltip: function () {
    return  Em.I18n.t('services.service.config_groups_popup.removeHost');
  }.property()

});
