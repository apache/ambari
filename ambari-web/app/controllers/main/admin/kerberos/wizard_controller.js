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

App.KerberosWizardController = App.WizardController.extend({

  exceptionsOnSkipClient: [{'KDC': 'realm'}, {'KDC': 'kdc_type'}, {'Advanced kerberos-env': 'executable_search_paths'}],

  name: 'kerberosWizardController',

  totalSteps: 8,

  isKerberosWizard: true,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  skipClientInstall: false,

  kerberosDescriptorConfigs: null,

  content: Em.Object.create({
    controllerName: 'kerberosWizardController',
    serviceName: 'KERBEROS',
    kerberosOption: null,
    cluster: null,
    services: [],
    advancedServiceConfig: null,
    serviceConfigProperties: [],
    failedTask: null
  }),

  /**
   * set current step
   * @param {string} currentStep
   * @param {boolean} completed
   * @param {boolean} skipStateSave
   */
  setCurrentStep: function (currentStep, completed, skipStateSave) {
    this._super(currentStep, completed);
    if (App.get('testMode') || skipStateSave) {
      return;
    }
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'KERBEROS_DEPLOY',
      wizardControllerName: 'kerberosWizardController',
      localdb: App.db.data
    });
  },

  setStepsEnable: function () {
    for (var i = 1; i <= this.get('totalSteps'); i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      if (i <= this.get('currentStep') && App.get('router.clusterController.isLoaded')) {
        step.set('value', false);
      } else {
        step.set('value', i != this.get('currentStep'));
      }
    }
  }.observes('currentStep', 'App.router.clusterController.isLoaded'),

  /**
   * return new object extended from clusterStatusTemplate
   * @return Object
   */
  getCluster: function () {
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.get('router').getClusterName()});
  },

  /**
   *  Gets the
   * @returns {*} jquery promise
   */
  getClusterEnvData: function () {
    var dfd = $.Deferred();
    var self = this;
    var siteName = 'cluster-env';
    var tags = [{siteName: siteName}];
    App.get('router.configurationController').getConfigsByTags(tags).done(function (data) {
      var properties = self.updateClusterEnvData(data[0].properties);
      var clusterConfig = {"type": siteName, "tag": 'version' + (new Date).getTime(), "properties": properties};
      var clusterConfigData = {
        Clusters: {
          desired_config: clusterConfig
        }
      };
      dfd.resolve(clusterConfigData);
    });
    return dfd;
  },

  updateClusterEnvData: function (configs) {
    var kerberosDescriptor = this.kerberosDescriptorConfigs;
    configs['security_enabled'] = true;
    configs['kerberos_domain'] = kerberosDescriptor.properties.realm;
    return configs;
  },

  /**
   * save status of the cluster.
   * @param clusterStatus object with status,requestId fields.
   */
  saveClusterStatus: function (clusterStatus) {
    var oldStatus = this.toObject(this.get('content.cluster'));
    clusterStatus = jQuery.extend(oldStatus, clusterStatus);
    if (clusterStatus.requestId) {
      clusterStatus.requestId.forEach(function (requestId) {
        if (clusterStatus.oldRequestsId.indexOf(requestId) === -1) {
          clusterStatus.oldRequestsId.push(requestId)
        }
      }, this);
    }
    this.set('content.cluster', clusterStatus);
    this.save('cluster');
  },

  saveConfigTag: function (tag) {
    App.db.setKerberosWizardConfigTag(tag);
    this.set('content.' + [tag.name], tag.value);
  },

  saveKerberosOption: function (stepController) {
    this.setDBProperty('kerberosOption', stepController.get('selectedItem'));
    this.set('content.kerberosOption', stepController.get('selectedItem'));
  },

  /**
   * Load serviceConfigProperties to model
   */
  loadServiceConfigProperties: function () {
    var serviceConfigProperties = this.getDBProperty('serviceConfigProperties');
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  },

  /**
   * load advanced configs from server
   */
  loadAdvancedConfigs: function (dependentController) {
    var self = this;
    var loadAdvancedConfigResult = [];
    dependentController.set('isAdvancedConfigLoaded', false);
    var serviceName = this.get('content.serviceName');
    App.config.loadAdvancedConfig(serviceName, function (properties) {
      loadAdvancedConfigResult.pushObjects(properties);
      self.set('content.advancedServiceConfig', loadAdvancedConfigResult);
      self.setDBProperty('advancedServiceConfig', loadAdvancedConfigResult);
      dependentController.set('isAdvancedConfigLoaded', true);
    });
  },

  loadKerberosDescriptorConfigs: function () {
    var kerberosDescriptorConfigs = this.getDBProperty('kerberosDescriptorConfigs');
    this.set('kerberosDescriptorConfigs', kerberosDescriptorConfigs);
  },

  /**
   * Override the visibility of a list of form items with a new value
   *
   * @param {Array} itemsArray
   * @param newValue
   */
  overrideVisibility: function (itemsArray, newValue, exceptions) {
    newValue = newValue || false;

    for (var i = 0, len = itemsArray.length; i < len; i += 1) {
      if (!Ember.$.isEmptyObject(itemsArray[i])) {
        var isException = exceptions.filterProperty(itemsArray[i].category, itemsArray[i].name);
        if (!isException.length) {
          itemsArray[i].isVisible = newValue;
        }
      }
    }
  },

  loadKerberosOption: function () {
    this.set('content.kerberosOption', this.getDBProperty('kerberosOption'));
  },

  saveKerberosDescriptorConfigs: function (kerberosDescriptorConfigs) {
    this.setDBProperty('kerberosDescriptorConfigs',kerberosDescriptorConfigs);
    this.set('kerberosDescriptorConfigs', kerberosDescriptorConfigs);
  },


  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.loadKerberosOption();
        }
      }
    ],
    '2': [
      {
        type: 'sync',
        callback: function () {
          var kerberosStep2controller = App.get('router.kerberosWizardStep2Controller');
          this.loadAdvancedConfigs(kerberosStep2controller);
          this.loadServiceConfigProperties();
        }
      }
    ],
    '3': [
      {
        type: 'sync',
        callback: function () {
          this.loadTasksStatuses();
          this.loadTasksRequestIds();
          this.loadRequestIds();
        }
      }
    ],
    '4': [
      {
        type: 'sync',
        callback: function () {
          this.loadKerberosDescriptorConfigs();
        }
      }
    ],
    '6': [
      {
        type: 'sync',
        callback: function () {
          this.loadKerberosDescriptorConfigs();
        }
      }
    ]
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

  clearTasksData: function () {
    this.saveTasksStatuses(undefined);
    this.saveRequestIds(undefined);
    this.saveTasksRequestIds(undefined);
  },
  /**
   * shows popup with to warn user
   * @param primary
   * @param isCritical
   */
  warnBeforeExitPopup: function(primary, isCritical) {
    var primaryText = Em.I18n.t('common.exitAnyway');
    var msg = isCritical ? Em.I18n.t('admin.kerberos.wizard.exit.critical.msg')
      : Em.I18n.t('admin.kerberos.wizard.exit.warning.msg');
    return App.showConfirmationPopup(primary, msg, null, null, primaryText, isCritical);
  },
  /**
   * Clear all temporary data
   */
  finish: function () {
    // The in-memory variable for current step should be reset to 1st step.
    this.setCurrentStep('1', false, true);
    // kerberos wizard namespace in the localStorage should be emptied
    this.resetDbNamespace();
    App.get('router.updateController').updateAll();
  }
});
