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
var blueprintUtils = require('utils/blueprint');
var numberUtils = require('utils/number_utils');
var validationUtils = require('utils/validator');

/**
 * Mixin for assign master-to-host step in wizards
 * Implements basic logic of assign masters page
 * Should be used with controller linked with App.AssignMasterComponentsView
 * @type {Ember.Mixin}
 */
App.AssignMasterComponents = Em.Mixin.create({

  /**
   * Array of master component names to show on the page
   * By default is empty, this means that masters of all selected services should be shown
   * @type {Array}
   */
  mastersToShow: [],

  /**
   * Array of master component names to add for install
   * @type {Array}
   */
  mastersToAdd: [],

  /**
   * Array of master component names, that are already installed, but should have ability to change host
   * @type {Array}
   */
  mastersToMove: [],

  /**
   * Array of master component names, that should be addable
   * Are used in HA wizards to add components, that are not addable for other wizards
   * @type {Array}
   */
  mastersAddableInHA: [],

  /**
   * Array of master component names to show 'Current' prefix in label before component name
   * Prefix will be shown only for installed instances
   * @type {Array}
   */
  showCurrentPrefix: [],

  /**
   * Array of master component names to show 'Additional' prefix in label before component name
   * Prefix will be shown only for not installed instances
   * @type {Array}
   */
  showAdditionalPrefix: [],

  /**
   * Array of objects with label and host keys to show specific hosts on the page
   * @type {Array}
   * format:
   * [
   *   {
   *     label: 'Current',
   *     host: 'c6401.ambari.apache.org'
   *   },
   *   {
   *     label: 'Additional',
   *     host: function () {
   *       return 'c6402.ambari.apache.org';
   *     }.property()
   *   }
   * ]
   */
  additionalHostsList: [],

  /**
   * Define whether show already installed masters first
   * @type {Boolean}
   */
  showInstalledMastersFirst: false,

  /**
   * Map of component name to list of hostnames for that component
   * format:
   * {
   *   NAMENODE: [
   *     'c6401.ambari.apache.org'
   *   ],
   *   DATANODE: [
   *     'c6402.ambari.apache.org',
   *     'c6403.ambari.apache.org',
   *   ]
   * }
   * @type {Object}
   */
  recommendedHostsForComponents: {},

  /**
   * Array of <code>servicesMasters</code> objects, that will be shown on the page
   * Are filtered using <code>mastersToShow</code>
   * @type {Array}
   */
  servicesMastersToShow: function () {
    var mastersToShow = this.get('mastersToShow');
    var servicesMasters = this.get('servicesMasters');
    var result = [];
    if (!mastersToShow.length) {
      result = servicesMasters;
    } else {
      mastersToShow.forEach(function (master) {
        result = result.concat(servicesMasters.filterProperty('component_name', master));
      });
    }

    if (this.get('showInstalledMastersFirst')) {
      result = this.sortMasterComponents(result);
    }

    return result;
  }.property('servicesMasters.length', 'mastersToShow.length', 'showInstalledMastersFirst'),

  /**
   * Sort masters, installed masters will be first.
   * @param masters
   * @returns {Array}
   */
  sortMasterComponents: function (masters) {
    return [].concat(masters.filterProperty('isInstalled'), masters.filterProperty('isInstalled', false));
  },

  /**
   * Check if <code>installerWizard</code> used
   * @type {bool}
   */
  isInstallerWizard: function () {
    return this.get('content.controllerName') === 'installerController';
  }.property('content.controllerName'),

  /**
   * Master components which could be assigned to multiple hosts
   * @type {string[]}
   */
  multipleComponents: function () {
    return App.get('components.multipleMasters');
  }.property('App.components.multipleMasters'),

  /**
   * Master components which could be assigned to multiple hosts
   * @type {string[]}
   */
  addableComponents: function () {
    return App.get('components.addableMasterInstallerWizard').concat(this.get('mastersAddableInHA')).uniq();
  }.property('App.components.addableMasterInstallerWizard', 'mastersAddableInHA'),

  /**
   * Define state for submit button
   * @type {bool}
   */
  submitDisabled: false,

  /**
   * Is Submit-click processing now
   * @type {bool}
   */
  submitButtonClicked: false,

  /**
   * Either use or not use server validation in this controller
   * @type {bool}
   */
  useServerValidation: true,

  /**
   * Trigger for executing host names check for components
   * Should de "triggered" when host changed for some component and when new multiple component is added/removed
   * @type {bool}
   */
  hostNameCheckTrigger: false,

  /**
   * List of hosts
   * @type {Array}
   */
  hosts: [],

  /**
   * Name of multiple component which host name was changed last
   * @type {Object|null}
   */
  componentToRebalance: null,

  /**
   * Name of component which host was changed last
   * @type {string}
   */
  lastChangedComponent: null,

  /**
   * Flag for rebalance multiple components
   * @type {number}
   */
  rebalanceComponentHostsCounter: 0,

  /**
   * @type {Ember.Enumerable}
   */
  servicesMasters: [],

  /**
   * @type {Ember.Enumerable}
   */
  selectedServicesMasters: [],

  /**
   * Is data for current step loaded
   * @type {bool}
   */
  isLoaded: false,

  /**
   * Validation error messages which don't related with any master
   */
  generalErrorMessages: [],

  /**
   * Validation warning messages which don't related with any master
   */
  generalWarningMessages: [],

  /**
   * Is masters-hosts layout initial one
   * @type {bool}
   */
  isInitialLayout: true,

  /**
   * true if any error exists
   */
  anyError: function() {
    return this.get('servicesMasters').some(function(m) { return m.get('errorMessage'); }) || this.get('generalErrorMessages').some(function(m) { return m; });
  }.property('servicesMasters.@each.errorMessage', 'generalErrorMessages'),

  /**
   * true if any warning exists
   */
  anyWarning: function() {
    return this.get('servicesMasters').some(function(m) { return m.get('warnMessage'); }) || this.get('generalWarningMessages').some(function(m) { return m; });
  }.property('servicesMasters.@each.warnMessage', 'generalWarningMessages'),

  /**
   * Clear loaded recommendations
   */
  clearRecommendations: function() {
    if (this.get('content.recommendations')) {
      this.set('content.recommendations', null);
    }
  },

  /**
   * List of host with assigned masters
   * Format:
   * <code>
   *   [
   *     {
   *       host_name: '',
   *       hostInfo: {},
   *       masterServices: [],
   *       masterServicesToDisplay: [] // used only in template
   *    },
   *    ....
   *   ]
   * </code>
   * @type {Ember.Enumerable}
   */
  masterHostMapping: function () {
    var mapping = [], mappingObject, mappedHosts, hostObj;
    //get the unique assigned hosts and find the master services assigned to them
    mappedHosts = this.get("selectedServicesMasters").mapProperty("selectedHost").uniq();
    mappedHosts.forEach(function (item) {
      hostObj = this.get("hosts").findProperty("host_name", item);
      // User may input invalid host name (this is handled in hostname checker). Here we just skip it
      if (!hostObj) return;
      var masterServices = this.get("selectedServicesMasters").filterProperty("selectedHost", item),
          masterServicesToDisplay = [];
      masterServices.mapProperty('display_name').uniq().forEach(function (n) {
        masterServicesToDisplay.pushObject(masterServices.findProperty('display_name', n));
      });
      mappingObject = Em.Object.create({
        host_name: item,
        hostInfo: hostObj.host_info,
        masterServices: masterServices,
        masterServicesToDisplay: masterServicesToDisplay
      });

      mapping.pushObject(mappingObject);
    }, this);

    return mapping.sortProperty('host_name');
  }.property("selectedServicesMasters.@each.selectedHost", 'selectedServicesMasters.@each.isHostNameValid'),

  /**
   * Count of hosts without masters
   * @type {number}
   */
  remainingHosts: function () {
    if (this.get('content.controllerName') === 'installerController') {
      return 0;
    } else {
      return (this.get("hosts.length") - this.get("masterHostMapping.length"));
    }
  }.property('masterHostMapping.length', 'selectedServicesMasters.@each.selectedHost'),

  /**
   * Update submit button status
   * @metohd updateIsSubmitDisabled
   */
  updateIsSubmitDisabled: function () {

    if (this.thereIsNoMasters()) {
      return false;
    }

    var isSubmitDisabled = this.get('servicesMasters').someProperty('isHostNameValid', false);

    if (this.get('useServerValidation')) {
      this.set('submitDisabled', true);

      if (this.get('servicesMasters').length === 0) {
        return;
      }

      if (!isSubmitDisabled) {
        if (!this.get('isInitialLayout')) {
          this.clearRecommendations(); // reset previous recommendations
        } else {
          this.set('isInitialLayout', false);
        }
        this.recommendAndValidate();
      }
    } else {
      isSubmitDisabled = isSubmitDisabled || !this.customClientSideValidation();
      this.set('submitDisabled', isSubmitDisabled);
      return isSubmitDisabled;
    }
  }.observes('servicesMasters.@each.selectedHost'),

  /**
   * Function to validate master-to-host assignments
   * Should be defined in controller
   * @returns {boolean}
   */
  customClientSideValidation: function () {
    return true;
  },

  /**
   * Send AJAX request to validate current host layout
   * @param blueprint - blueprint for validation (can be with/withour slave/client components)
   */
  validate: function(blueprint, callback) {
    var self = this;

    var selectedServices = App.StackService.find().filterProperty('isSelected').mapProperty('serviceName');
    var installedServices = App.StackService.find().filterProperty('isInstalled').mapProperty('serviceName');
    var services = installedServices.concat(selectedServices).uniq();

    var hostNames = self.get('hosts').mapProperty('host_name');

    App.ajax.send({
      name: 'config.validations',
      sender: self,
      data: {
        stackVersionUrl: App.get('stackVersionURL'),
        hosts: hostNames,
        services: services,
        validate: 'host_groups',
        recommendations: blueprint
      },
      success: 'updateValidationsSuccessCallback',
      error: 'updateValidationsErrorCallback'
    }).then(function() {
          if (callback) {
            callback();
          }
        }
    );
  },

  /**
   * Success-callback for validations request
   * @param {object} data
   * @method updateValidationsSuccessCallback
   */
  updateValidationsSuccessCallback: function (data) {
    var self = this;

    var generalErrorMessages = [];
    var generalWarningMessages = [];
    this.get('servicesMasters').setEach('warnMessage', null);
    this.get('servicesMasters').setEach('errorMessage', null);
    var anyErrors = false;

    var validationData = validationUtils.filterNotInstalledComponents(data);
    validationData.filterProperty('type', 'host-component').forEach(function(item) {
      var master = self.get('servicesMasters').find(function(m) {
        return m.component_name === item['component-name'] && m.selectedHost === item.host;
      });
      if (master) {
        if (item.level === 'ERROR') {
          anyErrors = true;
          master.set('errorMessage', item.message);
        } else if (item.level === 'WARN') {
          master.set('warnMessage', item.message);
        }
      }
    });

    this.set('generalErrorMessages', generalErrorMessages);
    this.set('generalWarningMessages', generalWarningMessages);

    // use this.set('submitDisabled', anyErrors); is validation results should block next button
    // It's because showValidationIssuesAcceptBox allow use accept validation issues and continue
    this.set('submitDisabled', false); //this.set('submitDisabled', anyErrors);
  },

  /**
   * Error-callback for validations request
   * @param {object} jqXHR
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @method updateValidationsErrorCallback
   */
  updateValidationsErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    console.error('Config validation failed: ', jqXHR, ajaxOptions, error, opt);
  },

  /**
   * Composes selected values of comboboxes into master blueprint + merge it with currenlty installed slave blueprint
   */
  getCurrentBlueprint: function() {
    var self = this;

    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    var mapping = self.get('masterHostMapping');

    mapping.forEach(function(item, i) {
      var group_name = 'host-group-' + (i+1);

      var host_group = {
        name: group_name,
        components: item.masterServices.map(function(master) {
          return { name: master.component_name };
        })
      };

      var binding = {
        name: group_name,
        hosts: [ { fqdn: item.host_name } ]
      };

      res.blueprint.host_groups.push(host_group);
      res.blueprint_cluster_binding.host_groups.push(binding);
    });

    return blueprintUtils.mergeBlueprints(res, self.getCurrentSlaveBlueprint());
  },

  /**
   * Clear controller data (hosts, masters etc)
   * @method clearStep
   */
  clearStep: function () {
    this.setProperties({
      hosts: [],
      selectedServicesMasters: [],
      servicesMasters: []
    });
    App.StackServiceComponent.find().forEach(function (stackComponent) {
      stackComponent.set('serviceComponentId', 1);
    }, this);

  },

  /**
   * Load controller data (hosts, host components etc)
   * @method loadStep
   */
  loadStep: function () {
    console.log("WizardStep5Controller: Loading step5: Assign Masters");
    this.clearStep();
    this.renderHostInfo();
    this.loadComponentsRecommendationsFromServer(this.loadStepCallback);
  },

  /**
   * Callback after load controller data (hosts, host components etc)
   * @method loadStepCallback
   */
  loadStepCallback: function(components, self) {
    self.renderComponents(components);

    self.get('addableComponents').forEach(function (componentName) {
      self.updateComponent(componentName);
    }, self);
    if (self.thereIsNoMasters()) {
      console.log('no master components to add');
      App.router.send('next');
    }
  },

  /**
   * Returns true if there is no new master components which need assigment to host
   */
  thereIsNoMasters: function() {
    return !this.get("selectedServicesMasters").filterProperty('isInstalled', false).length;
  },

  /**
   * Used to set showAddControl flag for installer wizard
   * @method updateComponent
   */
  updateComponent: function (componentName) {
    var component = this.last(componentName);
    if (!component) {
      return;
    }

    var showControl = !App.StackServiceComponent.find().findProperty('componentName', componentName).get('stackService').get('isInstalled')
        || this.get('mastersAddableInHA').contains(componentName);

    if (showControl) {
      var mastersLength = this.get("selectedServicesMasters").filterProperty("component_name", componentName).length;
      if (mastersLength < this.getMaxNumberOfMasters(componentName)) {
        component.set('showAddControl', true);
      } else if (mastersLength == 1) {
        component.set('showRemoveControl', false);
      }
    }
  },

  /**
   * Count max number of instances for masters <code>componentName</code>, according to their cardinality and number of hosts
   * @param componentName
   * @returns {Number}
   */
  getMaxNumberOfMasters: function (componentName) {
    var maxByCardinality = App.StackServiceComponent.find().findProperty('componentName', componentName).get('maxToInstall');
    var hostsNumber = this.get("hosts.length");
    return Math.min(maxByCardinality, hostsNumber);
  },

  /**
   * Load active host list to <code>hosts</code> variable
   * @method renderHostInfo
   */
  renderHostInfo: function () {
    var hostInfo = this.get('content.hosts');
    var result = [];

    for (var index in hostInfo) {
      var _host = hostInfo[index];
      if (_host.bootStatus === 'REGISTERED') {
        result.push(Em.Object.create({
          host_name: _host.name,
          cpu: _host.cpu,
          memory: _host.memory,
          disk_info: _host.disk_info,
          host_info: Em.I18n.t('installer.step5.hostInfo').fmt(_host.name, numberUtils.bytesToSize(_host.memory, 1, 'parseFloat', 1024), _host.cpu)
        }));
      }
    }
    this.set("hosts", result);
    this.sortHosts(this.get('hosts'));
    this.set('isLoaded', true);
  },

  /**
   * Sort list of host-objects by properties (memory - desc, cpu - desc, hostname - asc)
   * @param {object[]} hosts
   */
  sortHosts: function (hosts) {
    hosts.sort(function (a, b) {
      if (a.get('memory') == b.get('memory')) {
        if (a.get('cpu') == b.get('cpu')) {
          return a.get('host_name').localeCompare(b.get('host_name')); // hostname asc
        }
        return b.get('cpu') - a.get('cpu'); // cores desc
      }
      return b.get('memory') - a.get('memory'); // ram desc
    });
  },

  /**
   * Get recommendations info from API
   * @param {function}callback
   * @param {boolean} includeMasters
   * @method loadComponentsRecommendationsFromServer
   */
  loadComponentsRecommendationsFromServer: function(callback, includeMasters) {
    var self = this;

    if (this.get('content.recommendations')) {
      // Don't do AJAX call if recommendations has been already received
      // But if user returns to previous step (selecting services), stored recommendations will be cleared in routers' next handler and AJAX call will be made again
      callback(self.createComponentInstallationObjects(), self);
    }
    else {
      var selectedServices = App.StackService.find().filterProperty('isSelected').mapProperty('serviceName');
      var installedServices = App.StackService.find().filterProperty('isInstalled').mapProperty('serviceName');
      var services = installedServices.concat(selectedServices).uniq();

      var hostNames = self.get('hosts').mapProperty('host_name');

      var data = {
        stackVersionUrl: App.get('stackVersionURL'),
        hosts: hostNames,
        services: services,
        recommend: 'host_groups'
      };

      if (includeMasters) {
        // Made partial recommendation request for reflect in blueprint host-layout changes which were made by user in UI
        data.recommendations = self.getCurrentBlueprint();
      }
      else
        if (!self.get('isInstallerWizard')) {
          data.recommendations = self.getCurrentMasterSlaveBlueprint();
        }

      return App.ajax.send({
        name: 'wizard.loadrecommendations',
        sender: self,
        data: data,
        success: 'loadRecommendationsSuccessCallback',
        error: 'loadRecommendationsErrorCallback'
      }).then(function () {
          callback(self.createComponentInstallationObjects(), self);
        });
    }
  },

  /**
   * Create components for displaying component-host comboboxes in UI assign dialog
   * expects content.recommendations will be filled with recommendations API call result
   * @return {Object[]}
   */
  createComponentInstallationObjects: function() {
    var stackMasterComponentsMap = {},
        masterHosts = this.get('content.masterComponentHosts'), //saved to local storage info
        servicesToAdd = this.get('content.services').filterProperty('isSelected').filterProperty('isInstalled', false).mapProperty('serviceName'),
        recommendations = this.get('content.recommendations'),
        resultComponents = [],
        multipleComponentHasBeenAdded = {},
        hostGroupsMap = {};

    App.StackServiceComponent.find().forEach(function(component) {
      if (this.get('isInstallerWizard')) {
        if (component.get('isShownOnInstallerAssignMasterPage')) {
          stackMasterComponentsMap[component.get('componentName')] = component;
        }
      } else {
        if (component.get('isShownOnAddServiceAssignMasterPage') || this.get('mastersToShow').contains(component.get('componentName'))) {
          stackMasterComponentsMap[component.get('componentName')] = component;
        }
      }
    }, this);

    recommendations.blueprint_cluster_binding.host_groups.forEach(function(group) {
      hostGroupsMap[group.name] = group;
    });

    recommendations.blueprint.host_groups.forEach(function(host_group) {
      var hosts = hostGroupsMap[host_group.name] ? hostGroupsMap[host_group.name].hosts : [];

      hosts.forEach(function(host) {
        host_group.components.forEach(function(component) {
          var willBeDisplayed = true;
          var stackMasterComponent = stackMasterComponentsMap[component.name];
          if (stackMasterComponent) {
            // If service is already installed and not being added as a new service then render on UI only those master components
            // that have already installed hostComponents.
            // NOTE: On upgrade there might be a prior installed service with non-installed newly introduced serviceComponent
            if (!servicesToAdd.contains(stackMasterComponent.get('serviceName'))) {
              willBeDisplayed = masterHosts.someProperty('component', component.name);
            }

            if (willBeDisplayed) {
              var savedComponents = masterHosts.filterProperty('component', component.name);

              if (this.get('multipleComponents').contains(component.name) && savedComponents.length > 0) {
                if (!multipleComponentHasBeenAdded[component.name]) {
                  multipleComponentHasBeenAdded[component.name] = true;

                  savedComponents.forEach(function(saved) {
                    resultComponents.push(this.createComponentInstallationObject(stackMasterComponent, host.fqdn.toLowerCase(), saved));
                  }, this);
                }
              }
              else {
                var savedComponent = masterHosts.findProperty('component', component.name);
                resultComponents.push(this.createComponentInstallationObject(stackMasterComponent, host.fqdn.toLowerCase(), savedComponent));
              }
            }
          }
        }, this);
      }, this);
    }, this);
    return resultComponents;
  },

  /**
   * Create component for displaying component-host comboboxes in UI assign dialog
   * @param fullComponent - full component description
   * @param hostName - host fqdn where component will be installed
   * @param savedComponent - the same object which function returns but created before
   * @return {Object}
   */
  createComponentInstallationObject: function(fullComponent, hostName, savedComponent) {
    var componentName = fullComponent.get('componentName');

    var componentObj = {};
    componentObj.component_name = componentName;
    componentObj.display_name = App.format.role(fullComponent.get('componentName'), false);
    componentObj.serviceId = fullComponent.get('serviceName');
    componentObj.isServiceCoHost = App.StackServiceComponent.find().findProperty('componentName', componentName).get('isCoHostedComponent') && !this.get('mastersToMove').contains(componentName);
    componentObj.selectedHost = savedComponent ? savedComponent.hostName : hostName;
    componentObj.isInstalled = savedComponent ? savedComponent.isInstalled : false;
    return componentObj;
  },

  /**
   * Success-callback for recommendations request
   * @param {object} data
   * @method loadRecommendationsSuccessCallback
   */
  loadRecommendationsSuccessCallback: function (data) {
    var recommendations = data.resources[0].recommendations;
    this.set('content.recommendations', recommendations);

    var recommendedHostsForComponent = {};
    var hostsForHostGroup = {};

    recommendations.blueprint_cluster_binding.host_groups.forEach(function(hostGroup) {
      hostsForHostGroup[hostGroup.name] = hostGroup.hosts.mapProperty('fqdn');
    });

    recommendations.blueprint.host_groups.forEach(function (hostGroup) {
      var components = hostGroup.components.mapProperty('name');
      components.forEach(function (componentName) {
        var hostList = recommendedHostsForComponent[componentName] || [];
        var hostNames = hostsForHostGroup[hostGroup.name] || [];
        hostList.pushObjects(hostNames);
        recommendedHostsForComponent[componentName] = hostList;
      });
    });

    this.set('content.recommendedHostsForComponents', recommendedHostsForComponent);
  },

  /**
   * Error-callback for recommendations request
   * @param {object} jqXHR
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @method loadRecommendationsErrorCallback
   */
  loadRecommendationsErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.method, jqXHR.status);
    console.log('Load recommendations failed');
  },

  /**
   * Put master components to <code>selectedServicesMasters</code>, which will be automatically rendered in template
   * @param {Ember.Enumerable} masterComponents
   * @method renderComponents
   */
  renderComponents: function (masterComponents) {
    var installedServices = App.StackService.find().filterProperty('isSelected').filterProperty('isInstalled', false).mapProperty('serviceName'); //list of shown services
    var result = [];
    var serviceComponentId, previousComponentName;

    this.addNewMasters(masterComponents);

    masterComponents.forEach(function (item) {
      var masterComponent = App.StackServiceComponent.find().findProperty('componentName', item.component_name);
      var componentObj = Em.Object.create(item);
      var showRemoveControl;
      console.log("TRACE: render master component name is: " + item.component_name);
      if (masterComponent.get('isMasterWithMultipleInstances')) {
        showRemoveControl = installedServices.contains(masterComponent.get('stackService.serviceName')) &&
            (masterComponents.filterProperty('component_name', item.component_name).length > 1);
        previousComponentName = item.component_name;
        componentObj.set('serviceComponentId', result.filterProperty('component_name', item.component_name).length + 1);
        componentObj.set("showRemoveControl", showRemoveControl);
      }
      componentObj.set('isHostNameValid', true);
      componentObj.set('showCurrentPrefix', this.get('showCurrentPrefix').contains(item.component_name) && item.isInstalled);
      componentObj.set('showAdditionalPrefix', this.get('showAdditionalPrefix').contains(item.component_name) && !item.isInstalled);
      if (this.get('mastersToMove').contains(item.component_name)) {
        componentObj.set('isInstalled', false);
      }

      result.push(componentObj);
    }, this);
    result = this.sortComponentsByServiceName(result);
    this.set("selectedServicesMasters", result);
    this.set('servicesMasters', result);
  },

  /**
   * Add new master components from <code>mastersToAdd</code> list
   * @param masterComponents
   * @returns {masterComponents[]}
   */
  addNewMasters: function (masterComponents) {
    this.get('mastersToAdd').forEach(function(masterName){
      var hostName = this.getHostForMaster(masterName, masterComponents);
      var serviceName = this.getServiceByMaster(masterName);
      masterComponents.push(this.createComponentInstallationObject(
          Em.Object.create({
            componentName: masterName,
            serviceName: serviceName
          }),
          hostName
      ));
    }, this);
    return masterComponents;
  },

  /**
   * Find available host for master and return it
   * If there is no available hosts returns false
   * @param master
   * @param allMasters
   * @returns {*}
   */
  getHostForMaster: function (master, allMasters) {
    var masterHostList = [];

    allMasters.forEach(function (component) {
      if (component.component_name === master) {
        masterHostList.push(component.selectedHost);
      }
    });

    var recommendedHostsForMaster = this.get('content.recommendedHostsForComponents')[master] || [];
    for (var k = 0; k < recommendedHostsForMaster.length; k++) {
      if(!masterHostList.contains(recommendedHostsForMaster[k])) {
        return recommendedHostsForMaster[k];
      }
    }

    var usedHosts = allMasters.filterProperty('component_name', master).mapProperty('selectedHost');
    var allHosts = this.get('hosts');
    for (var i = 0; i < allHosts.length; i++) {
      if (!usedHosts.contains(allHosts[i].get('host_name'))) {
        return allHosts[i].get('host_name');
      }
    }

    return false;
  },

  /**
   * Find serviceName for master by it's componentName
   * @param master
   * @returns {*}
   */
  getServiceByMaster: function (master) {
    return App.StackServiceComponent.find().findProperty('componentName', master).get('serviceName');
  },

  /**
   * Sort components by their service (using <code>App.StackService.displayOrder</code>)
   * Services not in App.StackService.displayOrder are moved to the end of the list
   *
   * @param components
   * @returns {*}
   */
  sortComponentsByServiceName: function(components) {
    var displayOrder = App.StackService.displayOrder;
    var componentsOrderForService = App.StackService.componentsOrderForService;
    var indexForUnordered = Math.max(displayOrder.length, components.length);
    return components.sort(function (a, b) {
      if(a.serviceId === b.serviceId && a.serviceId in componentsOrderForService)
        return componentsOrderForService[a.serviceId].indexOf(a.component_name) - componentsOrderForService[b.serviceId].indexOf(b.component_name);
      var aValue = displayOrder.indexOf(a.serviceId) != -1 ? displayOrder.indexOf(a.serviceId) : indexForUnordered;
      var bValue = displayOrder.indexOf(b.serviceId) != -1 ? displayOrder.indexOf(b.serviceId) : indexForUnordered;
      return aValue - bValue;
    });
  },
  /**
   * Update dependent co-hosted components according to the change in the component host
   * @method updateCoHosts
   */
  updateCoHosts: function () {
    var components = App.StackServiceComponent.find().filterProperty('isOtherComponentCoHosted');
    var selectedServicesMasters = this.get('selectedServicesMasters');
    components.forEach(function (component) {
      var componentName = component.get('componentName');
      var hostComponent = selectedServicesMasters.findProperty('component_name', componentName);
      var dependentCoHosts = component.get('coHostedComponents');
      dependentCoHosts.forEach(function (coHostedComponent) {
        var dependentHostComponent = selectedServicesMasters.findProperty('component_name', coHostedComponent);
        if (!this.get('mastersToMove').contains(coHostedComponent) && hostComponent && dependentHostComponent) dependentHostComponent.set('selectedHost', hostComponent.get('selectedHost'));
      }, this);
    }, this);
  }.observes('selectedServicesMasters.@each.selectedHost'),


  /**
   * On change callback for inputs
   * @param {string} componentName
   * @param {string} selectedHost
   * @param {number} serviceComponentId
   * @method assignHostToMaster
   */
  assignHostToMaster: function (componentName, selectedHost, serviceComponentId) {
    var flag = this.isHostNameValid(componentName, selectedHost);
    var component;
    this.updateIsHostNameValidFlag(componentName, serviceComponentId, flag);
    if (serviceComponentId) {
      component = this.get('selectedServicesMasters').filterProperty('component_name', componentName).findProperty("serviceComponentId", serviceComponentId);
      if (component) component.set("selectedHost", selectedHost);
    }
    else {
      this.get('selectedServicesMasters').findProperty("component_name", componentName).set("selectedHost", selectedHost);
    }
  },

  /**
   * Determines if hostName is valid for component:
   * <ul>
   *  <li>host name shouldn't be empty</li>
   *  <li>host should exist</li>
   *  <li>host should have only one component with <code>componentName</code></li>
   * </ul>
   * @param {string} componentName
   * @param {string} selectedHost
   * @returns {boolean} true - valid, false - invalid
   * @method isHostNameValid
   */
  isHostNameValid: function (componentName, selectedHost) {
    return (selectedHost.trim() !== '') &&
    this.get('hosts').mapProperty('host_name').contains(selectedHost) &&
    (this.get('selectedServicesMasters').
        filterProperty('component_name', componentName).
        mapProperty('selectedHost').
        filter(function (h) {
          return h === selectedHost;
        }).length <= 1);
  },

  /**
   * Update <code>isHostNameValid</code> property with <code>flag</code> value
   * for component with name <code>componentName</code> and
   * <code>serviceComponentId</code>-property equal to <code>serviceComponentId</code>-parameter value
   * @param {string} componentName
   * @param {number} serviceComponentId
   * @param {bool} flag
   * @method updateIsHostNameValidFlag
   */
  updateIsHostNameValidFlag: function (componentName, serviceComponentId, flag) {
    var component;
    if (componentName) {
      if (serviceComponentId) {
        component = this.get('selectedServicesMasters').filterProperty('component_name', componentName).findProperty("serviceComponentId", serviceComponentId);
        if (component) component.set("isHostNameValid", flag);
      } else {
        this.get('selectedServicesMasters').findProperty("component_name", componentName).set("isHostNameValid", flag);
      }
    }
  },

  /**
   * Returns last component of selected type
   * @param {string} componentName
   * @return {Em.Object|null}
   * @method last
   */
  last: function (componentName) {
    return this.get("selectedServicesMasters").filterProperty("component_name", componentName).get("lastObject");
  },

  /**
   * Add new component to ZooKeeper Server and Hbase master
   * @param {string} componentName
   * @return {bool} true - added, false - not added
   * @method addComponent
   */
  addComponent: function (componentName) {
    /*
     * Logic: If ZooKeeper or Hbase service is selected then there can be
     * minimum 1 ZooKeeper or Hbase master in total, and
     * maximum 1 ZooKeeper or Hbase on every host
     */

    var maxNumMasters = this.getMaxNumberOfMasters(componentName),
        currentMasters = this.get("selectedServicesMasters").filterProperty("component_name", componentName),
        newMaster = null,
        masterHosts = null,
        suggestedHost = null,
        i = 0,
        lastMaster = null;

    if (!currentMasters.length) {
      console.log('ALERT: Zookeeper service was not selected');
      return false;
    }

    if (currentMasters.get("length") < maxNumMasters) {

      currentMasters.set("lastObject.showAddControl", false);
      currentMasters.set("lastObject.showRemoveControl", true);

      //create a new master component host based on an existing one
      newMaster = Em.Object.create({});
      lastMaster = currentMasters.get("lastObject");
      newMaster.set("display_name", lastMaster.get("display_name"));
      newMaster.set("component_name", lastMaster.get("component_name"));
      newMaster.set("selectedHost", lastMaster.get("selectedHost"));
      newMaster.set("serviceId", lastMaster.get("serviceId"));
      newMaster.set("isInstalled", false);
      newMaster.set('showAdditionalPrefix', this.get('showAdditionalPrefix').contains(lastMaster.get("component_name")));

      if (currentMasters.get("length") === (maxNumMasters - 1)) {
        newMaster.set("showAddControl", false);
      } else {
        newMaster.set("showAddControl", true);
      }
      newMaster.set("showRemoveControl", true);

      //get recommended host for the new Zookeeper server
      masterHosts = currentMasters.mapProperty("selectedHost").uniq();

      for (i = 0; i < this.get("hosts.length"); i++) {
        if (!(masterHosts.contains(this.get("hosts")[i].get("host_name")))) {
          suggestedHost = this.get("hosts")[i].get("host_name");
          break;
        }
      }

      newMaster.set("selectedHost", suggestedHost);
      newMaster.set("serviceComponentId", (currentMasters.get("lastObject.serviceComponentId") + 1));

      this.get("selectedServicesMasters").insertAt(this.get("selectedServicesMasters").indexOf(lastMaster) + 1, newMaster);

      this.set('componentToRebalance', componentName);
      this.incrementProperty('rebalanceComponentHostsCounter');
      this.toggleProperty('hostNameCheckTrigger');
      return true;
    }
    return false;//if no more zookeepers can be added
  },

  /**
   * Remove component from ZooKeeper server or Hbase Master
   * @param {string} componentName
   * @param {number} serviceComponentId
   * @return {bool} true - removed, false - no
   * @method removeComponent
   */
  removeComponent: function (componentName, serviceComponentId) {
    var currentMasters = this.get("selectedServicesMasters").filterProperty("component_name", componentName);

    //work only if the multiple master service is selected in previous step
    if (currentMasters.length <= 1) {
      return false;
    }

    this.get("selectedServicesMasters").removeAt(this.get("selectedServicesMasters").indexOf(currentMasters.findProperty("serviceComponentId", serviceComponentId)));

    currentMasters = this.get("selectedServicesMasters").filterProperty("component_name", componentName);
    if (currentMasters.get("length") < this.getMaxNumberOfMasters(componentName)) {
      currentMasters.set("lastObject.showAddControl", true);
    }

    if (currentMasters.filterProperty('isInstalled', false).get("length") === 1) {
      currentMasters.set("lastObject.showRemoveControl", false);
    }

    this.set('componentToRebalance', componentName);
    this.incrementProperty('rebalanceComponentHostsCounter');
    this.toggleProperty('hostNameCheckTrigger');
    return true;
  },

  recommendAndValidate: function(callback) {
    var self = this;

    // load recommendations with partial request
    self.loadComponentsRecommendationsFromServer(function() {
      // For validation use latest received recommendations because it contains current master layout and recommended slave/client layout
      self.validate(self.get('content.recommendations'), function() {
        if (callback) {
          callback();
        }
      });
    }, true);
  },

  /**
   * Submit button click handler
   * @method submit
   */
  submit: function () {
    var self = this;
    if (!this.get('submitButtonClicked')) {
      this.set('submitButtonClicked', true);

      var goNextStepIfValid = function () {
        if (!self.get('submitDisabled')) {
          App.router.send('next');
        }
      };

      if (this.get('useServerValidation')) {
        self.recommendAndValidate(function () {
          self.showValidationIssuesAcceptBox(goNextStepIfValid);
        });
      } else {
        self.updateIsSubmitDisabled();
        goNextStepIfValid();
        self.set('submitButtonClicked', false);
      }
    }
  },

  /**
   * In case of any validation issues shows accept dialog box for user which allow cancel and fix issues or continue anyway
   * @method showValidationIssuesAcceptBox
   */
  showValidationIssuesAcceptBox: function(callback) {
    var self = this;

    // If there are no warnings and no errors, return
    if (!self.get('anyWarning') && !self.get('anyError')) {
      callback();
      self.set('submitButtonClicked', false);
      return;
    }

    App.ModalPopup.show({
      primary: Em.I18n.t('common.continueAnyway'),
      header: Em.I18n.t('installer.step5.validationIssuesAttention.header'),
      body: Em.I18n.t('installer.step5.validationIssuesAttention'),
      onPrimary: function () {
        this._super();
        callback();
        self.set('submitButtonClicked', false);
      },
      onSecondary: function () {
        this._super();
        self.set('submitButtonClicked', false);
      },
      onClose: function () {
        this._super();
        self.set('submitButtonClicked', false);
      }
    });
  }
});
