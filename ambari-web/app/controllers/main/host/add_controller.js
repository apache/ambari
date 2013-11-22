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

App.AddHostController = App.WizardController.extend({

  name: 'addHostController',

  totalSteps: 7,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  /**
   * All wizards data will be stored in this variable
   *
   * cluster - cluster name
   * hosts - hosts, ssh key, repo info, etc.
   * services - services list
   * hostsInfo - list of selected hosts
   * slaveComponentHosts, hostSlaveComponents - info about slave hosts
   * masterComponentHosts - info about master hosts
   * serviceConfigGroups - info about selected config group for service
   * configGroups - all config groups
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
    controllerName: 'addHostController',
    serviceConfigGroups: null,
    configGroups: null
  }),

  components:require('data/service_components'),

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
   * return new object extended from installOptionsTemplate
   * @return Object
   */
  getInstallOptions: function(){
    return jQuery.extend({}, this.get('installOptionsTemplate'));
  },

  /**
   * return empty hosts array
   * @return Array
   */
  getHosts: function(){
    return [];
  },

  /**
   * Remove host from model. Used at <code>Confirm hosts</code> step
   * @param hosts Array of hosts, which we want to delete
   */
  removeHosts: function (hosts) {
    var dbHosts = this.getDBProperty('hosts');
    hosts.forEach(function (_hostInfo) {
      var host = _hostInfo.hostName;
      delete dbHosts[host];
    });
    this.setDBProperty('hosts', dbHosts);
  },

  /**
   * Load services data from server.
   */
  loadServicesFromServer: function() {
    var displayOrderConfig = require('data/services');
    var apiUrl = App.get('stack2VersionURL');
    var apiService = this.loadServiceComponents(displayOrderConfig, apiUrl);
    //
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
    var servicesInfo = this.getDBProperty('service');
    servicesInfo.forEach(function (item, index) {
      servicesInfo[index] = Em.Object.create(item);
    });
    this.set('content.services', servicesInfo);
    console.log('AddHostController.loadServices: loaded data ', servicesInfo);
    var serviceNames = servicesInfo.filterProperty('isSelected', true).mapProperty('serviceName');
    console.log('selected services ', serviceNames);
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    var masterComponentHosts = this.getDBProperty('masterComponentHosts');
    if (!masterComponentHosts) {
      masterComponentHosts = [];
      App.HostComponent.find().filterProperty('isMaster', true).forEach(function (item) {
        masterComponentHosts.push({
          component: item.get('componentName'),
          hostName: item.get('host.hostName'),
          isInstalled: true,
          serviceId: item.get('service.id'),
          display_name: item.get('displayName')
        })
      });
      this.setDBProperty('masterComponentHosts', masterComponentHosts);
    }
    this.set("content.masterComponentHosts", masterComponentHosts);
    console.log("AddHostController.loadMasterComponentHosts: loaded hosts ", masterComponentHosts);
  },

  /**
   * Save HBase and ZooKeeper to main controller
   * @param stepController
   */
  saveHbZk: function(stepController) {
    var self = this;
    var hosts = stepController.get('hosts');
    var headers = stepController.get('headers');
    var masterComponentHosts = this.getDBProperty('masterComponentHosts');

    headers.forEach(function(header) {
      var rm = masterComponentHosts.filterProperty('component', header.get('name'));
      if(rm) {
        masterComponentHosts.removeObjects(rm);
      }
    });

    headers.forEach(function(header) {
      var component = self.get('components').findProperty('component_name', header.get('name'));
      hosts.forEach(function(host) {
        if (host.get('checkboxes').findProperty('title', component.display_name).checked) {
          masterComponentHosts .push({
            display_name: component.display_name,
            component: component.component_name,
            hostName: host.get('hostName'),
            serviceId: component.service_name,
            isInstalled: false
          });
        }
      });
    });

    console.log("installerController.saveMasterComponentHosts: saved hosts ", masterComponentHosts);
    this.setDBProperty('masterComponentHosts', masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);
  },

  /**
   * return slaveComponents bound to hosts
   * @return {Array}
   */
  getSlaveComponentHosts: function () {
    var components = [
      {
        name: 'DATANODE',
        service: 'HDFS'
      },
      {
        name: 'TASKTRACKER',
        service: 'MAPREDUCE'
      },
      {
        name: 'HBASE_REGIONSERVER',
        service: 'HBASE'
      }
    ];

    var result = [];
    var services = App.Service.find();
    var selectedServices = this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName');
    for (var index = 0; index < components.length; index++) {
      var comp = components[index];
      if (!selectedServices.contains(comp.service)) {
        continue;
      }


      var service = services.findProperty('id', comp.service);
      var hosts = [];

      service.get('hostComponents').filterProperty('componentName', comp.name).forEach(function (host_component) {
        hosts.push({
          group: "Default",
          hostName: host_component.get('host.id'),
          isInstalled: true
        });
      }, this);

      result.push({
        componentName: comp.name,
        displayName: App.format.role(comp.name),
        hosts: hosts,
        isInstalled: true
      })
    }

    var clientsHosts = App.HostComponent.find().filterProperty('componentName', 'HDFS_CLIENT');
    var hosts = [];

    clientsHosts.forEach(function (host_component) {
      hosts.push({
        group: "Default",
        hostName: host_component.get('host.id'),
        isInstalled: true
      });
    }, this);

    result.push({
      componentName: 'CLIENT',
      displayName: 'client',
      hosts: hosts,
      isInstalled: true
    });

    return result;
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = this.getDBProperty('slaveComponentHosts');
    if (!slaveComponentHosts) {
      slaveComponentHosts = this.getSlaveComponentHosts();
    }
    this.set("content.slaveComponentHosts", slaveComponentHosts);
    console.log("AddHostController.loadSlaveComponentHosts: loaded hosts ", slaveComponentHosts);
  },

  /**
   * Load information about hosts with clients components
   */
  loadClients: function () {
    var clients = this.getDBProperty('clientInfo');
    this.set('content.clients', clients);
    console.log("AddHostController.loadClients: loaded list ", clients);
  },

  /**
   * Generate clients list for selected services and save it to model
   * @param stepController step4WizardController
   */
  saveClients: function () {
    var clients = [];
    var serviceComponents = require('data/service_components');
    var hostComponents = App.HostComponent.find();

    this.get('content.services').filterProperty('isSelected', true).forEach(function (_service) {
      var client = serviceComponents.filterProperty('service_name', _service.serviceName).findProperty('isClient', true);
      if (client) {
        clients.pushObject({
          component_name: client.component_name,
          display_name: client.display_name,
          isInstalled: hostComponents.filterProperty('componentName', client.component_name).length > 0
        });
      }
    }, this);

    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
    console.log("AddHostController.saveClients: saved list ", clients);
  },

  /**
   *  Apply config groups from step4 Configurations
   */
  applyConfigGroup: function () {
    var serviceConfigGroups = this.get('content.serviceConfigGroups');
    serviceConfigGroups.forEach(function (group){
      if(group.selectedConfigGroup != "Default") {
        var configGroup = group.configGroups.findProperty('ConfigGroup.group_name', group.selectedConfigGroup);
        group.hosts.forEach(function(host){
          configGroup.ConfigGroup.hosts.push({
            host_name: host
          });
        },this);
        delete configGroup.href;
        App.ajax.send({
          name: 'config_groups.update_config_group',
          sender: this,
          data: {
            id: configGroup.ConfigGroup.id,
            configGroup: configGroup
          }
        });
      }
    },this);
  },

  /**
   * Load information about selected config groups
   */
  getServiceConfigGroups: function () {
    var serviceConfigGroups = this.getDBProperty('serviceConfigGroups');
    this.set('content.serviceConfigGroups', serviceConfigGroups);
  },

  /**
   * Save information about selected config groups
   */
  saveServiceConfigGroups: function () {
    this.setDBProperty('serviceConfigGroups', this.get('content.serviceConfigGroups'));
    this.set('content.serviceConfigGroups', this.get('content.serviceConfigGroups'));
  },

  /**
   * Set content.serviceConfigGroups for step4
   */
  loadServiceConfigGroups: function () {
    var slaveComponentHosts = this.get('content.slaveComponentHosts');
    var selectedServices = [];
    var selectedClientHosts = slaveComponentHosts.findProperty('componentName','CLIENT').hosts.mapProperty('hostName');

    slaveComponentHosts.forEach(function (slave){
      if(slave.hosts.length > 0){
        if(slave.componentName != "CLIENT"){
          var service = App.HostComponent.find().findProperty('componentName', slave.componentName).get('service');
          var configGroups = this.get('content.configGroups').filterProperty('ConfigGroup.tag', service.get('id'));
          var configGroupsNames = configGroups.mapProperty('ConfigGroup.group_name')
          configGroupsNames.push('Default');
          selectedServices.push({
            serviceId : service.get('id'),
            displayName : service.get('displayName'),
            hosts : slave.hosts.mapProperty('hostName'),
            configGroupsNames : configGroupsNames,
            configGroups : configGroups,
            selectedConfigGroup: "Default"
          });
        }
      }
    },this);
    if(selectedClientHosts.length > 0){
      this.loadClients();
      var clients = this.get('content.clients');
      clients.forEach(function (client) {
        var service = App.HostComponent.find().findProperty('componentName', client.component_name).get('service');
        var serviceMatch = selectedServices.findProperty('serviceId',service.get('id'));
        if(serviceMatch){
          serviceMatch.hosts = serviceMatch.hosts.concat(selectedClientHosts).uniq();
        }else{
          var configGroups = this.get('content.configGroups').filterProperty('ConfigGroup.tag', service.get('id'));
          var configGroupsNames = configGroups.mapProperty('ConfigGroup.group_name')
          configGroupsNames.push('Default');
          selectedServices.push({
            serviceId : service.get('id'),
            displayName : service.get('displayName'),
            hosts : selectedClientHosts,
            configGroupsNames : configGroupsNames,
            configGroups : configGroups,
            selectedConfigGroup: "Default"
          });
        }
      },this);
    }
    this.set('content.serviceConfigGroups', selectedServices);
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
        this.loadServiceConfigProperties();
        this.getServiceConfigGroups();
      case '4':
      case '3':
        this.loadClients();
        this.loadServices();
        this.loadMasterComponentHosts();
        this.loadSlaveComponentHosts();
        this.load('hosts');
      case '2':
        this.loadServices();
      case '1':
        this.load('hosts');
        this.load('installOptions');
        this.load('cluster');
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
    App.updater.immediateRun('updateHost');
  },

  installServices: function (isRetry) {
    this.set('content.cluster.oldRequestsId', []);
    var clusterName = this.get('content.cluster.name');
    var data;
    var name;
    var hostnames = [];
    for (var hostname in this.getDBProperty('hosts')) {
      hostnames.push(hostname);
    }

    if (isRetry) {
      name = 'wizard.install_services.add_host_controller.is_retry';
    }
    else {
      name = 'wizard.install_services.add_host_controller.not_is_retry';
    }
    data = {
      "RequestInfo": {
        "context": Em.I18n.t('requestInfo.installComponents'),
        "query": "HostRoles/host_name.in(" + hostnames.join(',') + ")"
      },
      "Body": {
        "HostRoles": {"state": "INSTALLED"}
      }
    };
    data = JSON.stringify(data);
    App.ajax.send({
      name: name,
      sender: this,
      data: {
        data: data,
        cluster: clusterName
      },
      success: 'installServicesSuccessCallback',
      error: 'installServicesErrorCallback'
    });
  }

});
