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
  isBackBtnDisabled: Em.computed.alias('testConnectionInProgress'),

  /**
   * Should Next-button be disabled
   * @type {boolean}
   */
  isSubmitDisabled: function () {
    if (!this.get('stepConfigs.length') || this.get('testConnectionInProgress') || this.get('submitButtonClicked')) return true;
    return (!this.get('stepConfigs').filterProperty('showConfig', true).everyProperty('errorCount', 0) || this.get("miscModalVisible"));
  }.property('stepConfigs.@each.errorCount', 'miscModalVisible', 'submitButtonClicked', 'testConnectionInProgress'),

  hostNames: Em.computed.alias('App.allHostNames'),

  serviceConfigTags: [],

  clearStep: function () {
    this._super();
    this.set('configs', []);
    this.get('serviceConfigTags').clear();
    this.set('servicesInstalled', false);
  },


  isConfigsLoaded: Em.computed.alias('wizardController.stackConfigsLoaded'),

  /**
   * On load function
   * @method loadStep
   */

  loadStep: function () {
    if (!App.StackService.find().someProperty('serviceName', 'KERBEROS') || !this.get('isConfigsLoaded')) {
      return;
    }
    this.clearStep();
    App.config.setPreDefinedServiceConfigs(this.get('addMiscTabToPage'));
    var stored = this.get('content.serviceConfigProperties');
    var kerberosConfigTypes = Em.keys(App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'KERBEROS').get('configTypes'));

    this.set('configs', stored || App.configsCollection.getAll().filter(function(configProperty) {
      var fileName = Em.getWithDefault(configProperty, 'fileName', false);
      var isService = ['KERBEROS'].contains(Em.get(configProperty, 'serviceName'));
      var isFileName = fileName && kerberosConfigTypes.contains(App.config.getConfigTagFromFileName(fileName));
      return isService || isFileName;
    }));

    this.filterConfigs(this.get('configs'));
    if (!this.get('wizardController.skipClientInstall')) {
      this.initilizeKDCStoreProperties(this.get('configs'));
    }
    this.applyServicesConfigs(this.get('configs'));
  },

  /**
   * Make Active Directory or IPA specific configs visible if user has selected AD or IPA option
   * @param configs
   */
  filterConfigs: function (configs) {
    var kdcType = this.get('content.kerberosOption');
    var adConfigNames = ['ldap_url', 'container_dn', 'ad_create_attributes_template'];
    var mitConfigNames = ['kdc_create_attributes'];
    var ipaConfigNames = ['group', 'set_password_expiry', 'password_chat_timeout'];

    var kerberosWizardController = this.controllers.get('kerberosWizardController');
    var manageIdentitiesConfig = configs.findProperty('name', 'manage_identities');

    configs.filterProperty('serviceName', 'KERBEROS').setEach('isVisible', true);
    this.setKDCTypeProperty(configs);
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

    ipaConfigNames.forEach(function (_configName) {
      var config = configs.findProperty('name', _configName);
      if (config) {
        config.isVisible = kdcType === Em.I18n.t('admin.kerberos.wizard.step1.option.ipa');
      }
    }, this);
  },

  submit: function () {
    if (this.get('isSubmitDisabled')) return false;
    this.set('isSubmitDisabled', true);
    var self = this;
    this.get('wizardController').deleteKerberosService().always(function () {
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
    this.tweakIpaKdcProperties(properties);
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

  tweakIpaKdcProperties: function (properties) {
    if (typeof properties['kdc_type'] === 'undefined') {
      return;
    }
    if (this.get('content.kerberosOption') === App.router.get('mainAdminKerberosController.kdcTypesValues')['ipa']) {
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
    if (!this.get('wizardController.skipClientInstall')) {
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
  },

  setKDCTypeProperty: function(configs) {
    var selectedOption = this.get('content.kerberosOption');
    var kdcTypeProperty = configs.filterProperty('filename', 'kerberos-env.xml').findProperty('name', 'kdc_type');
    var kdcValuesMap = App.router.get('mainAdminKerberosController.kdcTypesValues');
    var kdcTypeValue = Em.keys(kdcValuesMap).filter(function(typeAlias) {
      return Em.get(kdcValuesMap, typeAlias) === selectedOption;
    })[0];
    if (kdcTypeProperty) {
      Em.set(kdcTypeProperty, 'value', kdcValuesMap[kdcTypeValue]);
    }
  }
});
