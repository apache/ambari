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

App.MainAdminSecurityAddStep4Controller = App.MainAdminSecurityProgressController.extend(App.AddSecurityConfigs, {

  name: 'mainAdminSecurityAddStep4Controller',

  isBackBtnDisabled: function () {
    return !this.get('commands').someProperty('isError');
  }.property('commands.@each.isCompleted'),

  isSecurityApplied: function () {
    return this.get('commands').someProperty('name', 'START_SERVICES') && this.get('commands').findProperty('name', 'START_SERVICES').get('isSuccess');
  }.property('commands.@each.isCompleted'),

  /**
   * control disabled property of completion button
   */
  enableSubmit: function () {
    var addSecurityController = App.router.get('addSecurityController');
    if (this.get('commands').someProperty('isError') || this.get('commands').everyProperty('isSuccess')) {
      this.set('isSubmitDisabled', false);
      if (this.get('commands').someProperty('isError')) {
        addSecurityController.setStepsEnable();
      }
    } else {
      this.set('isSubmitDisabled', true);
      addSecurityController.setLowerStepsDisable(4);
    }
  }.observes('commands.@each.isCompleted'),

  /**
   * clear step info
   */
  clearStep: function () {
    this.set('commands', []);
    this.set('isSubmitDisabled', true);
    this.get('serviceConfigTags').clear();
  },

  loadCommands: function () {
    this._super();

    // Determine if ATS Component needs to be removed
    var isATSInstalled = this.get('content.isATSInstalled');
    var doesATSSupportKerberos = App.get("doesATSSupportKerberos");
    if (isATSInstalled && !doesATSSupportKerberos) {
      this.get('commands').splice(2, 0, App.Poll.create({name: 'DELETE_ATS', label: Em.I18n.translations['admin.addSecurity.apply.delete.ats'], isPolling: false}));
    }
  },

  /**
   * load step info
   */
  loadStep: function () {
    this.clearStep();
    this.prepareSecureConfigs();

    if (!this.resumeSavedCommands()) {
      this.loadCommands();
      this.addInfoToCommands();
      this.syncStopServicesOperation();
      this.addObserverToCommands();
      this.moveToNextCommand();
    }
  },

  /**
   * synchronize "STOP_SERVICES" operation from BO with command of step
   * @return {Boolean}
   */
  syncStopServicesOperation: function () {
    var runningOperations = App.router.get('backgroundOperationsController.services').filterProperty('isRunning');
    var stopAllOperation = runningOperations.findProperty('name', 'Stop All Services');
    var stopCommand = this.get('commands').findProperty('name', 'STOP_SERVICES');
    if (stopCommand && stopAllOperation) {
      stopCommand.set('requestId', stopAllOperation.get('id'));
      return true;
    }
    return false;
  },

  /**
   * resume previously saved commands
   * @return {Boolean}
   */
  resumeSavedCommands: function () {
    var commands = App.db.getSecurityDeployCommands();
    if (Em.isNone(commands) || commands.length === 0) return false;

    commands.forEach(function (_command, index) {
      commands[index] = App.Poll.create(_command);
    }, this);
    if (commands.someProperty('isError')) {
      this.get('commands').pushObjects(commands);
      this.addObserverToCommands();
      return true;
    } else if (commands.filterProperty('isStarted').someProperty('isCompleted', false)) {
      var runningCommand = commands.filterProperty('isStarted').findProperty('isCompleted', false);
      runningCommand.set('isStarted', false);
      this.get('commands').pushObjects(commands);
    } else {
      this.get('commands').pushObjects(commands);
    }
    this.addObserverToCommands();
    this.moveToNextCommand();
    return true;
  },

  manageSecureConfigs: function () {
    var serviceConfigTags = this.get('serviceConfigTags');
    var secureConfigs = this.get('secureConfigs');
    var siteProperties = this.get('configs');
    if (serviceConfigTags) {
      secureConfigs.forEach(function (config) {
        this.setPrincipalValue(config.serviceName, config.name);
      }, this);
      serviceConfigTags.forEach(function (_serviceConfigTags) {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
        siteProperties.filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
          if (!/_hosts?$/.test(_config.name)) {
            _serviceConfigTags.configs[_config.name] = _config.value;
          }
        }, this);
      }, this);
      return true;
    } else {
      var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
      command.set('isSuccess', false);
      command.set('isError', true);
      this.onJsError();
      return false;
    }
  },

  /**
   * send DELETE command to server to delete component
   * @param componentName
   * @param hostName
   */
  deleteComponents: function(componentName, hostName) {
    App.ajax.send({
      name: 'common.delete.host_component',
      sender: this,
      data: {
        componentName: componentName,
        hostName: hostName
      },
      success: 'onDeleteComplete',
      error: 'onDeleteError'
    });
  },

  /**
   * callback on successful deletion of component
   */
  onDeleteComplete: function () {
    var deleteAtsCommand = this.get('commands').findProperty('name', 'DELETE_ATS');
    console.warn('APP_TIMELINE_SERVER doesn\'t support security mode in this HDP stack. It has been removed from YARN service ');
    deleteAtsCommand.set('isError', false);
    deleteAtsCommand.set('isSuccess', true);
  },

  /**
   * callback on failed deletion of component
   */
  onDeleteError: function () {
    var deleteAtsCommand = this.get('commands').findProperty('name', 'DELETE_ATS');
    console.warn('Error: Can\'t delete APP_TIMELINE_SERVER');
    deleteAtsCommand.set('isError', true);
    deleteAtsCommand.set('isSuccess', false);
  },

  /**
   * show popup when js error occurred
   */
  onJsError: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{t admin.security.apply.configuration.error}}</p>')
      })
    });
  }
});
