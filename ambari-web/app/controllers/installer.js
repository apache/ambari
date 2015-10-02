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
var stringUtils = require('utils/string_utils');

App.InstallerController = App.WizardController.extend({

  name: 'installerController',

  isCheckInProgress: false,

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
    clients: [],
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
      value: true
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
      var host = _hostInfo.name;
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
          hostComponents: host.hostComponents || []
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
   * Load stacks data from server or take exist data from in memory variable {{content.stacks}}
   * The series of API calls will be called  When landing first time on Select Stacks page
   * or on hitting refresh post select stacks page in installer wizard
   */
  loadStacks: function () {
    var stacks = this.get('content.stacks');
    var dfd = $.Deferred();
    App.StackConfigProperty.find().clear();
    App.Section.find().clear();
    App.SubSection.find().clear();
    App.SubSectionTab.find().clear();
    App.Tab.find().clear();
    this.set('stackConfigsLoaded', false);
    if (stacks && stacks.get('length')) {
      App.set('currentStackVersion', App.Stack.find().findProperty('isSelected').get('id'));
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
    this.set('loadStacksRequestsCounter', requests.length);
    return requests;
  },

  /**
   * Counter for counting number of successful requests to load stack versions
   */
  loadStacksRequestsCounter: 0,

  /**
   * Parse loaded data and create array of stacks objects
   */
  loadStacksVersionsSuccessCallback: function (data) {
    var stacks = App.db.getStacks();
    var isStacksExistInDb = stacks && stacks.length;
    if (isStacksExistInDb) {
      stacks.forEach(function (_stack) {
        var stack = data.items.filterProperty('Versions.stack_name', _stack.stack_name).findProperty('Versions.stack_version', _stack.stack_version);
        if (stack) {
          stack.Versions.is_selected = _stack.is_selected;
        }
      }, this);
    }
    App.stackMapper.map(data);
    if (!this.decrementProperty('loadStacksRequestsCounter')) {
      if (!isStacksExistInDb) {
        var defaultStackVersion = App.Stack.find().findProperty('id', App.defaultStackVersion);
        if (defaultStackVersion) {
          defaultStackVersion.set('isSelected', true)
        } else {
          App.Stack.find().objectAt(0).set('isSelected', true);
        }
      }
      this.set('content.stacks', App.Stack.find());
      App.set('currentStackVersion', App.Stack.find().findProperty('isSelected').get('id'));
    }
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
      name: 'ambari.service',
      sender: this,
      data: {
        fields: '?fields=RootServiceComponents/component_version,RootServiceComponents/properties/server.os_family&minimal_response=true'
      },
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
    App.set('isManagedMySQLForHiveEnabled', App.config.isManagedMySQLForHiveAllowed(data.RootServiceComponents.properties['server.os_family']));
  },
  getServerVersionErrorCallback: function () {
    console.log('ERROR: Cannot load Ambari server version');
  },

  /**
   * set stacks from server to content and local DB
   */
  setStacks: function () {
    var result = App.Stack.find() || [];
    Em.assert('Stack model is not populated', result.get('length'));
    App.db.setStacks(result.slice());
    this.set('content.stacks', result);
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
    this.set('content.installedServiceNames', installedServiceNames);
    this.setDBProperties({
      selectedServiceNames: selectedServiceNames,
      installedServiceNames: installedServiceNames
    });
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
    var props = this.getDBProperties(['masterComponentHosts', 'hosts']);
    var masterComponentHosts = props.masterComponentHosts,
      hosts = props.hosts || {},
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

  loadCurrentHostGroups: function () {
    this.set("content.recommendationsHostGroups", this.getDBProperty('recommendationsHostGroups'));
  },

  loadRecommendationsConfigs: function () {
    App.router.set("wizardStep7Controller.recommendationsConfigs", this.getDBProperty('recommendationsConfigs'));
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var props = this.getDBProperties(['slaveComponentHosts', 'hosts']);
    var slaveComponentHosts = props.slaveComponentHosts,
      hosts = props.hosts || {},
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
  },
  /**
   * Generate clients list for selected services and save it to model
   * @param stepController step4WizardController
   */
  saveClients: function (stepController) {
    var clients = [];
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
   * Check validation of the customized local urls
   */
  checkRepoURL: function (wizardStep1Controller) {
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    selectedStack.set('reload', true);
    var nameVersionCombo = selectedStack.get('id');
    var stackName = nameVersionCombo.split('-')[0];
    var stackVersion = nameVersionCombo.split('-')[1];
    var dfd = $.Deferred();
    if (selectedStack && selectedStack.get('operatingSystems')) {
      this.set('validationCnt', selectedStack.get('repositories').filterProperty('isSelected').length);
      var verifyBaseUrl = !wizardStep1Controller.get('skipValidationChecked');
      selectedStack.get('operatingSystems').forEach(function (os) {
        if (os.get('isSelected')) {
          os.get('repositories').forEach(function (repo) {
            repo.setProperties({
              errorTitle: '',
              errorContent: '',
              validation: App.Repository.validation['INPROGRESS']
            });
            this.set('content.isCheckInProgress', true);
            App.ajax.send({
              name: 'wizard.advanced_repositories.valid_url',
              sender: this,
              data: {
                stackName: stackName,
                stackVersion: stackVersion,
                repoId: repo.get('repoId'),
                osType: os.get('osType'),
                osId: os.get('id'),
                dfd: dfd,
                data: {
                  'Repositories': {
                    'base_url': repo.get('baseUrl'),
                    "verify_base_url": verifyBaseUrl
                  }
                }
              },
              success: 'checkRepoURLSuccessCallback',
              error: 'checkRepoURLErrorCallback'
            });
          }, this);
        }
      }, this);
    }
    return dfd.promise();
  },
  /**
   * onSuccess callback for check Repo URL.
   */
  checkRepoURLSuccessCallback: function (response, request, data) {
    console.log('Success in check Repo URL. data osType: ' + data.osType);
    var selectedStack = this.get('content.stacks').findProperty('isSelected');
    if (selectedStack && selectedStack.get('operatingSystems')) {
      var os = selectedStack.get('operatingSystems').findProperty('id', data.osId);
      var repo = os.get('repositories').findProperty('repoId', data.repoId);
      if (repo) {
        repo.set('validation', App.Repository.validation['OK']);
      }
    }
    this.set('validationCnt', this.get('validationCnt') - 1);
    if (!this.get('validationCnt')) {
      this.set('content.isCheckInProgress', false);
      data.dfd.resolve();
    }
  },

  /**
   * onError callback for check Repo URL.
   */
  checkRepoURLErrorCallback: function (request, ajaxOptions, error, data, params) {
    console.log('Error in check Repo URL. The baseURL sent is:  ' + data.data);
    var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
    if (selectedStack && selectedStack.get('operatingSystems')) {
      var os = selectedStack.get('operatingSystems').findProperty('id', params.osId);
      var repo = os.get('repositories').findProperty('repoId', params.repoId);
      if (repo) {
        repo.setProperties({
          validation: App.Repository.validation['INVALID'],
          errorTitle: request.status + ":" + request.statusText,
          errorContent: $.parseJSON(request.responseText) ? $.parseJSON(request.responseText).message : ""
        });
      }
    }
    this.set('content.isCheckInProgress', false);
    params.dfd.reject();
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
          var dfd = $.Deferred();

          this.loadStacks().always(function() {
            App.router.get('clusterController').loadAmbariProperties().always(function() {
              dfd.resolve();
            });
          });

          return dfd.promise();
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
          this.setSkipSlavesStep(App.StackService.find().filterProperty('isSelected'), 6);
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
        type: 'async',
        callback: function () {
          this.loadServiceConfigGroups();
          this.loadServiceConfigProperties();
          this.loadCurrentHostGroups();
          this.loadRecommendationsConfigs();
          return this.loadConfigThemes();
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
    App.router.get('userSettingsController').postUserPref('show_bg', true);
  },

  /**
   * Save cluster provisioning state to the server
   * @param state cluster provisioning state
   */
  setClusterProvisioningState: function (state) {
    return App.ajax.send({
      name: 'cluster.save_provisioning_state',
      sender: this,
      data: {
        state: state
      }
    });
  },

  setStepsEnable: function () {
    for (var i = 0; i <= this.totalSteps; i++) {
      this.get('isStepDisabled').findProperty('step', i).set('value', i > this.get('currentStep'));
    }
  }.observes('currentStep'),

  setLowerStepsDisable: function (stepNo) {
    for (var i = 0; i < stepNo; i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      step.set('value', true);
    }
  },


  /**
   * Compare jdk versions used for ambari and selected stack.
   * Validation check will fire only for non-custom jdk configuration.
   *
   * @param {Function} successCallback
   * @param {Function} failCallback
   */
  validateJDKVersion: function (successCallback, failCallback) {
    var selectedStack = App.Stack.find().findProperty('isSelected', true),
        currentJDKVersion = App.router.get('clusterController.ambariProperties')['java.version'],
        // use min as max, or max as min version, in case when some of them missed
        minJDKVersion = selectedStack.get('minJdkVersion') || selectedStack.get('maxJdkVersion'),
        maxJDKVersion = selectedStack.get('maxJdkVersion') || selectedStack.get('minJdkVersion'),
        t = Em.I18n.t,
        fCallback = failCallback || function() {},
        sCallback = successCallback || function() {};

    // Skip jdk check if min and max required version not set in stack definition.
    if (!minJDKVersion && !maxJDKVersion) {
      sCallback();
      return;
    }

    if (currentJDKVersion) {
      if (stringUtils.compareVersions(currentJDKVersion, minJDKVersion) < 0 ||
          stringUtils.compareVersions(maxJDKVersion, currentJDKVersion) < 0) {
        // checks and process only minor part for now
        var versionDistance = parseInt(maxJDKVersion.split('.')[1]) - parseInt(minJDKVersion.split('.')[1]);
        var versionsList = [minJDKVersion];
        for (var i = 1; i < (versionDistance + 1); i++) {
          versionsList.push("" + minJDKVersion.split('.')[0] + '.' + (+minJDKVersion.split('.')[1] + i));
        }
        var versionsString = stringUtils.getFormattedStringFromArray(versionsList, t('or'));
        var popupBody = t('popup.jdkValidation.body').format(selectedStack.get('stackName') + ' ' + selectedStack.get('stackVersion'), versionsString, currentJDKVersion);
        App.showConfirmationPopup(sCallback, popupBody, fCallback, t('popup.jdkValidation.header'), t('common.proceedAnyway'), true);
        return;
      }
    }
    sCallback();
  }

});
