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
require('controllers/main/admin/kerberos/step4_controller');

App.MainAdminKerberosController = App.KerberosWizardStep4Controller.extend({
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
        templateName: require('templates/main/admin/security/notify_security_off_popup')
      })
    });
  },

  getUpdatedSecurityStatus: function () {
    this.setSecurityStatus();
    return this.get('securityEnabled');
  },

  /**
   * performes clustere check before kerbefos security
   * wizard starts if <code>preKerberizeCheck<code> supports is true
   * otherwise runs <code>startKerberosWizard<code>
   * @method checkAndStartKerberosWizard
   */
  checkAndStartKerberosWizard: function() {
    if (App.get('supports.preKerberizeCheck')) {
      App.ajax.send({
        name: "admin.kerberos_security.checks",
        sender: this,
        success: "runSecurityCheckSuccess"
      });
    } else {
      this.startKerberosWizard();
    }
  },

  /**
   * success callback of <code>checkAndStartKerberosWizard()</code>
   * if there are some fails - it shows popup else open security wizard
   * @param data {object}
   * @param opt {object}
   * @param params {object}
   * @returns {App.ModalPopup|undefined}
   */
  runSecurityCheckSuccess: function (data, opt, params) {
    //TODO correct check
    if (data.items.someProperty('UpgradeChecks.status', "FAIL")) {
      var header = Em.I18n.t('popup.clusterCheck.Security.header').format(params.label);
      var title = Em.I18n.t('popup.clusterCheck.Security.title');
      var alert = Em.I18n.t('popup.clusterCheck.Security.alert');
      App.showClusterCheckPopup(data, header, title, alert);
    } else {
      this.startKerberosWizard();
    }
  },

  startKerberosWizard: function () {
    this.setAddSecurityWizardStatus('RUNNING');
    App.router.transitionTo('adminKerberos.adminAddKerberos');
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
  },

  /**
   * Override <code>App.KerberosWizardStep4Controller</code>
   *
   * @returns {$.ajax}
   */
  loadStackDescriptorConfigs: function () {
    return App.ajax.send({
      sender: this,
      name: 'get.cluster.artifact',
      data: {
        artifactName: 'kerberos_descriptor',
        stackVersionNumber: App.get('currentStackVersionNumber')
      }
    });
  },

  
  /**
   * Override <code>App.KerberosWizardStep4Controller</code>
   * 
   * @param {App.ServiceConfigProperty[]} configs
   * @returns {App.ServiceConfigProperty[]} 
   */
  prepareConfigProperties: function(configs) {
    var configProperties = configs.slice(0);
    var siteProperties = App.config.get('preDefinedSiteProperties');
    var installedServiceNames = ['Cluster'].concat(App.Service.find().mapProperty('serviceName'));
    configProperties = configProperties.filter(function(item) {
      return installedServiceNames.contains(item.get('serviceName'));
    });
    configProperties.setEach('isSecureConfig', false);
    configProperties.forEach(function(property, item, allConfigs) {
      if (property.get('observesValueFrom')) {
        var observedValue = allConfigs.findProperty('name', property.get('observesValueFrom')).get('value');
        property.set('value', observedValue);
        property.set('defaultValue', observedValue);
      }
      if (property.get('serviceName') == 'Cluster') {
        property.set('category', 'Global');
      } else {
        property.set('category', property.get('serviceName'));
      }
      // All user identity should be grouped under "Ambari Principals" category
      if (property.get('identityType') == 'user') property.set('category', 'Ambari Principals');
      var siteProperty = siteProperties.findProperty('name', property.get('name'));
      if (siteProperty) {
        if (siteProperty.category === property.get('category')) {
          property.set('displayName',siteProperty.displayName);
          if (siteProperty.index) {
            property.set('index', siteProperty.index);
          }
        }
      }
    });
    configProperties.setEach('isEditable', false);
    return configProperties;
  }
  
});
