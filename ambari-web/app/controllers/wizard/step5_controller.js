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

App.WizardStep5Controller = Em.Controller.extend(App.BlueprintMixin, {

  name: "wizardStep5Controller",

  /**
   * Step title
   * Custom if <code>App.ReassignMasterController</code> is used
   * @type {string}
   */
  title: function () {
    if (this.get('content.controllerName') == 'reassignMasterController') {
      return Em.I18n.t('installer.step5.reassign.header');
    }
    return Em.I18n.t('installer.step5.header');
  }.property('content.controllerName'),

  /**
   * Is ReassignWizard used
   * @type {bool}
   */
  isReassignWizard: function () {
    return this.get('content.controllerName') == 'reassignMasterController';
  }.property('content.controllerName'),

  /**
   * Is isHighAvailabilityWizard used
   * @type {bool}
   */
  isHighAvailabilityWizard: function () {
    return this.get('content.controllerName') == 'highAvailabilityWizardController';
  }.property('content.controllerName'),

  /**
   * Check if <code>installerWizard</code> used
   * @type {bool}
   */
  isInstallerWizard: function () {
    return this.get('content.controllerName') === 'installerController';
  }.property('content.controllerName'),

  /**
   * Is AddServiceWizard used
   * @type {bool}
   */
  isAddServiceWizard: function () {
    return this.get('content.controllerName') == 'addServiceController';
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
    return App.get('components.addableMasterInstallerWizard');
  }.property('App.components.addableMasterInstallerWizard'),

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
    var self = this;

    if (self.thereIsNoMasters()) {
      return false;
    }

    if (App.get('supports.serverRecommendValidate') && this.get('useServerValidation')) {
      self.set('submitDisabled', true);

      // reset previous recommendations
      this.clearRecommendations();

      if (self.get('servicesMasters').length === 0) {
        return;
      }

      var isSubmitDisabled = this.get('servicesMasters').someProperty('isHostNameValid', false);
      if (!isSubmitDisabled) {
        self.recommendAndValidate();
      }
    } else {
      var isSubmitDisabled = this.get('servicesMasters').someProperty('isHostNameValid', false);
      self.set('submitDisabled', isSubmitDisabled);
      return isSubmitDisabled;
    }
  }.observes('servicesMasters.@each.selectedHost', 'servicesMasters.@each.isHostNameValid'),

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
    }).
      then(function() {
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
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.method, jqXHR.status);
    console.log('Load validations failed');
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
    this.set('hosts', []);
    this.set('selectedServicesMasters', []);
    this.set('servicesMasters', []);
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
    if (App.get('supports.serverRecommendValidate')) {
      this.loadComponentsRecommendationsFromServer(this.loadStepCallback);
    } else {
      this.loadComponentsRecommendationsLocally(this.loadStepCallback);
    }
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
    var services = App.StackService.find().filterProperty('isInstalled', true).mapProperty('serviceName');
    var currentService = componentName.split('_')[0];
    var showControl = !services.contains(currentService);

    if (showControl) {
      var mastersLength = this.get("selectedServicesMasters").filterProperty("component_name", componentName).length;
      if (mastersLength < this.get("hosts.length") && !this.get('isReassignWizard') && !this.get('isHighAvailabilityWizard')) {
        component.set('showAddControl', true);
      } else if (mastersLength == 1 || this.get('isReassignWizard') || this.get('isHighAvailabilityWizard')) {
        component.set('showRemoveControl', false);
      }
    }
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
   * @return {undefined}
   * @param function(componentInstallationobjects, this) callback
   * @param bool includeMasters
   */
  loadComponentsRecommendationsFromServer: function(callback, includeMasters) {
    var self = this;

    if (this.get('content.recommendations')) {
      // Don't do AJAX call if recommendations has been already received
      // But if user returns to previous step (selecting services), stored recommendations will be cleared in routers' next handler and AJAX call will be made again
      callback(self.createComponentInstallationObjects(), self);
    } else {
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
      } else if (!self.get('isInstallerWizard')) {
        data.recommendations = self.getCurrentMasterSlaveBlueprint();
      }

      return App.ajax.send({
        name: 'wizard.loadrecommendations',
        sender: self,
        data: data,
        success: 'loadRecommendationsSuccessCallback',
        error: 'loadRecommendationsErrorCallback'
      }).
        then(function () {
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
    var self = this;

    var masterComponents = [];
    if (self.get('isAddServiceWizard')) {
      masterComponents = App.StackServiceComponent.find().filterProperty('isShownOnAddServiceAssignMasterPage');
    } else {
      masterComponents = App.StackServiceComponent.find().filterProperty('isShownOnInstallerAssignMasterPage');
    }

    var masterHosts = self.get('content.masterComponentHosts'); //saved to local storage info
    var selectedNotInstalledServices = self.get('content.services').filterProperty('isSelected').filterProperty('isInstalled', false).mapProperty('serviceName');
    var recommendations = this.get('content.recommendations');

    var resultComponents = [];
    var multipleComponentHasBeenAdded = {};

    recommendations.blueprint.host_groups.forEach(function(host_group) {
      var hosts = recommendations.blueprint_cluster_binding.host_groups.findProperty('name', host_group.name).hosts;

      hosts.forEach(function(host) {
        host_group.components.forEach(function(component) {
          var willBeAdded = true;
          var fullComponent = masterComponents.findProperty('componentName', component.name);
          // If it's master component which should be shown
          if (fullComponent) {
            // If service is already installed and not being added as a new service then render on UI only those master components
            // that have already installed hostComponents.
            // NOTE: On upgrade there might be a prior installed service with non-installed newly introduced serviceComponent
            var isNotSelectedService = !selectedNotInstalledServices.contains(fullComponent.get('serviceName'));
            if (isNotSelectedService) {
              willBeAdded = App.HostComponent.find().someProperty('componentName', component.name);
            }

            if (willBeAdded) {
              var savedComponents = masterHosts.filterProperty('component', component.name);

              if (self.get('multipleComponents').contains(component.name) && savedComponents.length > 0) {
                if (!multipleComponentHasBeenAdded[component.name]) {
                  multipleComponentHasBeenAdded[component.name] = true;

                  savedComponents.forEach(function(saved) {
                    resultComponents.push(self.createComponentInstallationObject(fullComponent, host.fqdn, saved));
                  });
                }
              } else {
                var savedComponent = masterHosts.findProperty('component', component.name);
                resultComponents.push(self.createComponentInstallationObject(fullComponent, host.fqdn, savedComponent));
              }
            }
          }
        });
      });
    });
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
    componentObj.display_name = App.format.role(fullComponent.get('componentName'));
    componentObj.serviceId = fullComponent.get('serviceName');
    componentObj.isServiceCoHost = App.StackServiceComponent.find().findProperty('componentName', componentName).get('isCoHostedComponent') && !this.get('isReassignWizard');

    if (savedComponent) {
      componentObj.selectedHost = savedComponent.hostName;
      componentObj.isInstalled = savedComponent.isInstalled;
    } else {
      componentObj.selectedHost = hostName;
      componentObj.isInstalled = false;
    }

    return componentObj;
  },

  /**
   * Success-callback for recommendations request
   * @param {object} data
   * @method loadRecommendationsSuccessCallback
   */
  loadRecommendationsSuccessCallback: function (data) {
    this.set('content.recommendations', data.resources[0].recommendations);
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
   * Load services info to appropriate variable and return masterComponentHosts
   * @return {Object[]}
   */
  loadComponentsRecommendationsLocally: function (callback) {
    var selectedServices = App.StackService.find().filterProperty('isSelected').mapProperty('serviceName');
    var installedServices = App.StackService.find().filterProperty('isInstalled').mapProperty('serviceName');
    var services = installedServices.concat(selectedServices).uniq();
    var selectedNotInstalledServices = this.get('content.services').filterProperty('isSelected').filterProperty('isInstalled', false).mapProperty('serviceName');

    var masterComponents = [];
    //get full list from mock data
    if (this.get('isAddServiceWizard')) {
      masterComponents = App.StackServiceComponent.find().filterProperty('isShownOnAddServiceAssignMasterPage');
    } else {
      masterComponents = App.StackServiceComponent.find().filterProperty('isShownOnInstallerAssignMasterPage');
    }
    var masterHosts = this.get('content.masterComponentHosts'); //saved to local storage info

    var resultComponents = [];

    for (var index = 0; index < services.length; index++) {
      var componentInfo = masterComponents.filterProperty('serviceName', services[index]);
      // If service is already installed and not being added as a new service then render on UI only those master components
      // that have already installed hostComponents.
      // NOTE: On upgrade there might be a prior installed service with non-installed newly introduced serviceComponent
      var isNotSelectedService = !selectedNotInstalledServices.contains(services[index]);
      if (isNotSelectedService) {
        componentInfo = componentInfo.filter(function (_component) {
          return App.HostComponent.find().someProperty('componentName',_component.get('componentName'));
        });
      }

      componentInfo.forEach(function (_componentInfo) {
        if (this.get('multipleComponents').contains(_componentInfo.get('componentName'))) {
          var savedComponents = masterHosts.filterProperty('component', _componentInfo.get('componentName'));
          if (savedComponents.length) {
            savedComponents.forEach(function (item) {
              var multipleMasterHost = {};
              multipleMasterHost.component_name = _componentInfo.get('componentName');
              multipleMasterHost.display_name = _componentInfo.get('displayName');
              multipleMasterHost.selectedHost = item.hostName;
              multipleMasterHost.serviceId = services[index];
              multipleMasterHost.isInstalled = item.isInstalled;
              multipleMasterHost.isServiceCoHost = false;
              resultComponents.push(multipleMasterHost);
            })
          } else {
            var multipleMasterHosts = this.selectHostLocally(_componentInfo.get('componentName'));
            multipleMasterHosts.forEach(function (_host) {
              var multipleMasterHost = {};
              multipleMasterHost.component_name = _componentInfo.get('componentName');
              multipleMasterHost.display_name = _componentInfo.get('displayName');
              multipleMasterHost.selectedHost = _host;
              multipleMasterHost.serviceId = services[index];
              multipleMasterHost.isInstalled = false;
              multipleMasterHost.isServiceCoHost = false;
              resultComponents.push(multipleMasterHost);
            });

          }
        } else {
          var savedComponent = masterHosts.findProperty('component', _componentInfo.get('componentName'));
          var componentObj = {};
          componentObj.component_name = _componentInfo.get('componentName');
          componentObj.display_name = _componentInfo.get('displayName');
          componentObj.selectedHost = savedComponent ? savedComponent.hostName : this.selectHostLocally(_componentInfo.get('componentName'));   // call the method that plays selectNode algorithm or fetches from server
          componentObj.isInstalled = savedComponent ? savedComponent.isInstalled : false;
          componentObj.serviceId = services[index];
          componentObj.isServiceCoHost = App.StackServiceComponent.find().findProperty('componentName', _componentInfo.get('componentName')).get('isCoHostedComponent') && !this.get('isReassignWizard');
          resultComponents.push(componentObj);
        }
      }, this);
    }

    callback(resultComponents, this);
  },

  /**
   * @param {string} componentName
   * @returns {bool}
   * @private
   * @method _isHiveCoHost
   */
  _isHiveCoHost: function (componentName) {
    return ['HIVE_METASTORE', 'WEBHCAT_SERVER'].contains(componentName) && !this.get('isReassignWizard');
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

    masterComponents.forEach(function (item) {
      var serviceComponent = App.StackServiceComponent.find().findProperty('componentName', item.component_name);
      var showRemoveControl = installedServices.contains(serviceComponent.get('stackService.serviceName')) &&
        (masterComponents.filterProperty('component_name', item.component_name).length > 1);
      var componentObj = Em.Object.create(item);
      console.log("TRACE: render master component name is: " + item.component_name);
      var masterComponent = App.StackServiceComponent.find().findProperty('componentName', item.component_name);
      if (masterComponent.get('isMasterWithMultipleInstances')) {
        previousComponentName = item.component_name;
        componentObj.set('serviceComponentId', result.filterProperty('component_name', item.component_name).length + 1);
        componentObj.set("showRemoveControl", showRemoveControl);
      }
      componentObj.set('isHostNameValid', true);

      result.push(componentObj);
    }, this);
    result = this.sortComponentsByServiceName(result);
    this.set("selectedServicesMasters", result);
    if (this.get('isReassignWizard')) {
      var components = result.filterProperty('component_name', this.get('content.reassign.component_name'));
      components.setEach('isInstalled', false);
      this.set('servicesMasters', components);
    } else {
      this.set('servicesMasters', result);
    }
  },

  sortComponentsByServiceName: function(components) {
    var displayOrder = App.StackService.displayOrder;
    return components.sort(function (a, b) {
      var aValue = displayOrder.indexOf(a.serviceId) != -1 ? displayOrder.indexOf(a.serviceId) : components.length;
      var bValue = displayOrder.indexOf(b.serviceId) != -1 ? displayOrder.indexOf(b.serviceId) : components.length;
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
        if (hostComponent && dependentHostComponent) dependentHostComponent.set('selectedHost', hostComponent.get('selectedHost'));
      }, this);
    }, this);
  }.observes('selectedServicesMasters.@each.selectedHost'),

  /**
   * select and return host for component by scheme
   * Scheme is an object that has keys which compared to number of hosts,
   * if key more that number of hosts, then return value of that key.
   * Value is index of host in hosts array.
   *
   * @param {object} componentName
   * @param {object} hosts
   * @return {string}
   * @method getHostForComponent
   */
  getHostForComponent: function (componentName, hosts) {
    var component = App.StackServiceComponent.find().findProperty('componentName', componentName);
    if (component) {
      var selectionScheme = App.StackServiceComponent.find().findProperty('componentName', componentName).get('selectionSchemeForMasterComponent');
    } else {
      return hosts[0];
    }

    if (hosts.length === 1 || $.isEmptyObject(selectionScheme)) {
      return hosts[0];
    } else {
      for (var i in selectionScheme) {
        if (window.isFinite(i)) {
          if (hosts.length < window.parseInt(i)) {
            return hosts[selectionScheme[i]];
          }
        }
      }
      return hosts[selectionScheme['else']]
    }
  },

  /**
   * Get list of host names for master component with multiple instances
   * @param {Object} component
   * @param {Object} hosts
   * @returns {string[]}
   * @method getHostsForComponent
   */
  getHostsForComponent: function (component, hosts) {
    var defaultNoOfMasterHosts = component.get('defaultNoOfMasterHosts');
    var masterHosts = [];
    if (hosts.length < defaultNoOfMasterHosts) {
      defaultNoOfMasterHosts = hosts.length;
    }
    for (var index = 0; index < defaultNoOfMasterHosts; index++) {
      masterHosts.push(hosts[index]);
    }
    return masterHosts;
  },

  /**
   * Return hostName of masterNode for specified service
   * @param componentName
   * @return {string|string[]}
   * @method selectHostLocally
   */
  selectHostLocally: function (componentName) {
    var component = App.StackServiceComponent.find().findProperty('componentName', componentName);
    var hostNames = this.get('hosts').mapProperty('host_name');
    if (hostNames.length > 1 && App.StackServiceComponent.find().filterProperty('isNotPreferableOnAmbariServerHost').mapProperty('componentName').contains(componentName)) {
      hostNames = this.get('hosts').mapProperty('host_name').filter(function (item) {
        return item !== location.hostname;
      }, this);
    }
    if (this.get('multipleComponents').contains(componentName)) {
      if (component.get('defaultNoOfMasterHosts') > 1) {
        return this.getHostsForComponent(component, hostNames);
      } else {
        return [this.getHostForComponent(componentName, hostNames)];
      }
    } else {
      return this.getHostForComponent(componentName, hostNames);
    }
  },

  /**
   * On change callback for inputs
   * @param {string} componentName
   * @param {string} selectedHost
   * @param {number} serviceComponentId
   * @method assignHostToMaster
   */
  assignHostToMaster: function (componentName, selectedHost, serviceComponentId) {
    var flag = this.isHostNameValid(componentName, selectedHost);
    this.updateIsHostNameValidFlag(componentName, serviceComponentId, flag);
    if (serviceComponentId) {
      this.get('selectedServicesMasters').filterProperty('component_name', componentName).findProperty("serviceComponentId", serviceComponentId).set("selectedHost", selectedHost);
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
    if (componentName) {
      if (serviceComponentId) {
        this.get('selectedServicesMasters').filterProperty('component_name', componentName).findProperty("serviceComponentId", serviceComponentId).set("isHostNameValid", flag);
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

    var maxNumMasters = this.get("hosts.length"),
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
    if (currentMasters.get("length") < this.get("hosts.length")) {
      currentMasters.set("lastObject.showAddControl", true);
    }

    if (currentMasters.get("length") === 1) {
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
      // For validation use latest received recommendations because ir contains current master layout and recommended slave/client layout
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
        self.set('submitButtonClicked', false);
      };

      if (App.get('supports.serverRecommendValidate')  && this.get('useServerValidation')) {
        self.recommendAndValidate(function () {
          self.showValidationIssuesAcceptBox(goNextStepIfValid);
        });
      } else {
        self.updateIsSubmitDisabled();
        goNextStepIfValid();
      }
    }
  },

  /**
   * In case of any validation issues shows accept dialog box for user which allow cancel and fix issues or continue anyway
   * @method showValidationIssuesAcceptBox
   */
  showValidationIssuesAcceptBox: function(callback) {
    var self = this;
    if (self.get('anyWarning') || self.get('anyError')) {
      App.ModalPopup.show({
        primary: Em.I18n.t('common.continueAnyway'),
        header: Em.I18n.t('installer.step5.validationIssuesAttention.header'),
        body: Em.I18n.t('installer.step5.validationIssuesAttention'),
        onPrimary: function () {
          this.hide();
          callback();
        }
      });
    } else {
      callback();
    }
  }
});
