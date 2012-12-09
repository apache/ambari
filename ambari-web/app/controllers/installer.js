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
    hosts: null,
    services: null,
    hostsInfo: [],
    slaveComponentHosts: null,
    masterComponentHosts: null,
    serviceConfigProperties: null,
    advancedServiceConfig: null,
    slaveGroupProperties: null,
    controllerName: 'installerController'
  }),

  getCluster: function(){
    return jQuery.extend(this.get('clusterStatusTemplate'), {});
  },

  /**
   * Load all data for <code>Specify Host(install step2)</code> step
   * Data Example:
   * {
   *   hostNames: '',
   *   manualInstall: false,
   *   sshKey: '',
   *   passphrase: '',
   *   confirmPassphrase: '',
   *   localRepo: false,
   *   localRepoPath: ''
   *   bootRequestId: ''
   * }
   */
  loadInstallOptions: function () {

    if (!this.content.hosts) {
      this.content.hosts = Em.Object.create();
    }

    //TODO : rewire it as model. or not :)
    var hostsInfo = Em.Object.create();

    hostsInfo.hostNames = App.db.getAllHostNamesPattern() || ''; //empty string if undefined

    //TODO : should we check installType for add host wizard????
    var installType = App.db.getInstallType();
    //false if installType not equals 'manual'
    hostsInfo.manualInstall = installType && installType.installType === 'manual' || false;

    var softRepo = App.db.getSoftRepo();
    if (softRepo && softRepo.repoType === 'local') {
      hostsInfo.localRepo = true;
      hostsInfo.localRepopath = softRepo.repoPath;
    } else {
      hostsInfo.localRepo = false;
      hostsInfo.localRepoPath = '';
    }
    hostsInfo.bootRequestId = App.db.getBootRequestId() || null;
    hostsInfo.sshKey = App.db.getSshKey() || '';
    hostsInfo.passphrase = '';
    hostsInfo.confirmPassphrase = '';


    this.set('content.hosts', hostsInfo);
    console.log("InstallerController:loadHosts: loaded data ", hostsInfo);
  },

  /**
   * Save data, which user filled, to main controller
   * @param stepController App.WizardStep2Controller
   */
  saveHosts: function (stepController) {
    //TODO: put data to content.hosts and only then save it)

    //App.db.setBootStatus(false);
    App.db.setAllHostNames(stepController.get('hostNameArr').join("\n"));
    App.db.setAllHostNamesPattern(stepController.get('hostNames'));
    App.db.setBootRequestId(stepController.get('bootRequestId'));
    App.db.setHosts(stepController.getHostInfo());
    if (stepController.get('manualInstall') === false) {
      App.db.setInstallType({installType: 'ambari' });
      App.db.setSshKey(stepController.get('sshKey'));
    } else {
      App.db.setInstallType({installType: 'manual' });
    }
    if (stepController.get('localRepo') === false) {
      App.db.setSoftRepo({ 'repoType': 'remote', 'repoPath': null});
    } else {
      App.db.setSoftRepo({ 'repoType': 'local', 'repoPath': stepController.get('localRepoPath') });
    }
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
   * Save data, which user filled, to main controller
   * @param stepController App.WizardStep3Controller
   */
  saveConfirmedHosts: function (stepController) {
    var hostInfo = {};
    stepController.get('content.hostsInfo').forEach(function (_host) {
      hostInfo[_host.name] = {
        name: _host.name,
        cpu: _host.cpu,
        memory: _host.memory,
        disk_info: _host.disk_info,
        bootStatus: _host.bootStatus,
        isInstalled: false
      };
    });
    console.log('installerController:saveConfirmedHosts: save hosts ', hostInfo);
    App.db.setHosts(hostInfo);
    this.set('content.hostsInfo', hostInfo);
  },

  /**
   * Load confirmed hosts.
   * Will be used at <code>Assign Masters(step5)</code> step
   */
  loadConfirmedHosts: function () {
    this.set('content.hostsInfo', App.db.getHosts() || []);
  },

  /**
   * Save data after installation to main controller
   * @param stepController App.WizardStep9Controller
   */
  saveInstalledHosts: function (stepController) {
    var hosts = stepController.get('hosts');
    var hostInfo = App.db.getHosts();

    for (var index in hostInfo) {
      var host = hosts.findProperty('name', hostInfo[index].name);
      if (host) {
        hostInfo[index].status = host.status;
        //tasks should be empty because they loads from the server
        //hostInfo[index].tasks = host.tasks;
        hostInfo[index].message = host.message;
        hostInfo[index].progress = host.progress;
      }
    }
    App.db.setHosts(hostInfo);
    this.set('content.hostsInfo', hostInfo);
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
   * Save slaveHostComponents to main controller
   * @param stepController called at the submission of step6
   */
  saveSlaveComponentHosts: function (stepController) {

    var hosts = stepController.get('hosts');
    var isMrSelected = stepController.get('isMrSelected');
    var isHbSelected = stepController.get('isHbSelected');

    var dataNodeHosts = [];
    var taskTrackerHosts = [];
    var regionServerHosts = [];
    var clientHosts = [];

    hosts.forEach(function (host) {
      if (host.get('isDataNode')) {
        dataNodeHosts.push({
          hostName: host.hostName,
          group: 'Default',
          isInstalled: false
        });
      }
      if (isMrSelected && host.get('isTaskTracker')) {
        taskTrackerHosts.push({
          hostName: host.hostName,
          group: 'Default',
          isInstalled: false
        });
      }
      if (isHbSelected && host.get('isRegionServer')) {
        regionServerHosts.push({
          hostName: host.hostName,
          group: 'Default',
          isInstalled: false
        });
      }
      if (host.get('isClient')) {
        clientHosts.pushObject({
          hostName: host.hostName,
          group: 'Default',
          isInstalled: false
        });
      }
    }, this);

    var slaveComponentHosts = [];
    slaveComponentHosts.push({
      componentName: 'DATANODE',
      displayName: 'DataNode',
      hosts: dataNodeHosts
    });
    if (isMrSelected) {
      slaveComponentHosts.push({
        componentName: 'TASKTRACKER',
        displayName: 'TaskTracker',
        hosts: taskTrackerHosts
      });
    }
    if (isHbSelected) {
      slaveComponentHosts.push({
        componentName: 'HBASE_REGIONSERVER',
        displayName: 'RegionServer',
        hosts: regionServerHosts
      });
    }
    slaveComponentHosts.pushObject({
      componentName: 'CLIENT',
      displayName: 'client',
      hosts: clientHosts
    });

    App.db.setSlaveComponentHosts(slaveComponentHosts);
    this.set('content.slaveComponentHosts', slaveComponentHosts);
    console.log("InstallerController.saveSlaveComponentHosts: saved hosts ", slaveComponentHosts);
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
        var configProperty = {
          id: _configProperties.get('id'),
          name: _configProperties.get('name'),
          value: _configProperties.get('value'),
          defaultValue: _configProperties.get('defaultValue'),
          service: _configProperties.get('serviceName'),
          domain:  _configProperties.get('domain'),
          filename: _configProperties.get('filename')
        };
        serviceConfigProperties.push(configProperty);
      }, this);

    }, this);

    App.db.setServiceConfigProperties(serviceConfigProperties);
    this.set('content.serviceConfigProperties', serviceConfigProperties);

    var slaveConfigProperties = [];
    stepController.get('stepConfigs').forEach(function (_content) {
      if (_content.get('configCategories').someProperty('isForSlaveComponent', true)) {
        var slaveCategory = _content.get('configCategories').findProperty('isForSlaveComponent', true);
        slaveCategory.get('slaveConfigs.groups').forEach(function (_group) {
          _group.get('properties').forEach(function (_property) {
            var displayType = _property.get('displayType');
            if (displayType === 'directories' || displayType === 'directory') {
              var value = _property.get('value').trim().split(/\s+/g).join(',');
              _property.set('value', value);
            }
            _property.set('storeValue', _property.get('value'));
          }, this);
        }, this);
        slaveConfigProperties.pushObject(slaveCategory.get('slaveConfigs'));
      }
    }, this);
    App.db.setSlaveProperties(slaveConfigProperties);
    this.set('content.slaveGroupProperties', slaveConfigProperties);
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
   * Load properties for group of slaves to model
   */
  loadSlaveGroupProperties: function () {
    var groupConfigProperties = App.db.getSlaveProperties() ? App.db.getSlaveProperties() : this.get('content.slaveComponentHosts');
    if (groupConfigProperties) {
      groupConfigProperties.forEach(function (_slaveComponentObj) {
        if (_slaveComponentObj.groups) {
          var groups = [];
          _slaveComponentObj.groups.forEach(function (_group) {
            var properties = [];
            _group.properties.forEach(function (_property) {
              var property = App.ServiceConfigProperty.create(_property);
              property.set('value', _property.storeValue);
              properties.pushObject(property);
            }, this);
            _group.properties = properties;
            groups.pushObject(App.Group.create(_group));
          }, this);
          _slaveComponentObj.groups = groups;
        }
      }, this);
    }
    this.set('content.slaveGroupProperties', groupConfigProperties);
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
        this.loadSlaveGroupProperties();
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
        this.loadInstallOptions();
      case '1':
        this.load('cluster');
    }
  },

  /**
   * Generate serviceComponents as pr the stack definition  and save it to localdata
   * called form stepController step4WizardController
   */
  loadServiceComponents: function (stepController, displayOrderConfig, apiUrl) {
    var self = this;
    var method = 'GET';
    var testUrl = '/data/wizard/stack/hdp/version/1.2.0.json';
    var url = (App.testMode) ? testUrl : App.apiPrefix + apiUrl;
    $.ajax({
      type: method,
      url: url,
      async: false,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: getService ajax call  -> In success function for the getServiceComponents call");
        console.log("TRACE: jsonData.services : " + jsonData.services);

        // Creating Model
        var Service = Ember.Object.extend({
          serviceName: null,
          displayName: null,
          isDisabled: true,
          isSelected: true,
          description: null,
          version: null
        });

        var data = [];

        // loop through all the service components
        for (var i = 0; i < displayOrderConfig.length; i++) {
          var entry = jsonData.services.findProperty("name", displayOrderConfig[i].serviceName);

          var myService = Service.create({
            serviceName: entry.name,
            displayName: displayOrderConfig[i].displayName,
            isDisabled: i === 0,
            isSelected: true,
            isHidden: displayOrderConfig[i].isHidden,
            description: entry.comment,
            version: entry.version
          });

          data.push(myService);
        }

        stepController.set('serviceComponents', data);
        console.log('TRACE: service components: ' + JSON.stringify(stepController.get('components')));

      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: STep5 -> In error function for the getServiceComponents call");
        console.log("TRACE: STep5 -> value of the url is: " + url);
        console.log("TRACE: STep5 -> error code status is: " + request.status);
        console.log('Step8: Error message is: ' + request.responseText);
      },

      statusCode: require('data/statusCodes')
    });

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
    var url = (App.testMode) ? '/data/wizard/stack/hdp/version01/' + serviceName + '.json' : App.apiPrefix + '/stacks/HDP/version/1.2.0/services/' + serviceName; // TODO: get this url from the stack selected by the user in Install Options page
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
    App.db.setService(undefined); //not to use this data at AddService page
    App.db.setHosts(undefined);
    App.db.setMasterComponentHosts(undefined);
    App.db.setSlaveComponentHosts(undefined);
    App.db.setCluster(undefined);
    App.db.setAllHostNames(undefined);
    App.db.setSlaveProperties(undefined);
    App.db.setSshKey(undefined);
  }

});

