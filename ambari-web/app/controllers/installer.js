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

  totalSteps: 11,

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

  init: function () {
    this._super();
    this.get('isStepDisabled').setEach('value', true);
    this.get('isStepDisabled').pushObject(Ember.Object.create({
      step: 0,
      value: false
    }));
  },

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
    if(servicesInfo && servicesInfo.length) {
      servicesInfo.forEach(function (item, index) {
        servicesInfo[index] = Em.Object.create(item);
        servicesInfo[index].isInstalled = false;
      });
      this.set('content.services', servicesInfo);
      console.log('installerController.loadServices: loaded data ', JSON.stringify(servicesInfo));
      console.log("The type odf serviceInfo: " + typeof servicesInfo);
      console.log('selected services ', servicesInfo.filterProperty('isSelected', true).mapProperty('serviceName'));
    } else {
      console.log("Failed to load Services");
   }
  },

  stacks: [],

  /**
   * Load stacks data from server or take exist data from local db
   */
  loadStacks: function () {
    var stacks = App.db.getStacks();
    if (stacks && stacks.length) {
      var convertedStacks = [];
      stacks.forEach(function (stack) {
        convertedStacks.pushObject(Ember.Object.create(stack));
      });
      App.set('currentStackVersion', convertedStacks.findProperty('isSelected').get('name'));
      this.set('content.stacks', convertedStacks);
    } else {
      App.ajax.send({
        name: 'wizard.stacks',
        sender: this,
        success: 'loadStacksSuccessCallback',
        error: 'loadStacksErrorCallback'
      });
    }
  },

  /**
   * Send queries to load versions for each stack
   */
  loadStacksSuccessCallback: function (data) {
    var stacks = data.items;
    var result;
    this.get('stacks').clear();
    stacks.forEach(function (stack) {
      App.ajax.send({
        name: 'wizard.stacks_versions',
        sender: this,
        data: {
          stackName: stack.Stacks.stack_name
        },
        success: 'loadStacksVersionsSuccessCallback',
        error: 'loadStacksVersionsErrorCallback'
      });
    }, this);
    result = this.get('stacks');
    if (!result.length) {
      console.log('Error: therea are no active stacks');
    } else {
      var defaultStackVersion = result.findProperty('name', App.defaultStackVersion);
      if (defaultStackVersion) {
        defaultStackVersion.set('isSelected', true)
      } else {
        result.objectAt(0).set('isSelected', true);
      }
    }
    App.db.setStacks(result);
    this.set('content.stacks', result);
  },

  /**
   * onError callback for loading stacks data
   */
  loadStacksErrorCallback: function () {
    console.log('Error in loading stacks');
  },

  /**
   * Parse loaded data and create array of stacks objects
   */
  loadStacksVersionsSuccessCallback: function (data) {
    var result = [];
    var stackVersions = data.items.filterProperty('Versions.active');
    stackVersions.sort(function (a, b) {
      if (a.Versions.stack_version > b.Versions.stack_version) {
        return -1;
      }
      if (a.Versions.stack_version < b.Versions.stack_version) {
        return 1;
      }
      return 0;
    });
    stackVersions.forEach(function (version) {
      /*
       * operatingSystems:[
       *  {
       *    osType: 'centos5',
       *    baseUrl: 'http://...',
       *    originalBaseUrl: 'http://...',
       *    defaultBaseUrl: 'http://...',
       *    mirrorsList: '';
       *  },
       *  {
       *    osType: 'centos6',
       *    baseUrl: 'http://...',
       *    originalBaseUrl: 'http://...',
       *    defaultBaseUrl: 'http://...',
       *    mirrorsList: '';
       *  },
       * ]
       */
      var oses = [];
      if (version.operatingSystems) {
        version.operatingSystems.forEach(function (os) {
          if (os.repositories) {
            os.repositories.forEach(function (repo) {
              if(repo.Repositories.repo_name == version.Versions.stack_name){
                oses.push({
                  osType: os.OperatingSystems.os_type,
                  baseUrl: repo.Repositories.base_url,
                  originalBaseUrl: repo.Repositories.base_url,
                  defaultBaseUrl: repo.Repositories.default_base_url ? 
                      repo.Repositories.default_base_url : repo.Repositories.base_url,
                  mirrorsList: repo.Repositories.mirrors_list
                });
              }
            });
          }
        });
      }
      result.push(
          Ember.Object.create({
            name: version.Versions.stack_name + "-" + version.Versions.stack_version,
            isSelected: false,
            operatingSystems: oses
          })
      );
    }, this);
    this.get('stacks').pushObjects(result);
  },

  /**
   * onError callback for loading stacks data
   */
  loadStacksVersionsErrorCallback: function () {
    console.log('Error in loading stacks');
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
   * Save stacks data to local db
   * @param stepController step1WizardController
   */
  saveStacks: function (stepController) {
    var stacks = stepController.get('content.stacks');
    if (stacks.length) {
      App.set('currentStackVersion', stacks.findProperty('isSelected').get('name'));
    } else {
      App.set('currentStackVersion', App.defaultStackVersion);
    }
    App.db.setStacks(stacks);
    this.set('content.stacks', stacks);
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
        this.loadStacks();
      case '0':
        this.load('cluster');
    }
  },
  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('0');
    this.clearStorageData();
  },

  setStepsEnable: function () {
    for (var i = 0; i <= this.totalSteps; i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      if (i <= this.get('currentStep')) {
        step.set('value', false);
      } else {
        step.set('value', true);
      }
    }
  }.observes('currentStep'),

  setLowerStepsDisable: function (stepNo) {
    for (var i = 0; i < stepNo; i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      step.set('value', true);
    }
  }

});

