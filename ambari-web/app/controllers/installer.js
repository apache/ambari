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
    configGroups: [],
    slaveGroupProperties: null,
    stacks: null,
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
  /**
   * redefined connectOutlet method to avoid view loading by unauthorized user
   * @param view
   * @param content
   */
  connectOutlet: function(view, content) {
    if(App.db.getAuthenticated()) {
      this._super(view, content);
    }
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
    var dbHosts = this.getDBProperty('hosts');
    hosts.forEach(function (_hostInfo) {
      var host = _hostInfo.hostName;
      delete dbHosts[host];
    });
    this.setDBProperty('hosts', dbHosts);
  },

  /**
   * Load confirmed hosts.
   * Will be used at <code>Assign Masters(step5)</code> step
   */
  loadConfirmedHosts: function () {
    this.set('content.hosts', this.getDBProperty('hosts') || []);
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var servicesInfo = this.getDBProperty('service');
    if(servicesInfo && servicesInfo.length) {
      servicesInfo.forEach(function (item, index) {
        servicesInfo[index] = Em.Object.create(item);
        servicesInfo[index].isInstalled = false;
      });
      this.set('content.services', servicesInfo);
      console.log('installerController.loadServices: loaded data ', JSON.stringify(servicesInfo));
      console.log('selected services ', servicesInfo.filterProperty('isSelected', true).mapProperty('serviceName'));
    } else {
      console.log("Failed to load Services");
   }
  },

  /**
   * total set of hosts registered to cluster, analog of App.Host model,
   * used in Installer wizard until hosts are installed
   */
  allHosts: function () {
    var rawHosts = this.get('content.hosts');
    var masterComponents = this.get('content.masterComponentHosts');
    var slaveComponents = this.get('content.slaveComponentHosts');
    var hosts = [];
    masterComponents.forEach(function (component) {
      var host = rawHosts[component.hostName];
      if (host.hostComponents) {
        host.hostComponents.push(Em.Object.create({
          componentName: component.component,
          displayName: component.display_name
        }));
      } else {
        rawHosts[component.hostName].hostComponents = [
          Em.Object.create({
            componentName: component.component,
            displayName: component.display_name
          })
        ]
      }
    });
    slaveComponents.forEach(function (component) {
      component.hosts.forEach(function (rawHost) {
        var host = rawHosts[rawHost.hostName];
        if (host.hostComponents) {
          host.hostComponents.push(Em.Object.create({
            componentName: component.componentName,
            displayName: component.displayName
          }));
        } else {
          rawHosts[rawHost.hostName].hostComponents = [
            Em.Object.create({
              componentName: component.componentName,
              displayName: component.displayName
            })
          ]
        }
      });
    });

    for (var hostName in rawHosts) {
      var host = rawHosts[hostName];
      var disksOverallCapacity = 0;
      var diskFree = 0;
      host.disk_info.forEach(function (disk) {
        disksOverallCapacity += parseFloat(disk.size);
        diskFree += parseFloat(disk.available);
      });
      hosts.pushObject(Em.Object.create({
          id: host.name,
          ip: host.ip,
          osType: host.os_type,
          osArch: host.os_arch,
          hostName: host.name,
          publicHostName: host.name,
          cpu: host.cpu,
          memory: host.memory,
          diskInfo: host.disk_info,
          diskTotal: disksOverallCapacity / (1024 * 1024),
          diskFree: diskFree / (1024 * 1024),
          hostComponents: host.hostComponents
        }
      ))
    }
    return hosts;
  }.property('content.hosts'),

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
    stackVersions.sortProperty('Versions.stack_version').reverse().forEach(function (version) {
      /*
       * operatingSystems:[
       *  {
       *    osType: 'centos5',
       *    baseUrl: 'http://...',
       *    originalBaseUrl: 'http://...',
       *    defaultBaseUrl: 'http://...',
       *    latestBaseUrl: 'http://...',
       *    mirrorsList: '';
       *  },
       *  {
       *    osType: 'centos6',
       *    baseUrl: 'http://...',
       *    originalBaseUrl: 'http://...',
       *    defaultBaseUrl: 'http://...',
       *    latestBaseUrl: 'http://...',
       *    mirrorsList: '';
       *  },
       * ]
       */
      var oses = [];
      if (version.operatingSystems) {
        version.operatingSystems.forEach(function (os) {
          if (os.repositories) {
            os.repositories.forEach(function (repo) {
              if(repo.Repositories.repo_name == version.Versions.stack_name) {
                var defaultBaseUrl = repo.Repositories.default_base_url || repo.Repositories.base_url;
                var latestBaseUrl = repo.Repositories.latest_base_url || defaultBaseUrl;
                oses.push({
                  osType: os.OperatingSystems.os_type,
                  baseUrl: latestBaseUrl,
                  latestBaseUrl: latestBaseUrl,
                  originalLatestBaseUrl: latestBaseUrl,
                  originalBaseUrl: repo.Repositories.base_url,
                  defaultBaseUrl: defaultBaseUrl,
                  mirrorsList: repo.Repositories.mirrors_list
                });
              }
            });
          }
        });
      }
      result.push(
          Em.Object.create({
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
   * check server version and web client version
   */
  checkServerClientVersion: function () {
    var dfd = $.Deferred();
    var self = this;
    self.getServerVersion().done(function () {
      dfd.resolve();
    });
    return dfd.promise();
  },
  getServerVersion: function(){
    return App.ajax.send({
      name: 'ambari.service.load_server_version',
      sender: this,
      success: 'getServerVersionSuccessCallback',
      error: 'getServerVersionErrorCallback'
    });
  },
  getServerVersionSuccessCallback: function (data) {
    var clientVersion = App.get('version');
    var serverVersion = (data.RootServiceComponents.component_version).toString();
    this.set('versionConflictAlertBody', Em.I18n.t('app.versionMismatchAlert.body').format(serverVersion, clientVersion));
    this.set('isServerClientVersionMismatch', clientVersion != serverVersion);
  },
  getServerVersionErrorCallback: function () {
    console.log('ERROR: Cannot load Ambari server version');
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var serviceNames = [];
    this.setDBProperty('service', stepController.get('content'));
    stepController.filterProperty('isSelected', true).forEach(function (item) {
      serviceNames.push(item.serviceName);
    });
    this.set('content.selectedServiceNames', serviceNames);
    this.setDBProperty('selectedServiceNames', serviceNames);
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
    this.setDBProperty('masterComponentHosts', masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    var masterComponentHosts = this.getDBProperty('masterComponentHosts') || [];
    this.set("content.masterComponentHosts", masterComponentHosts);
    console.log("InstallerController.loadMasterComponentHosts: loaded hosts ", masterComponentHosts);
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = this.getDBProperty('slaveComponentHosts') || null;
    this.set("content.slaveComponentHosts", slaveComponentHosts);
    console.log("InstallerController.loadSlaveComponentHosts: loaded hosts ", slaveComponentHosts);
  },

  /**
   * Load serviceConfigProperties to model
   */
  loadServiceConfigProperties: function () {
    var serviceConfigProperties = this.getDBProperty('serviceConfigProperties');
    this.set('content.serviceConfigProperties', serviceConfigProperties);
    console.log("InstallerController.loadServiceConfigProperties: loaded config ", serviceConfigProperties);

    this.set('content.advancedServiceConfig', this.getDBProperty('advancedServiceConfig'));
  },

  /**
   * Load information about hosts with clients components
   */
  loadClients: function () {
    var clients = this.getDBProperty('clientInfo');
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

    this.setDBProperty('clientInfo', clients);
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
   * Check validation of the customized local urls
   * @param stepController step1WizardController
   */
  checkRepoURL: function (stepController) {
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    selectedStack.set('reload', true);
    var nameVersionCombo = selectedStack.name;
    var stackName = nameVersionCombo.split('-')[0];
    var stackVersion = nameVersionCombo.split('-')[1];
    if (selectedStack && selectedStack.operatingSystems) {
      this.set('validationCnt', selectedStack.get('operatingSystems').filterProperty('selected', true).length);
      this.set('invalidCnt', 0);
      selectedStack.operatingSystems.forEach(function (os) {
        os.errorTitle = null;
        os.errorContent = null;
        if (os.skipValidation) {
          this.set('validationCnt', 0);
        }
        if (os.selected && !os.skipValidation) {
          os.validation = 'icon-repeat';
          selectedStack.set('reload', !selectedStack.get('reload'));
          App.ajax.send({
            name: 'wizard.advanced_repositories.valid_url',
            sender: this,
            data: {
              stackName: stackName,
              stackVersion: stackVersion,
              nameVersionCombo: nameVersionCombo,
              osType: os.osType,
              data: {
                'Repositories': {
                  'base_url': os.baseUrl
                }
              }
            },
            success: 'checkRepoURLSuccessCallback',
            error: 'checkRepoURLErrorCallback'
          });
        }
      }, this);
    }
  },
  setInvalidUrlCnt: function () {
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    selectedStack.set('invalidCnt', this.get('invalidCnt'));
  }.observes('invalidCnt'),
  /**
   * onSuccess callback for check Repo URL.
   */
  checkRepoURLSuccessCallback: function (response, request, data) {
    console.log('Success in check Repo URL. data osType: ' + data.osType );
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      var os = selectedStack.operatingSystems.findProperty('osType', data.osType);
      os.validation = 'icon-ok';
      selectedStack.set('reload', !selectedStack.get('reload'));
      this.set('validationCnt', this.get('validationCnt') - 1);
    }
  },

  /**
   * onError callback for check Repo URL.
   */
  checkRepoURLErrorCallback: function (request, ajaxOptions, error, data) {
    console.log('Error in check Repo URL. The baseURL sent is:  ' + data.data);
    var osType = data.url.split('/')[8];
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      var os = selectedStack.operatingSystems.findProperty('osType', osType);
      os.validation = 'icon-exclamation-sign';
      os.errorTitle = request.status + ":" + request.statusText;
      os.errorContent = $.parseJSON(request.responseText) ? $.parseJSON(request.responseText).message : "";
      selectedStack.set('reload', !selectedStack.get('reload'));
      this.set('validationCnt', this.get('validationCnt') - 1);
      this.set('invalidCnt', this.get('invalidCnt') + 1);
    }
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
        this.loadServiceConfigGroups();
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
    var persists = App.router.get('applicationController').persistKey();
    App.router.get('applicationController').postUserPref(persists,true);
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

