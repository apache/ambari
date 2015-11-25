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
require('controllers/wizard/step7_controller');

App.KerberosWizardStep2Controller = App.WizardStep7Controller.extend(App.KDCCredentialsControllerMixin, {
  name: "kerberosWizardStep2Controller",

  isKerberosWizard: true,

  selectedServiceNames: ['KERBEROS'],

  allSelectedServiceNames: ['KERBEROS'],

  componentName: 'KERBEROS_CLIENT',

  installedServiceNames: [],

  servicesInstalled: false,

  addMiscTabToPage: false,

  /**
   * @type {boolean} true if test connection to hosts is in progress
   */
  testConnectionInProgress: false,

  /**
   * Should Back-button be disabled
   * @type {boolean}
   */
  isBackBtnDisabled: function() {
    return this.get('testConnectionInProgress');
  }.property('testConnectionInProgress'),

  /**
   * Should Next-button be disabled
   * @type {boolean}
   */
  isSubmitDisabled: function () {
    if (!this.get('stepConfigs.length') || this.get('testConnectionInProgress') || this.get('submitButtonClicked')) return true;
    return (!this.get('stepConfigs').filterProperty('showConfig', true).everyProperty('errorCount', 0) || this.get("miscModalVisible"));
  }.property('stepConfigs.@each.errorCount', 'miscModalVisible', 'submitButtonClicked', 'testConnectionInProgress'),

  hostNames: function () {
    return App.get('allHostNames');
  }.property('App.allHostNames'),

  serviceConfigTags: [],

  clearStep: function () {
    this._super();
    this.set('configs', []);
    this.get('serviceConfigTags').clear();
    this.set('servicesInstalled', false);
  },


  isConfigsLoaded: function () {
    return this.get('wizardController.stackConfigsLoaded');
  }.property('wizardController.stackConfigsLoaded'),

  /**
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    console.log("TRACE: Loading step2: Configure Kerberos");
    if (!App.StackService.find().someProperty('serviceName', 'KERBEROS') || !this.get('isConfigsLoaded')) {
      return;
    }
    this.clearStep();
    //STEP 2: Load on-site configs by service from local DB
    var storedConfigs = this.get('content.serviceConfigProperties');
    //STEP 3: Merge pre-defined configs with loaded on-site configs
    this.set('configs', App.config.mergePreDefinedWithStack(this.get('selectedServiceNames')));
    App.config.setPreDefinedServiceConfigs(this.get('addMiscTabToPage'));

    this.filterConfigs(this.get('configs'));
    if (App.get('supports.storeKDCCredentials') && !this.get('wizardController.skipClientInstall')) {
      this.initilizeKDCStoreProperties(this.get('configs'));
    }
    this.applyServicesConfigs(this.get('configs'), storedConfigs);
  },

  /**
   * Make Active Directory specific configs visible if user has selected AD option
   * @param configs
   */
  filterConfigs: function (configs) {
    var kdcType = this.get('content.kerberosOption');
    var adConfigNames = ['ldap_url', 'container_dn', 'ad_create_attributes_template'];
    var mitConfigNames = ['kdc_create_attributes'];
    var kerberosWizardController = this.controllers.get('kerberosWizardController');
    var manageIdentitiesConfig = configs.findProperty('name', 'manage_identities');

    if (kdcType === Em.I18n.t('admin.kerberos.wizard.step1.option.manual')) {
      if (kerberosWizardController.get('skipClientInstall')) {
        kerberosWizardController.overrideVisibility(configs, false, kerberosWizardController.get('exceptionsOnSkipClient'));
      }
      return;
    } else if (manageIdentitiesConfig) {
      manageIdentitiesConfig.isVisible = false;
      manageIdentitiesConfig.value = 'true';
    }

    adConfigNames.forEach(function (_configName) {
      var config = configs.findProperty('name', _configName);
      if (config) {
        config.isVisible = kdcType === Em.I18n.t('admin.kerberos.wizard.step1.option.ad');
      }
    }, this);

    mitConfigNames.forEach(function (_configName) {
      var config = configs.findProperty('name', _configName);
      if (config) {
        config.isVisible = kdcType === Em.I18n.t('admin.kerberos.wizard.step1.option.kdc');
      }
    }, this);
  },

  submit: function () {
    if (this.get('isSubmitDisabled')) return false;
    this.set('isSubmitDisabled', true);
    var self = this;
    this.deleteKerberosService().always(function (data) {
      self.configureKerberos();
    });
  },

  configureKerberos: function () {
    var self = this;
    var wizardController = App.router.get(this.get('content.controllerName'));
    var callback = function () {
      self.createConfigurations().done(function () {
        self.createKerberosAdminSession().done(function () {
          App.router.send('next');
        });
      });
    };
    if (wizardController.get('skipClientInstall')) {
      callback();
    } else {
      wizardController.createKerberosResources(callback);
    }
  },

  /**
   * Delete Kerberos service if it exists
   */
  deleteKerberosService: function () {
    var serviceName = this.selectedServiceNames[0];
    if (App.cache.services.someProperty('ServiceInfo.service_name', 'KERBEROS')) {
      App.cache.services.removeAt(App.cache.services.indexOf(App.cache.services.findProperty('ServiceInfo.service_name', 'KERBEROS')));
    }
    if (App.Service.find().someProperty('serviceName', 'KERBEROS')) {
      App.serviceMapper.deleteRecord(App.Service.find('KERBEROS'));
    }
    return App.ajax.send({
      name: 'common.delete.service',
      sender: this,
      data: {
        serviceName: serviceName
      }
    });
  },

  createConfigurations: function () {
    var service = App.StackService.find().findProperty('serviceName', 'KERBEROS');
    var serviceConfigTags = [];
    var tag = 'version' + (new Date).getTime();
    Object.keys(service.get('configTypes')).forEach(function (type) {
      if (!serviceConfigTags.someProperty('type', type)) {
        var obj = this.createKerberosSiteObj(type, tag);
        obj.service_config_version_note = Em.I18n.t('admin.kerberos.wizard.configuration.note');
        serviceConfigTags.pushObject(obj);
      }
    }, this);
    var allConfigData = [];
    var serviceConfigData = [];
    Object.keys(service.get('configTypesRendered')).forEach(function (type) {
      var serviceConfigTag = serviceConfigTags.findProperty('type', type);
      if (serviceConfigTag) {
        serviceConfigData.pushObject(serviceConfigTag);
      }
    }, this);
    if (serviceConfigData.length) {
      allConfigData.pushObject(JSON.stringify({
        Clusters: {
          desired_config: serviceConfigData
        }
      }));
    }
    return App.ajax.send({
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + allConfigData.toString() + ']'
      }
    });
  },

  createKerberosSiteObj: function (site, tag) {
    var properties = {};
    var content = this.get('stepConfigs')[0].get('configs');
    var configs = content.filterProperty('filename', site + '.xml');
    // properties that should be formated as hosts
    var hostProperties = ['kdc_host', 'realm'];
    configs.forEach(function (_configProperty) {
      // do not pass any globals whose name ends with _host or _hosts
      if (_configProperty.isRequiredByAgent !== false) {
        if (hostProperties.contains(_configProperty.name)) {
          properties[_configProperty.name] = App.config.trimProperty({displayType: 'host', value: _configProperty.value});
        } else {
          properties[_configProperty.name] = App.config.trimProperty(_configProperty);
        }
      }
    }, this);
    this.tweakKdcTypeValue(properties);
    this.tweakManualKdcProperties(properties);
    return {"type": site, "tag": tag, "properties": properties};
  },

  tweakKdcTypeValue: function (properties) {
    for (var prop in App.router.get('mainAdminKerberosController.kdcTypesValues')) {
      if (App.router.get('mainAdminKerberosController.kdcTypesValues').hasOwnProperty(prop)) {
        if (App.router.get('mainAdminKerberosController.kdcTypesValues')[prop] === properties['kdc_type']) {
          properties['kdc_type'] = prop;
        }
      }
    }
  },

  tweakManualKdcProperties: function (properties) {
    var kerberosWizardController = this.controllers.get('kerberosWizardController');
    if (properties['kdc_type'] === 'none' || kerberosWizardController.get('skipClientInstall')) {
      if (properties.hasOwnProperty('manage_identities')) {
        properties['manage_identities'] = 'false';
      }
      if (properties.hasOwnProperty('install_packages')) {
        properties['install_packages'] = 'false';
      }
      if (properties.hasOwnProperty('manage_krb5_conf')) {
        properties['manage_krb5_conf'] = 'false';
      }
    }
  },

  /**
   * puts kerberos admin credentials in the live cluster session
   * @returns {*} jqXHr
   */
  createKerberosAdminSession: function (configs) {
    configs = configs || this.get('stepConfigs')[0].get('configs');
    if (App.get('supports.storeKDCCredentials') && !this.get('wizardController.skipClientInstall')) {
      return this.createKDCCredentials(configs);
    }

    var adminPrincipalValue = configs.findProperty('name', 'admin_principal').value;
    var adminPasswordValue = configs.findProperty('name', 'admin_password').value;
    return App.ajax.send({
      name: 'common.cluster.update',
      sender: this,
      data: {
        clusterName: App.get('clusterName') || App.clusterStatus.get('clusterName'),
        data: [{
          session_attributes: {
            kerberos_admin: {principal: adminPrincipalValue, password: adminPasswordValue}
          }
        }]
      }
    });
  },

  /**
   * shows popup with to warn user
   * @param primary
   */
  showConnectionInProgressPopup: function(primary) {
    var primaryText = Em.I18n.t('common.exitAnyway');
    var msg = Em.I18n.t('services.service.config.connection.exitPopup.msg');
    App.showConfirmationPopup(primary, msg, null, null, primaryText)
  }
});
