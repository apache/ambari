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

App.InstallerController = App.WizardController.extend({

  name: 'installerController',

  totalSteps: 10,

  content: Em.Object.create({
    cluster: null,
    installOptions: null,
    hosts: null,
    services: null,
    slaveComponentHosts: null,
    masterComponentHosts: null,
    serviceConfigProperties: null,
    advancedServiceConfig: null,
    slaveGroupProperties: null,
    controllerName: 'installerController'
  }),

  getCluster: function(){
    return jQuery.extend({}, this.get('clusterStatusTemplate'));
  },

  getInstallOptions: function(){
    return jQuery.extend({}, this.get('installOptionsTemplate'));
  },

  getHosts: function(){
    return [];
  },

  /**
   * Remove host from model. Used at <code>Confirm hosts(step2)</code> step
   * @param hosts Array of hosts, which we want to delete
   */
  removeHosts: function (hosts) {
    //todo Replace this code with real logic
    App.db.removeHosts(hosts);
  },

  /**
   * Load confirmed hosts.
   * Will be used at <code>Assign Masters(step5)</code> step
   */
  loadConfirmedHosts: function () {
    this.set('content.hosts', App.db.getHosts() || []);
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var servicesInfo = App.db.getService();
    servicesInfo.forEach(function (item, index) {
      servicesInfo[index] = Em.Object.create(item);
      servicesInfo[index].isInstalled = false;
    });
    this.set('content.services', servicesInfo);
    console.log('installerController.loadServices: loaded data ', JSON.stringify(servicesInfo));
    console.log("The type odf serviceInfo: " + typeof servicesInfo);
    console.log('selected services ', servicesInfo.filterProperty('isSelected', true).mapProperty('serviceName'));
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var serviceNames = [];
    App.db.setService(stepController.get('content'));
    stepController.filterProperty('isSelected', true).forEach(function (item) {
      serviceNames.push(item.serviceName);
    });
    this.set('content.selectedServiceNames', serviceNames);
    App.db.setSelectedServiceNames(serviceNames);
    console.log('installerController.saveServices: saved data ', serviceNames);
  },

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   */
  saveMasterComponentHosts: function (stepController) {
    var obj = stepController.get('selectedServicesMasters');

    var masterComponentHosts = [];
    obj.forEach(function (_component) {
      masterComponentHosts.push({
        display_name: _component.get('display_name'),
        component: _component.get('component_name'),
        hostName: _component.get('selectedHost'),
        serviceId: _component.get('serviceId'),
        isInstalled: false
      });
    });

    console.log("installerController.saveMasterComponentHosts: saved hosts ", masterComponentHosts);
    App.db.setMasterComponentHosts(masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    var masterComponentHosts = App.db.getMasterComponentHosts() || [];
    this.set("content.masterComponentHosts", masterComponentHosts);
    console.log("InstallerController.loadMasterComponentHosts: loaded hosts ", masterComponentHosts);
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = App.db.getSlaveComponentHosts() || null;
    this.set("content.slaveComponentHosts", slaveComponentHosts);
    console.log("InstallerController.loadSlaveComponentHosts: loaded hosts ", slaveComponentHosts);
  },

  /**
   * Save config properties
   * @param stepController Step7WizardController
   */
  saveServiceConfigProperties: function (stepController) {
    var serviceConfigProperties = [];
    stepController.get('stepConfigs').forEach(function (_content) {
      _content.get('configs').forEach(function (_configProperties) {
        var displayType = _configProperties.get('displayType');
        if (displayType === 'directories' || displayType === 'directory') {
          var value = _configProperties.get('value').trim().split(/\s+/g).join(',');
          _configProperties.set('value', value);
        }
        var overrides = _configProperties.get('overrides');
        var overridesArray = [];
        if(overrides!=null){
          overrides.forEach(function(override){
            var overrideEntry = {
                value: override.get('value'),
                hosts: []
            };
            override.get('selectedHostOptions').forEach(function(host){
              overrideEntry.hosts.push(host);
            });
            overridesArray.push(overrideEntry);
          });
        }
        var configProperty = {
          id: _configProperties.get('id'),
          name: _configProperties.get('name'),
          value: _configProperties.get('value'),
          defaultValue: _configProperties.get('defaultValue'),
          service: _configProperties.get('serviceName'),
          domain:  _configProperties.get('domain'),
          filename: _configProperties.get('filename'),
          overrides: overridesArray
        };
        serviceConfigProperties.push(configProperty);
      }, this);

    }, this);

    App.db.setServiceConfigProperties(serviceConfigProperties);
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  },

  /**
   * Load serviceConfigProperties to model
   */
  loadServiceConfigProperties: function () {
    var serviceConfigProperties = App.db.getServiceConfigProperties();
    this.set('content.serviceConfigProperties', serviceConfigProperties);
    console.log("InstallerController.loadServiceConfigProperties: loaded config ", serviceConfigProperties);

    this.set('content.advancedServiceConfig', App.db.getAdvancedServiceConfig());
  },

  /**
   * Load information about hosts with clients components
   */
  loadClients: function () {
    var clients = App.db.getClientsForSelectedServices();
    this.set('content.clients', clients);
    console.log("InstallerController.loadClients: loaded list ", clients);
  },

  /**
   * Generate clients list for selected services and save it to model
   * @param stepController step4WizardController
   */
  saveClients: function (stepController) {
    var clients = [];
    var serviceComponents = require('data/service_components');

    stepController.get('content').filterProperty('isSelected', true).forEach(function (_service) {
      var client = serviceComponents.filterProperty('service_name', _service.serviceName).findProperty('isClient', true);
      if (client) {
        clients.pushObject({
          component_name: client.component_name,
          display_name: client.display_name,
          isInstalled: false
        });
      }
    }, this);

    App.db.setClientsForSelectedServices(clients);
    this.set('content.clients', clients);
    console.log("InstallerController.saveClients: saved list ", clients);
  },

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '10':
      case '9':
      case '8':
      case '7':
        this.loadServiceConfigProperties();
      case '6':
        this.loadSlaveComponentHosts();
        this.loadClients();
      case '5':
        this.loadMasterComponentHosts();
        this.loadConfirmedHosts();
      case '4':
        this.loadServices();
      case '3':
        this.loadConfirmedHosts();
      case '2':
        this.load('installOptions');
      case '1':
        this.load('cluster');
    }
  },

  loadAdvancedConfigs: function () {
    var configs = [];
    App.db.getSelectedServiceNames().forEach(function (_serviceName) {
      var serviceComponents = this.loadAdvancedConfig(_serviceName);
      configs = configs.concat(serviceComponents);
    }, this);
    this.set('content.advancedServiceConfig', configs);
    App.db.setAdvancedServiceConfig(configs);
  },

  /**
   * Generate serviceProperties save it to localdata
   * called form stepController step6WizardController
   */
  loadAdvancedConfig: function (serviceName) {
    var self = this;
    var url = (App.testMode) ? '/data/wizard/stack/hdp/version01/' + serviceName + '.json' : App.apiPrefix + App.get('stackVersionURL') + '/services/' + serviceName; // TODO: get this url from the stack selected by the user in Install Options page
    var method = 'GET';
    var serviceComponents;
    $.ajax({
      type: method,
      url: url,
      async: false,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: Step6 submit -> In success function for the loadAdvancedConfig call");
        console.log("TRACE: Step6 submit -> value of the url is: " + url);
        serviceComponents = jsonData.properties;
        serviceComponents.setEach('serviceName', serviceName);
        console.log('TRACE: servicename: ' + serviceName);
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: STep6 submit -> In error function for the loadAdvancedConfig call");
        console.log("TRACE: STep6 submit-> value of the url is: " + url);
        console.log("TRACE: STep6 submit-> error code status is: " + request.status);
        console.log('Step6 submit: Error message is: ' + request.responseText);
      },

      statusCode: require('data/statusCodes')
    });
    return serviceComponents;
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('1');
    this.clearStorageData();
  }

});

