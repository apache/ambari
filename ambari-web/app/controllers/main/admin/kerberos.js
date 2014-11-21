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
App.MainAdminKerberosController = Em.Controller.extend({
  name: 'mainAdminKerberosController',
  securityEnabled: false,
  dataIsLoaded: false,
  isRecommendedLoaded: true,
  getAddSecurityWizardStatus: function () {
    return App.db.getSecurityWizardStatus();
  },
  setAddSecurityWizardStatus: function (status) {
    App.db.setSecurityWizardStatus(status);
  },

  setDisableSecurityStatus: function (status) {
    App.db.setDisableSecurityStatus(status);
  },
  getDisableSecurityStatus: function (status) {
    return App.db.getDisableSecurityStatus();
  },

  notifySecurityOff: false,
  notifySecurityAdd: false,

  notifySecurityOffPopup: function () {
    var self = this;
    App.ModalPopup.show({
      header: Em.I18n.t('popup.confirmation.commonHeader'),
      primary: Em.I18n.t('ok'),
      onPrimary: function () {
        App.db.setSecurityDeployCommands(undefined);
        self.setDisableSecurityStatus("RUNNING");
        App.router.transitionTo('disableSecurity');
        this.hide();
      },
      bodyClass: Ember.View.extend({
        isMapReduceInstalled: App.Service.find().mapProperty('serviceName').contains('MAPREDUCE'),
        templateName: require('templates/main/admin/security/notify_security_off_popup')
      })
    })
  },

  getUpdatedSecurityStatus: function () {
    this.setSecurityStatus();
    return this.get('securityEnabled');
  },

  startKerberosWizard: function () {
    this.setAddSecurityWizardStatus('RUNNING');
    App.router.transitionTo('adminAddKerberos');
  },

  /**
   * Loads the security status from server (security_enabled property in cluster-env configuration)
   */
  loadSecurityStatusFromServer: function () {
    if (App.get('testMode')) {
      this.set('securityEnabled', !App.get('testEnableSecurity'));
      this.set('dataIsLoaded', true);
    } else {
      //get Security Status From Server
      var self = this;
      var tags = [{siteName: 'cluster-env'}];
      App.router.get('configurationController').getConfigsByTags(tags).done(function (data) {
        var configs = data[0].properties;
        if (configs) {
          self.set('securityEnabled', configs['security_enabled'] === 'true');
        }
        self.set('dataIsLoaded', true);
      });
    }
  }
});




