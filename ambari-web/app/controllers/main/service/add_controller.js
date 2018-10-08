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

App.AddServiceController = App.WizardController.extend(App.AddSecurityConfigs, App.Persist, {

  name: 'addServiceController',

  isCheckInProgress: false,

  steps: [
    "configureDownload",
    "selectMpacks",
    "customMpackRepos",
    "downloadMpacks",
    "customProductRepos",
    "verifyProducts",
    "step5",
    "step6",
    "step7",
    "step8",
    "step9",
    "step10"
  ],

    /**
   * @type {string}
   */
  displayName: Em.I18n.t('services.add.header'),

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  errors: [],

  hasErrors: function () {
    return this.get('errors').length > 0;
  }.property('errors'),

  addError: function (newError) {
    const errors = this.get('errors');
    this.set('errors', errors.concat(newError));
  },

  clearErrors: function () {
    this.set('errors', []);
  },

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
    // list of components, that was added from configs page via AssignMasterOnStep7Controller
    componentsFromConfigs: [],
    /**
     * recommendations for host groups loaded from server
     */
    recommendations: null,
    /**
     * recommendationsHostGroups - current component assignment after 5 and 6 steps,
     * or adding hiveserver2 interactive on "configure services" page
     * (uses for host groups validation and to load recommended configs)
     */
    recommendationsHostGroups: null,
    controllerName: 'addServiceController',
    mpacks: [],
    registeredMpacks: [],
    mpackVersions: [],
    mpackServiceVersions: [],
    mpackServices: [],
    serviceGroups: [],
    serviceInstances: [],
    // Tracks which steps have been saved before.
    // If you revisit a step, we will know if the step has been saved previously and we can warn about making changes.
    // If a previously saved step is changed, setStepSaved() will "unsave" all subsequent steps so we don't warn on every screen.
    // Furthermore, we only need to track this state for steps that have an affect on subsequent steps.
    stepsSavedState: null
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
    'clients',
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
    'recommendationsConfigs',
    'componentsFromConfigs',
    'operatingSystems',
    'repositories',
    'selectedMpacks',
    'mpacksToRegister',
    'selectedServices',
    'selectedStack',
    'downloadConfig',
    'stepsSavedState',
    'serviceGroups',
    'addedServiceGroups',
    'serviceInstances',
    'addedServiceInstances',
    'registeredMpacks'
  ],

  init: function () {
    this._super();

    //enable first step, which is at index 0 in this wizard
    const stepAtIndex0 = this.get('isStepDisabled').findProperty('step', 0)
    if (stepAtIndex0) {
      stepAtIndex0.set('value', false);
    }
  },

  /**
   * Load data for services selected from mpacks. Will be used at <code>Download Mpacks</code> step submit action.
   *
   * @param  {string} stackName
   * @param  {string} stackVersion
   * @param  {string} serviceName
   */
  loadMpackServiceInfo: function (stackName, stackVersion, serviceName) {
    return App.ajax.send({
      name: 'wizard.mpack_service_components',
      sender: this,
      data: {
        stackName: stackName,
        stackVersion: stackVersion,
        serviceName: serviceName
      }
    });
  },

  loadMpackServiceInfoSuccess: function (serviceInfo) {
    serviceInfo.StackServices.is_selected = true;
    App.MpackServiceMapper.map(serviceInfo);
  },

  loadMpackServiceInfoError: function (request, status, error) {
    console.log(`${message} ${status} - ${error}`);
    
    const message = Em.I18n.t('installer.error.mpackServiceInfo');
    this.addError(message);
    return message;   
  },

  allServiceGroups: function () {
    return [].concat(this.get('content.serviceGroups')).concat(this.get('content.addedServiceGroups'));
  }.property('content.serviceGroups', 'content.addedServiceGroups'),

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   * @param  skip  {Boolean}
   */
  saveMasterComponentHosts: function (stepController, skip) {
    var obj = stepController.get('selectedServicesMasters'),
      hosts = this.getDBProperty('hosts');

    var masterComponentHosts = [];
    obj.forEach(function (_component) {
      masterComponentHosts.push({
        display_name: _component.get('display_name'),
        component: _component.get('component_name'),
        serviceId: _component.get('serviceId'),
        serviceGroupName: _component.get('mpackInstance'),
        isInstalled: false,
        host_id: hosts[_component.get('selectedHost')].id
      });
    });

    this.set('content.masterComponentHosts', masterComponentHosts);
    if (!skip) {
      this.setDBProperty('masterComponentHosts', masterComponentHosts);
    }
  },

  /**
   * Load master component hosts data for using in required step controllers
   * @param inMemory {Boolean}: Load master component hosts from memory
   */
  loadMasterComponentHosts: function (lookInMemoryOnly) {
    var props = this.getDBProperties(['masterComponentHosts', 'hosts']),
      masterComponentHosts = this.get("content.masterComponentHosts"),
      hosts = props.hosts || {},
      hostNames = Em.keys(hosts);

    if (!lookInMemoryOnly && !masterComponentHosts) {
      masterComponentHosts = props.masterComponentHosts;
    }

    if (Em.isNone(masterComponentHosts)) {
      masterComponentHosts = [];
    } else {
      masterComponentHosts.forEach(function (component) {
        for (var i = 0; i < hostNames.length; i++) {
          if (hosts[hostNames[i]].id === component.host_id) {
            component.hostName = hostNames[i];
            break;
          }
        }
      });
    }
    this.set("content.masterComponentHosts", masterComponentHosts);
  },

  loadCurrentHostGroups: function () {
    this.set("content.recommendations", this.getDBProperty('recommendations'));
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
      hostNames = Em.keys(hosts);
    if (!Em.isNone(slaveComponentHosts)) {
      slaveComponentHosts.forEach(function (component) {
        component.hosts.forEach(function (host) {
          for (var i = 0; i < hostNames.length; i++) {
            if (hosts[hostNames[i]].id === host.host_id) {
              host.hostName = hostNames[i];
              break;
            }
          }
        });
      });
    }
    this.set("content.slaveComponentHosts", slaveComponentHosts);
  },

  loadMap: {
    'selectMpacks': [
      {
        type: 'async',
        callback: function () {
          const self = this;
          return this.loadRegisteredMpacks()
          .done(this.loadServiceGroups.bind(this))
          .done(() => {
            self.load('stepsSavedState');
            self.load('cluster');
            self.load('selectedServices');
            self.load('selectedMpacks');
            self.load('addedServiceGroups');
            self.load('addedServiceInstances');
            self.load('advancedMode');
            
            const dfd = $.Deferred();
            dfd.resolve();
            return dfd.promise();
          });
        }
      }
    ],
    'customProductRepos': [
      {
        type: 'async',
        callback: function () {
          return this.loadSelectedServiceInfo(this.getStepSavedState('customProductRepos'));
        }
      },
    ],
    'step3': [
      {
        type: 'async',
        callback: function () {
          return this.loadHosts();
        }
      }
    ],
    'step5': [
      {
        type: 'sync',
        callback: function () {
          this.setSkipSlavesStep(App.StackService.find().filterProperty('isSelected'), this.getStepIndex('step7'));
          this.loadMasterComponentHosts();
          this.loadConfirmedHosts();
          this.loadComponentsFromConfigs();
          this.loadRecommendations();
        }
      }
    ],
    'step6': [
      {
        type: 'sync',
        callback: function () {
          this.loadSlaveComponentHosts();
          this.loadClients();
          this.loadComponentsFromConfigs();
          this.loadRecommendations();
        }
      }
    ],
    'step7': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();
          var self = this;
          this.loadServiceConfigGroups();
          this.loadCurrentHostGroups();
          this.loadRecommendationsConfigs();
          this.loadComponentsFromConfigs();
          this.loadConfigThemes().then(function() {
            self.loadServiceConfigProperties();
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ],
    'step8': [
      {
        type: 'sync',
        callback: function () {
          this.load('selectedStack');
        }
      }
    ]
  },

  gotoStep5: function () {
    this.gotoStep('step5');
  },

  gotoStep6: function () {
    this.gotoStep('step6');
  },

  gotoStep7: function () {
    this.gotoStep('step7');
  },

  gotoStep8: function () {
    this.gotoStep('step8');
  },

  gotoStep9: function () {
    this.gotoStep('step9');
  },

  gotoStep10: function () {
    this.gotoStep('step10');
  },

  gotoConfigureDownload: function () {
    this.gotoStep('configureDownload');
  },
  
  gotoSelectMpacks: function () {
    this.gotoStep('selectMpacks');
  },

  gotoCustomMpackRepos: function () {
    this.gotoStep('customMpackRepos');
  },

  gotoDownloadMpacks: function () {
    this.gotoStep('downloadMpacks');
  },

  gotoCustomProductRepos: function () {
    this.gotoStep('customProductRepos');
  },

  gotoVerifyProducts: function () {
    this.gotoStep('verifyProducts');
  },

  isStep5: function () {
    return this.get('currentStep') == this.getStepIndex('step5');
  }.property('currentStep'),

  isStep6: function () {
    return this.get('currentStep') == this.getStepIndex('step6');
  }.property('currentStep'),

  isStep7: function () {
    return this.get('currentStep') == this.getStepIndex('step7');
  }.property('currentStep'),

  isStep8: function () {
    return this.get('currentStep') == this.getStepIndex('step8');
  }.property('currentStep'),

  isStep9: function () {
    return this.get('currentStep') == this.getStepIndex('step9');
  }.property('currentStep'),

  isStep10: function () {
    return this.get('currentStep') == this.getStepIndex('step10');
  }.property('currentStep'),

  isConfigureDownload: function () {
    return this.get('currentStep') == this.getStepIndex('configureDownload');
  }.property('currentStep'),

  isSelectMpacks: function () {
    return this.get('currentStep') == this.getStepIndex('selectMpacks');
  }.property('currentStep'),

  isCustomMpackRepos: function () {
    return this.get('currentStep') == this.getStepIndex('customMpackRepos');
  }.property('currentStep'),

  isDownloadMpacks: function () {
    return this.get('currentStep') == this.getStepIndex('downloadMpacks');
  }.property('currentStep'),

  isCustomProductRepos: function () {
    return this.get('currentStep') == this.getStepIndex('customProductRepos');
  }.property('currentStep'),

  isVerifyProducts: function () {
    return this.get('currentStep') == this.getStepIndex('verifyProducts');
  }.property('currentStep'),

  clearConfigActionComponents: function() {
    var masterComponentHosts = this.get('content.masterComponentHosts');
    var componentsAddedFromConfigAction = this.get('content.componentsFromConfigs');

    if (componentsAddedFromConfigAction && componentsAddedFromConfigAction.length) {
      componentsAddedFromConfigAction.forEach(function(_masterComponent){
        masterComponentHosts = masterComponentHosts.rejectProperty('component', _masterComponent);
      });
    }
    this.set('content.masterComponentHosts', masterComponentHosts);
    this.setDBProperty('masterComponentHosts', masterComponentHosts);
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('0');
    this.clearStorageData();
    this.clearServiceConfigProperties();
    App.router.get('userSettingsController').postUserPref('show_bg', true);
    App.themesMapper.resetModels();
  },

  clearStackServices: function (deleteAll) {
    var dfd = $.Deferred();

    if (deleteAll) {
      const stackServices = App.StackService.find();
      let stackServicesCount = stackServices.content.length;

      if (stackServicesCount > 0) {
        stackServices.forEach(service => {
          Em.run.once(this, () => {
            App.MpackServiceMapper.deleteRecord(service);
            stackServicesCount--;

            if (stackServicesCount === 0) {
              dfd.resolve();
            }
          });
        });
      } else {
        dfd.resolve();
      }  
    } else {
      dfd.resolve();
    }  

    return dfd.promise();
  },

  /**
   * Updates the stepsSaved array based on the stepName provided.
   * If the passed step is already saved, then nothing is changed.
   * Otherwise, the passed step is set to saved and all subsequent steps are set to unsaved.
   *
   * @param  {type} stepName Name of the step being saved.
   */
  setStepSaved: function (stepName) {
    const stepIndex = this.getStepIndex(stepName);
    const oldState = this.get('content.stepsSavedState') || {};
    const newState = Em.Object.create(oldState);

    if (!newState[stepIndex]) {
      for (let i = stepIndex + 1, length = this.get('steps').length; i < length; i++) {
        newState[i] = false;
      };

      newState[stepIndex] = true;

      this.set('content.stepsSavedState', newState);
      this.save('stepsSavedState');
    }
  },

  /**
   * Populates the StackService model from the "stack" info that was created when mpacks were registered in the Download Mpack step.
   * Then, it locally persists info about the selected services.
   *
   * @param {Boolean} keepStackServices If true, previously loaded stack services are retained.
   *                                    This is to support back/forward navigation in the wizard
   *                                    and should correspond to the saved state of the step after Download Mpacks.
   * @return {object} a promise
   */
  loadSelectedServiceInfo: function (keepStackServices) {
    var dfd = $.Deferred();

    this.clearStackServices(!keepStackServices).then(() => {
      //get info about services from specific stack versions and save to StackService model
      this.set('content.selectedServiceNames', this.getDBProperty('selectedServiceNames'));
      const selectedServices = this.get('content.selectedServices');
      const servicePromises = selectedServices.map(service =>
        this.loadMpackServiceInfo(service.mpackName, service.mpackVersion, service.name)
          .then(this.loadMpackServiceInfoSuccess.bind(this), this.loadMpackServiceInfoError.bind(this))
      );

      return $.when(...servicePromises);
    }).then(() => {
      const services = App.StackService.find();
      this.set('content.services', services);

      const clients = [];
      services.forEach(service => {
        const client = service.get('serviceComponents').filterProperty('isClient', true);
        client.forEach(clientComponent => {
          clients.pushObject({
            component_name: clientComponent.get('componentName'),
            service_name: service.get('serviceName'),
            serviceGroupName: service.get('stackName'),
            display_name: clientComponent.get('displayName'),
            isInstalled: false
          });
        });
      });
      this.set('content.clients', clients);
      this.save('clients');

      dfd.resolve();
    });
    
    return dfd.promise();
  },

  /**
   * Load config themes for enhanced config layout.
   *
   * @method loadConfigThemes
   * @return {$.Deferred}
   */
  loadConfigThemes: function () {
    const dfd = $.Deferred();
    
    if (!this.get('stackConfigsLoaded')) {
      this.loadServiceConfigs().always(() => {
        dfd.resolve();
      });
    } else {
      dfd.resolve();
    }

    return dfd.promise();
  }
});
