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
    clients:[],
    /**
     * recommendations for host groups loaded from server
     */
    recommendations: null,
    /**
     * recommendationsHostGroups - current component assignment after 5 and 6 steps
     * (uses for host groups validation and to load recommended configs)
     */
    recommendationsHostGroups: null,
    controllerName: 'installerController'
  }),

  /**
   * Wizard properties in local storage, which should be cleaned right after wizard has been finished
   */
  dbPropertiesToClean: [
    'service',
    'hosts',
    'masterComponentHosts',
    'slaveComponentHosts',
    'cluster',
    'allHostNames',
    'installOptions',
    'allHostNamesPattern',
    'serviceComponents',
    'advancedServiceConfig',
    'clientInfo',
    'selectedServiceNames',
    'serviceConfigGroups',
    'serviceConfigProperties',
    'fileNamesToUpdate',
    'bootStatus',
    'stacksVersions',
    'currentStep',
    'serviceInfo',
    'hostInfo',
    'recommendations',
    'recommendationsHostGroups',
    'recommendationsConfigs'
  ],

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
  connectOutlet: function (view, content) {
    if (App.db.getAuthenticated()) {
      this._super(view, content);
    }
  },

  getCluster: function () {
    return jQuery.extend({}, this.get('clusterStatusTemplate'));
  },

  getInstallOptions: function () {
    return jQuery.extend({}, this.get('installOptionsTemplate'));
  },

  getHosts: function () {
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
    this.set('content.hosts', this.getDBProperty('hosts') || {});
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var dfd = $.Deferred();
    var self = this;
    var stackServices = App.StackService.find().mapProperty('serviceName');
    if (!(stackServices && !!stackServices.length && App.StackService.find().objectAt(0).get('stackVersion') == App.get('currentStackVersionNumber'))) {
      this.loadServiceComponents().complete(function () {
        self.set('content.services', App.StackService.find());
        dfd.resolve();
      });
    } else {
      dfd.resolve();
    }
    return dfd.promise();
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
   * stack names used as auxiliary data to query stacks by name
   */
  stackNames: [],

  /**
   * Load stacks data from server or take exist data from local db
   */
  loadStacks: function () {
    var stacks = App.db.getStacks();
    var dfd = $.Deferred();
    if (stacks && stacks.length) {
      var convertedStacks = [];
      stacks.forEach(function (stack) {
        convertedStacks.pushObject(Ember.Object.create(stack));
      });
      App.set('currentStackVersion', convertedStacks.findProperty('isSelected').get('name'));
      this.set('content.stacks', convertedStacks);
      dfd.resolve(true);
    } else {
      App.ajax.send({
        name: 'wizard.stacks',
        sender: this,
        success: 'loadStacksSuccessCallback',
        error: 'loadStacksErrorCallback'
      }).complete(function () {
          dfd.resolve(false);
        });
    }
    return dfd.promise();
  },

  /**
   * Send queries to load versions for each stack
   */
  loadStacksSuccessCallback: function (data) {
    this.get('stacks').clear();
    this.set('stackNames', data.items.mapProperty('Stacks.stack_name'));
  },

  /**
   * set stacks from server to content and local DB
   */
  setStacks: function() {
    var result = this.get('stacks');
    if (!result.length) {
      console.log('Error: there are no active stacks');
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
   * query every stack names from server
   * @return {Array}
   */
  loadStacksVersions: function () {
    var requests = [];
    this.get('stackNames').forEach(function (stackName) {
      requests.push(App.ajax.send({
        name: 'wizard.stacks_versions',
        sender: this,
        data: {
          stackName: stackName
        },
        success: 'loadStacksVersionsSuccessCallback',
        error: 'loadStacksVersionsErrorCallback'
      }));
    }, this);
    return requests;
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
              var defaultBaseUrl = repo.Repositories.default_base_url || repo.Repositories.base_url;
              var latestBaseUrl = repo.Repositories.latest_base_url || defaultBaseUrl;
              if (!App.supports.ubuntu && os.OperatingSystems.os_type == 'ubuntu12') return; // @todo: remove after Ubuntu support confirmation
              oses.push({
                osType: os.OperatingSystems.os_type,
                baseUrl: latestBaseUrl,
                latestBaseUrl: latestBaseUrl,
                originalLatestBaseUrl: latestBaseUrl,
                originalBaseUrl: repo.Repositories.base_url,
                defaultBaseUrl: defaultBaseUrl,
                mirrorsList: repo.Repositories.mirrors_list,
                id: os.OperatingSystems.os_type + repo.Repositories.repo_name,
                repoId: repo.Repositories.repo_id,
                selected: true
              });
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
  getServerVersion: function () {
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
    this.set('ambariServerVersion', serverVersion);
    if (clientVersion) {
      this.set('versionConflictAlertBody', Em.I18n.t('app.versionMismatchAlert.body').format(serverVersion, clientVersion));
      this.set('isServerClientVersionMismatch', clientVersion != serverVersion);
    } else {
      this.set('isServerClientVersionMismatch', false);
    }
  },
  getServerVersionErrorCallback: function () {
    console.log('ERROR: Cannot load Ambari server version');
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var selectedServiceNames = [];
    var installedServiceNames = [];
    stepController.filterProperty('isSelected').forEach(function (item) {
      selectedServiceNames.push(item.get('serviceName'));
    });
    stepController.filterProperty('isInstalled').forEach(function (item) {
      installedServiceNames.push(item.get('serviceName'));
    });
    this.set('content.services', App.StackService.find());
    this.set('content.selectedServiceNames', selectedServiceNames);
    this.setDBProperty('selectedServiceNames', selectedServiceNames);
    this.set('content.installedServiceNames', installedServiceNames);
    this.setDBProperty('installedServiceNames', installedServiceNames);
  },

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   */
  saveMasterComponentHosts: function (stepController) {

    var obj = stepController.get('selectedServicesMasters'),
      hosts = this.getDBProperty('hosts');

    var masterComponentHosts = [];
    obj.forEach(function (_component) {
      masterComponentHosts.push({
        display_name: _component.get('display_name'),
        component: _component.get('component_name'),
        serviceId: _component.get('serviceId'),
        isInstalled: false,
        host_id: hosts[_component.get('selectedHost')].id
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
    var masterComponentHosts = this.getDBProperty('masterComponentHosts'),
      hosts = this.getDBProperty('hosts'),
      host_names = Em.keys(hosts);
    if (Em.isNone(masterComponentHosts)) {
      masterComponentHosts = [];
    }
    else {
      masterComponentHosts.forEach(function (component) {
        for (var i = 0; i < host_names.length; i++) {
          if (hosts[host_names[i]].id === component.host_id) {
            component.hostName = host_names[i];
            break;
          }
        }
      });
    }
    this.set("content.masterComponentHosts", masterComponentHosts);
  },

  loadRecommendations: function() {
    this.set("content.recommendations", this.getDBProperty('recommendations'));
  },

  loadCurrentHostGroups: function() {
    this.set("content.recommendationsHostGroups", this.getDBProperty('recommendationsHostGroups'));
  },

  loadRecommendationsConfigs: function() {
    App.router.set("wizardStep7Controller.recommendationsConfigs", this.getDBProperty('recommendationsConfigs'));
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = this.getDBProperty('slaveComponentHosts'),
      hosts = this.getDBProperty('hosts'),
      host_names = Em.keys(hosts);
    if (!Em.isNone(slaveComponentHosts)) {
      slaveComponentHosts.forEach(function (component) {
        component.hosts.forEach(function (host) {
          for (var i = 0; i < host_names.length; i++) {
            if (hosts[host_names[i]].id === host.host_id) {
              host.hostName = host_names[i];
              break;
            }
          }
        });
      });
    }
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
   * Generate clients list for selected services and save it to model
   * @param stepController step4WizardController
   */
  saveClients: function (stepController) {
    var clients = [];
    var serviceComponents = App.StackServiceComponent.find();
    var services =
      stepController.get('content').filterProperty('isSelected', true).forEach(function (_service) {
        var client = _service.get('serviceComponents').filterProperty('isClient', true);
        client.forEach(function (clientComponent) {
          clients.pushObject({
            component_name: clientComponent.get('componentName'),
            display_name: clientComponent.get('displayName'),
            isInstalled: false
          });
        }, this);
      }, this);
    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
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
   */
  checkRepoURL: function () {
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
        var verifyBaseUrl = os.skipValidation ? false : true;
        if (os.selected) {
          os.validation = 'icon-repeat';
          selectedStack.set('reload', !selectedStack.get('reload'));
          App.ajax.send({
            name: 'wizard.advanced_repositories.valid_url',
            sender: this,
            data: {
              stackName: stackName,
              stackVersion: stackVersion,
              repoId: os.repoId,
              osType: os.osType,
              osId: os.id,
              data: {
                'Repositories': {
                  'base_url': os.baseUrl,
                  "verify_base_url": verifyBaseUrl
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
    console.log('Success in check Repo URL. data osType: ' + data.osType);
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      var os = selectedStack.operatingSystems.findProperty('id', data.osId);
      os.validation = 'icon-ok';
      selectedStack.set('reload', !selectedStack.get('reload'));
      this.set('validationCnt', this.get('validationCnt') - 1);
    }
  },

  /**
   * onError callback for check Repo URL.
   */
  checkRepoURLErrorCallback: function (request, ajaxOptions, error, data, params) {
    console.log('Error in check Repo URL. The baseURL sent is:  ' + data.data);
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.operatingSystems) {
      var os = selectedStack.operatingSystems.findProperty('id', params.osId);
      os.validation = 'icon-exclamation-sign';
      os.errorTitle = request.status + ":" + request.statusText;
      os.errorContent = $.parseJSON(request.responseText) ? $.parseJSON(request.responseText).message : "";
      selectedStack.set('reload', !selectedStack.get('reload'));
      this.set('validationCnt', this.get('validationCnt') - 1);
      this.set('invalidCnt', this.get('invalidCnt') + 1);
    }
  },

  loadMap: {
    '0': [
      {
        type: 'sync',
        callback: function () {
          this.load('cluster');
        }
      }
    ],
    '1': [
      {
        type: 'async',
        callback: function () {
          return this.loadStacks();
        }
      },
      {
        type: 'async',
        callback: function (stacksLoaded) {
          var dfd = $.Deferred();

          if (!stacksLoaded) {
            $.when.apply(this, this.loadStacksVersions()).done(function () {
              dfd.resolve(stacksLoaded);
            });
          } else {
            dfd.resolve(stacksLoaded);
          }

          return dfd.promise();
        }
      },
      {
        type: 'sync',
        callback: function (stacksLoaded) {
          if (!stacksLoaded) {
            this.setStacks();
          }
        }
      }
    ],
    '2': [
      {
        type: 'sync',
        callback: function () {
          this.load('installOptions');
        }
      }
    ],
    '3': [
      {
        type: 'sync',
        callback: function () {
          this.loadConfirmedHosts();
        }
      }
    ],
    '4': [
      {
        type: 'async',
        callback: function () {
          return this.loadServices();
        }
      }
    ],
    '5': [
      {
        type: 'sync',
        callback: function () {
          this.loadMasterComponentHosts();
          this.loadConfirmedHosts();
          this.loadRecommendations();
        }
      }
    ],
    '6': [
      {
        type: 'sync',
        callback: function () {
          this.loadSlaveComponentHosts();
          this.loadClients();
          this.loadRecommendations();
        }
      }
    ],
    '7': [
      {
        type: 'sync',
        callback: function () {
          this.loadServiceConfigGroups();
          this.loadServiceConfigProperties();
          this.loadCurrentHostGroups();
          this.loadRecommendationsConfigs();
        }
      }
    ]
  },
  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('0');
    this.clearStorageData();
    var persists = App.router.get('applicationController').persistKey();
    App.router.get('applicationController').postUserPref(persists, true);
  },

  /**
   * Save cluster provisioning state to the server
   * @param state cluster provisioning state
   * @param callback is called after request completes
   */
  setClusterProvisioningState: function (state, callback) {
    App.ajax.send({
      name: 'cluster.save_provisioning_state',
      sender: this,
      data: {
        state: state
      }
    }).complete(callback());
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

