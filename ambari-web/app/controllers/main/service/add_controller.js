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
App.AddServiceController = App.WizardController.extend({

  name: 'addServiceController',

  totalSteps: 7,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  /**
   * All wizards data will be stored in this variable
   *
   * cluster - cluster name
   * installOptions - ssh key, repo info, etc.
   * services - services list
   * hosts - list of selected hosts
   * slaveComponentHosts, - info about slave hosts
   * masterComponentHosts - info about master hosts
   * config??? - to be described later
   */
  content: Em.Object.create({
    cluster: null,
    hosts: null,
    installOptions: null,
    services: null,
    slaveComponentHosts: null,
    masterComponentHosts: null,
    serviceConfigProperties: null,
    advancedServiceConfig: null,
    controllerName: 'addServiceController',
    configGroups: [],
    additionalClients: []
  }),

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
  },

  /**
   * return new object extended from clusterStatusTemplate
   * @return Object
   */
  getCluster: function(){
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.router.getClusterName()});
  },

  /**
   * Load services data from server.
   */
  loadServicesFromServer: function() {
    if(this.getDBProperty('service')){
      return;
    }
    var apiService = this.loadServiceComponents();
    apiService.forEach(function(item, index){
      apiService[index].isSelected = App.Service.find().someProperty('id', item.serviceName);
      apiService[index].isDisabled = apiService[index].isSelected;
      apiService[index].isInstalled = apiService[index].isSelected;
    });
    this.set('content.services', apiService);
    this.setDBProperty('service', apiService);
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var services = this.getDBProperty('services');
    if (!services) {
      services = {
        selectedServices: [],
        installedServices: []
      };
      App.StackService.find().forEach(function(item){
        var isInstalled = App.Service.find().someProperty('id', item.get('serviceName'));
        item.set('isSelected', isInstalled);
        item.set('isInstalled', isInstalled);
        if (isInstalled) {
          services.selectedServices.push(item.get('serviceName'));
          services.installedServices.push(item.get('serviceName'));
        }
      },this);
      this.setDBProperty('services',services);
    } else {
      App.StackService.find().forEach(function(item) {
        var isSelected =   services.selectedServices.contains(item.get('serviceName'));
        var isInstalled = services.installedServices.contains(item.get('serviceName'));
        item.set('isSelected', isSelected);
        item.set('isInstalled', isInstalled);
      },this);
      var serviceNames = App.StackService.find().filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
      console.log('selected services ', serviceNames);

      this.set('content.skipSlavesStep', !serviceNames.contains('MAPREDUCE') && !serviceNames.contains('HBASE')  && !serviceNames.contains('STORM') && !serviceNames.contains('YARN'));
      if (this.get('content.skipSlavesStep')) {
        this.get('isStepDisabled').findProperty('step', 3).set('value', this.get('content.skipSlavesStep'));
      }
    }
    this.set('content.services', App.StackService.find());
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var serviceNames = [];
    var services = {
      selectedServices: [],
      installedServices: []
    };
    var selectedServices = stepController.get('content').filterProperty('isSelected',true).filterProperty('isInstalled', false).mapProperty('serviceName');
    services.selectedServices.pushObjects(selectedServices);
    services.installedServices.pushObjects(stepController.get('content').filterProperty('isInstalled',true).mapProperty('serviceName'));
    this.setDBProperty('services',services);
    console.log('AddServiceController.saveServices: saved data', stepController.get('content'));

    this.set('content.selectedServiceNames', selectedServices);
    this.setDBProperty('selectedServiceNames',selectedServices);

    this.set('content.skipSlavesStep', !serviceNames.contains('MAPREDUCE') && !serviceNames.contains('HBASE') && !serviceNames.contains('STORM') && !serviceNames.contains('YARN'));
    if (this.get('content.skipSlavesStep')) {
      this.get('isStepDisabled').findProperty('step', 3).set('value', this.get('content.skipSlavesStep'));
    }
  },

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   */
  saveMasterComponentHosts: function (stepController) {
    var obj = stepController.get('selectedServicesMasters');
    var masterComponentHosts = [];
    var installedComponents = App.HostComponent.find();

    obj.forEach(function (_component) {
        masterComponentHosts.push({
          display_name: _component.display_name,
          component: _component.component_name,
          hostName: _component.selectedHost,
          serviceId: _component.serviceId,
          isInstalled: installedComponents.someProperty('componentName', _component.component_name)
        });
    });

    console.log("AddServiceController.saveMasterComponentHosts: saved hosts ", masterComponentHosts);
    this.setDBProperty('masterComponentHosts', masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);

    this.set('content.skipMasterStep', this.get('content.masterComponentHosts').everyProperty('isInstalled', true));
    this.get('isStepDisabled').findProperty('step', 2).set('value', this.get('content.skipMasterStep'));
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    this._super();
    this.set('content.skipMasterStep', this.get('content.masterComponentHosts').everyProperty('isInstalled', true));
    this.get('isStepDisabled').findProperty('step', 2).set('value', this.get('content.skipMasterStep'));
  },

  /**
   * Does service have any configs
   * @param {string} serviceName
   * @returns {boolean}
   */
  isServiceNotConfigurable: function(serviceName) {
    return App.get('services.noConfigTypes').contains(serviceName);
  },

  /**
   * Should Config Step be skipped (based on selected services list)
   * @returns {boolean}
   */
  skipConfigStep: function() {
    var skipConfigStep = true;
    var selectedServices = this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
    selectedServices.map(function(serviceName) {
      skipConfigStep = skipConfigStep && this.isServiceNotConfigurable(serviceName);
    }, this);
    return skipConfigStep;
  },

  loadServiceConfigProperties: function() {
    this._super();
    if (!this.get('content.services')) {
      this.loadServices();
    }
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  saveServiceConfigProperties: function(stepController) {
    this._super(stepController);
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = this.getDBProperty('slaveComponentHosts'),
      hosts = this.getDBProperty('hosts'),
      host_names = Em.keys(hosts);
    if (!Em.isNone(slaveComponentHosts)) {
      slaveComponentHosts.forEach(function(component) {
        component.hosts.forEach(function(host) {
          //Em.set(host, 'hostName', hosts[host.host_id].name);
          for (var i = 0; i < host_names.length; i++) {
            if (hosts[host_names[i]].id === host.host_id) {
              host.hostName = host_names[i];
              break;
            }
          }
        });
      });
    }
    if(!slaveComponentHosts){
      slaveComponentHosts = this.getSlaveComponentHosts();
    }
    this.set("content.slaveComponentHosts", slaveComponentHosts);
    console.log("AddServiceController.loadSlaveComponentHosts: loaded hosts ", slaveComponentHosts);
  },

  /**
   * return slaveComponents bound to hosts
   * @return {Array}
   */
  getSlaveComponentHosts: function () {
    var components = this.get('slaveComponents');
    var result = [];
    var installedServices = App.Service.find().mapProperty('serviceName');
    var selectedServices = this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName');
    var installedComponentsMap = {};
    var uninstalledComponents = [];
    var hosts = this.get('content.hosts');

    components.forEach(function (component) {
      if (installedServices.contains(component.get('serviceName'))) {
        installedComponentsMap[component.get('componentName')] = [];
      } else if (selectedServices.contains(component.get('serviceName'))) {
        uninstalledComponents.push(component);
      }
    }, this);
    installedComponentsMap['HDFS_CLIENT'] = [];

    for (var hostName in hosts) {
      if (hosts[hostName].isInstalled) {
        hosts[hostName].hostComponents.forEach(function (component) {
          if (installedComponentsMap[component.HostRoles.component_name]) {
            installedComponentsMap[component.HostRoles.component_name].push(hostName);
          }
        }, this);
      }
    }

    for (var componentName in installedComponentsMap) {
      var name = (componentName === 'HDFS_CLIENT') ? 'CLIENT' : componentName;
      var component = {
        componentName: name,
        displayName: App.format.role(name),
        hosts: [],
        isInstalled: true
      };
      installedComponentsMap[componentName].forEach(function (hostName) {
        component.hosts.push({
          group: "Default",
          hostName: hostName,
          isInstalled: true
        });
      }, this);
      result.push(component);
    }

    uninstalledComponents.forEach(function (component) {
      var hosts = jQuery.extend(true, [], result.findProperty('componentName', 'DATANODE').hosts);
      hosts.setEach('isInstalled', false);
      result.push({
        componentName: component.get('componentName'),
        displayName: App.format.role(component.get('componentName')),
        hosts: hosts,
        isInstalled: false
      })
    });

    return result;
  },

  /**
   * Generate clients list for selected services and save it to model
   * @param stepController step4WizardController
   */
  saveClients: function(stepController){
    var clients = [];
    var serviceComponents = App.StackServiceComponent.find();
    var clientComponents = [];
    var dbHosts = this.get('content.hosts');

    for (var hostName in dbHosts) {
      dbHosts[hostName].hostComponents.forEach(function (component) {
        clientComponents[component.HostRoles.component_name] = true;
      }, this);
    }

    this.get('content.services').filterProperty('isSelected').forEach(function (_service) {
      var client = serviceComponents.filterProperty('serviceName', _service.serviceName).findProperty('isClient');
      if (client) {
        clients.push({
          component_name: client.get('componentName'),
          display_name: client.get('displayName'),
          isInstalled: !!clientComponents[client.get('componentName')]
        });
      }
    }, this);

    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
    console.log("AddServiceController.saveClients: saved list ", clients);
  },

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '7':
      case '6':
      case '5':
        this.load('cluster');
        this.set('content.additionalClients', []);
      case '4':
        this.loadServiceConfigGroups();
        this.loadServiceConfigProperties();
      case '3':
        this.loadServices();
        this.loadClients();
        this.loadSlaveComponentHosts();//depends on loadServices
      case '2':
        this.loadMasterComponentHosts();
        this.load('hosts');
      case '1':
        this.loadServices();
    }
  },

  /**
   * Remove all loaded data.
   * Created as copy for App.router.clearAllSteps
   */
  clearAllSteps: function () {
    this.clearInstallOptions();
    // clear temporary information stored during the install
    this.set('content.cluster', this.getCluster());
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('1');
    this.clearAllSteps();
    this.clearStorageData();
    App.router.get('updateController').updateAll();
  },

  installServices: function (isRetry, callback) {
    this.set('content.cluster.oldRequestsId', []);
    this.installAdditionalClients();
    if (isRetry) {
      this.getFailedHostComponents(callback);
    }
    else {
      var name = 'common.services.update';
      var data = {
        "context": Em.I18n.t('requestInfo.installServices'),
        "ServiceInfo": {"state": "INSTALLED"},
        "urlParams": "ServiceInfo/state=INIT"
      };
      this.installServicesRequest(name, data, callback);
    }
  },

  installServicesRequest: function (name, data, callback) {
    callback = callback || Em.K;
    App.ajax.send({
      name: name,
      sender: this,
      data: data,
      success: 'installServicesSuccessCallback',
      error: 'installServicesErrorCallback'
    }).then(callback, callback);
  },

  /**
   * installs clients before install new services
   * on host where some components require this
   * @method installAdditionalClients
   */
  installAdditionalClients: function () {
    this.get('content.additionalClients').forEach(function (c) {
      App.ajax.send({
        name: 'common.host.host_component.update',
        sender: this,
        data: {
          hostName: c.hostName,
          componentName: c.componentName,
          serviceName: c.componentName.slice(0, -7),
          context: Em.I18n.t('requestInfo.installHostComponent') + " " + c.hostName,
          HostRoles: {
            state: 'INSTALLED'
          }
        }
      });
    }, this);
  },

  /**
   * List of failed to install HostComponents while adding Service
   */
  failedHostComponents: [],

  getFailedHostComponents: function(callback) {
    callback = this.sendInstallServicesRequest(callback);
    App.ajax.send({
      name: 'wizard.install_services.add_service_controller.get_failed_host_components',
      sender: this,
      success: 'getFailedHostComponentsSuccessCallback',
      error: 'getFailedHostComponentsErrorCallback'
    }).then(callback, callback);
  },

  /**
   * Parse all failed components and filter out installed earlier components (based on selected to install services)
   * @param {Object} json
   */
  getFailedHostComponentsSuccessCallback: function(json) {
    var allFailed = json.items.filterProperty('HostRoles.state', 'INSTALL_FAILED');
    var currentFailed = [];
    var selectedServices = this.getDBProperty('service').filterProperty('isInstalled', false).filterProperty('isSelected', true).mapProperty('serviceName');
    allFailed.forEach(function(failed) {
      if (selectedServices.contains(failed.component[0].ServiceComponentInfo.service_name)) {
        currentFailed.push(failed.HostRoles.component_name);
      }
    });
    this.set('failedHostComponents', currentFailed);
  },

  getFailedHostComponentsErrorCallback: function(request, ajaxOptions, error) {
    console.warn(error);
  },

  sendInstallServicesRequest: function (callback) {
    console.log('failedHostComponents', this.get('failedHostComponents'));
    var name = 'common.host_components.update';
    var data = {
      "context" : Em.I18n.t('requestInfo.installComponents'),
      "query": "HostRoles/component_name.in(" + this.get('failedHostComponents').join(',') + ")",
      "HostRoles": {
        "state": "INSTALLED"
      },
      "urlParams": "HostRoles/state=INSTALLED"
    };
    this.installServicesRequest(name, data, callback);
  }

});
