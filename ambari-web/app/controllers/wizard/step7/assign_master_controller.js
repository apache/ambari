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

App.AssignMasterOnStep7Controller = Em.Controller.extend(App.BlueprintMixin, App.AssignMasterComponents, {

  name:"assignMasterOnStep7Controller",

  useServerValidation: false,

  showInstalledMastersFirst: false,

  deferred: null,

  popup: null,

  mastersToCreate: [],

  markSavedComponentsAsInstalled: true,

  execute: function(context) {
    var dfd = $.Deferred();
    this.set('content', context.get('content'));
    var allConfigs = context.get('stepConfigs').mapProperty('configs').filter(function(item) {
      return item.length;
    }).reduce(function(p, c) {
      if (p) {
        return p.concat(c);
      }
    });
    var storedConfigs = this.get('content.serviceConfigProperties');
    var configThemeActions =  App.configTheme.getConfigThemeActions(allConfigs, storedConfigs || []);

    if (configThemeActions['add'].length) {
      this.showAssignComponentPopup(dfd, configThemeActions['add']);
    } else {
      if (configThemeActions['delete'].length) {
        this.removeMasterComponent(configThemeActions['delete']);
      }
      dfd.resolve();
    }
    return dfd.promise();
  },

  showAssignComponentPopup: function(dfd, componentsToAdd) {
    var self = this;
    this.set('deferred', dfd);
    this.set('mastersToCreate', componentsToAdd);

    var popup = App.ModalPopup.show({
      classNames: ['full-width-modal', 'add-service-wizard-modal'],
      header: Em.I18n.t('admin.highAvailability.wizard.step2.header'),
      bodyClass: App.AssignMasterOnStep7View.extend({
        controller: self
      }),
      primary: Em.I18n.t('form.cancel'),
      showFooter: false,
      secondary: null,
      onClose: function () {
        this.hide();
      },
      didInsertElement: function () {
        this._super();
        this.fitHeight();
      }
    });
    this.set('popup', popup);
  },

  removeMasterComponent: function(componentsToDelete) {
    var parentController = App.router.get(this.get('content.controllerName'));
    var masterComponentHosts =   this.get('content.masterComponentHosts');
    componentsToDelete.forEach(function(_componentName) {
      masterComponentHosts = masterComponentHosts.rejectProperty('component',_componentName);
    }, this);
    this.get('content').set('masterComponentHosts', masterComponentHosts);
    parentController.setDBProperty('masterComponentHosts',  masterComponentHosts);
  },

  /**
   * Submit button click handler
   * @method submit
   */
  submit: function () {
    if (this.get('deferred')) {
      this.get('popup').hide();
      var controller = App.router.get(this.get('content.controllerName'));
      controller.saveMasterComponentHosts(this);
      this.get('deferred').resolve();
      this.set('deferred', null);
    }
  }
});