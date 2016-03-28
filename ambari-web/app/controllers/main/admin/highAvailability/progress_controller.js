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

App.HighAvailabilityProgressPageController = App.HighAvailabilityWizardController.extend(App.wizardProgressPageControllerMixin, {

  name: 'highAvailabilityProgressPageController',
  clusterDeployState: 'HIGH_AVAILABILITY_DEPLOY',
  tasksMessagesPrefix: 'admin.highAvailability.wizard.step',
  isRollback: false,

  manualRollback: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('admin.highAvailability.confirmRollbackHeader'),
      primary: Em.I18n.t('yes'),
      showCloseButton: false,
      onPrimary: function () {
        var self = this;
        var controller = App.router.get('highAvailabilityWizardController');
        controller.clearTasksData();
        controller.clearStorageData();
        controller.finish();
        App.router.get('updateController').set('isWorking', true);
        App.clusterStatus.setClusterStatus({
          clusterName: App.router.get('content.cluster.name'),
          clusterState: 'DEFAULT',
          localdb: App.db.data
        }, {
          alwaysCallback: function () {
            self.hide();
            App.router.transitionTo('main.index');
            Em.run.next(function () {
              location.reload();
            });
          }
        });
      },
      secondary: Em.I18n.t('no'),
      onSecondary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(Em.I18n.t('admin.highAvailability.confirmManualRollbackBody'))
      })
    });
  },

  rollback: function () {
    var task = this.get('tasks').findProperty('status', 'FAILED');
    App.router.get(this.get('content.controllerName')).saveFailedTask(task);
    App.ModalPopup.show({
      header: Em.I18n.t('admin.highAvailability.confirmRollbackHeader'),
      primary: Em.I18n.t('common.confirm'),
      showCloseButton: false,
      onPrimary: function () {
        App.router.get('highAvailabilityWizardController').clearTasksData();
        App.router.transitionTo('main.admin.highAvailabilityRollback');
        this.hide();
      },
      secondary: Em.I18n.t('common.cancel'),
      body: Em.I18n.t('admin.highAvailability.confirmRollbackBody')
    });
  },

  /**
   * Prepare object to send to the server to save configs
   * Split all configs by site names and tag and note
   * @param siteNames Array
   * @param data Object
   * @param note String
   */
  reconfigureSites: function(siteNames, data, note) {
    var tagName = App.get('testMode') ? 'version1' : 'version' + (new Date).getTime();
    return siteNames.map(function(_siteName) {
      var config = data.items.findProperty('type', _siteName);
      var configToSave = {
        type: _siteName,
        tag: tagName,
        properties: config && config.properties,
        service_config_version_note: note || ''
      };
      if (config && config.properties_attributes) {
        configToSave.properties_attributes = config.properties_attributes;
      }
      return configToSave;
    });
  }
});
