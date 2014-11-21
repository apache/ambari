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

App.KerberosWizardStep2Controller = App.WizardStep7Controller.extend({
  name: "kerberosWizardStep2Controller",
  isKerberosWizard: true,

  selectedServiceNames: ['KERBEROS'],

  allSelectedServiceNames: ['KERBEROS'],

  installedServiceNames: [],

  servicesInstalled: false,

  addMiscTabToPage: false,

  hostNames: function() {
    return this.get('content.hosts');
  }.property('content.hosts'),

  serviceConfigTags: [],


  clearStep: function () {
    this._super();
    this.get('serviceConfigTags').clear();
    this.set('servicesInstalled', false);
  },


  /**
   * On load function
   * @method loadStep
   */
  loadStep: function () {
    console.log("TRACE: Loading step7: Configure Services");
    this.clearStep();
    var kerberosStackService = App.StackService.find().findProperty('serviceName', 'KERBEROS');
    if (!kerberosStackService) return;
    //STEP 1: Load advanced configs
    var advancedConfigs = this.get('content.advancedServiceConfig');
    //STEP 2: Load on-site configs by service from local DB
    var storedConfigs = this.get('content.serviceConfigProperties');
    //STEP 3: Merge pre-defined configs with loaded on-site configs
    var configs = App.config.mergePreDefinedWithStored(
      storedConfigs,
      advancedConfigs,
      this.get('selectedServiceNames'));
    App.config.setPreDefinedServiceConfigs(this.get('addMiscTabToPage'));
    //STEP 4: Add advanced configs
    App.config.addAdvancedConfigs(configs, advancedConfigs);
    this.applyServicesConfigs(configs, storedConfigs);
  },

  submit: function () {
    this.set('isSubmitDisabled', true);
    var self = this;
    this.deleteKerberosService().always(function (data) {
      self.createKerberosResources();
    });
  },

  createKerberosResources: function () {
    var self = this;
    this.createKerberosService().done(function() {
      self.createConfigurations().done(function() {
        App.router.send('next');
      });
    });
  },

  /**
   * Delete Kerberos service if it exists
   */
  deleteKerberosService: function () {
    var serviceName = this.selectedServiceNames[0];
    return App.ajax.send({
      name: 'common.delete.service',
      sender: this,
      data: {
        serviceName: serviceName
      }
    });
  },

  createKerberosService: function () {
    return App.ajax.send({
      name: 'wizard.step8.create_selected_services',
      sender: this,
      data: {
        data: '{"ServiceInfo": { "service_name": "KERBEROS"}}',
        cluster: App.get('clusterName') || App.clusterStatus.get('clusterName')
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
    configs.forEach(function (_configProperty) {
      // do not pass any globals whose name ends with _host or _hosts
      if (_configProperty.isRequiredByAgent !== false) {
        properties[_configProperty.name] = _configProperty.value;
      }
    }, this);
    return {"type": site, "tag": tag, "properties": properties};
  }
});

