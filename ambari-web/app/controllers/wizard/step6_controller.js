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
var validationUtils = require('utils/validator');
require('./wizardStep_controller');

/**
 * By Step 6, we have the following information stored in App.db and set on this
 * controller by the router:
 *
 *   hosts: App.db.hosts (list of all hosts the user selected in Step 3)
 *   selectedServiceNames: App.db.selectedServiceNames (the services that the user selected in Step 4)
 *   masterComponentHosts: App.db.masterComponentHosts (master-components-to-hosts mapping the user selected in Step 5)
 *
 * Step 6 will set the following information in App.db:
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */
App.WizardStep6Controller = App.WizardStepController.extend(App.HostComponentValidationMixin, App.HostComponentRecommendationMixin, {

  name: 'wizardStep6Controller',

  stepName: 'step6',

  /**
   * List of hosts
   * @type {object[]}
   */
  hosts: [],

  /**
   * List of components info about selecting/deselecting status for components.
   *
   * @type {Array}
   *  @item {Em.Object}
   *    @property name {String} - component name
   *    @property label {String} - component display name
   *    @property allChecked {bool} - all checkboxes are checked
   *    @property noChecked {bool} - no checkboxes checked
   */
  headers: [],

  /**
   * @type {bool}
   */
  isLoaded: false,

  /**
   * Indication if user has chosen hosts to install clients
   * @type {bool}
   */
  isClientsSet: false,

  /**
   * Define state for submit button
   * @type {bool}
   */
  submitDisabled: Em.computed.or('validationInProgress', 'App.router.btnClickInProgress'),

  /**
   * timer for validation request
   */
  timer: null,

  /**
   * true if request for validation is in progress
   *
   * @type {bool}
   */
  validationInProgress: false,

  /**
   * Check if <code>addHostWizard</code> used
   * @type {bool}
   */
  isAddHostWizard: Em.computed.equal('content.controllerName', 'addHostController'),

  /**
   * Check if <code>installerWizard</code> used
   * @type {bool}
   */
  isInstallerWizard: Em.computed.equal('content.controllerName', 'installerController'),

  isSaved: function () {
    const wizardController = this.get('wizardController');
    if (wizardController) {
      return wizardController.getStepSavedState('step6');
    }
    return false;
  }.property('wizardController.content.stepsSavedState'),

  hostsChanged: function () {
    this.get('wizardController').setStepUnsaved('step6');
  },

  isAllCheckboxesEmpty: function() {
    var hosts = this.get('hosts');
    for (var i = 0; i < hosts.length; i++) {
      var checkboxes = hosts[i].checkboxes;
      for (var j = 0; j < checkboxes.length; j++) {
        if (checkboxes[j].checked) {
          return false;
        }
      }
    }
    return true;
  },

  /**
   * Check if <code>addServiceWizard</code> used
   * @type {bool}
   */
  isAddServiceWizard: Em.computed.equal('content.controllerName', 'addServiceController'),

  installedServiceNames: function () {
    return this.get('content.services').filterProperty('isInstalled').mapProperty('serviceName');
  }.property('content.services').cacheable(),

  /**
   * Validation error messages which don't related with any master
   */
  generalErrorMessages: [],

  /**
   * Validation warning messages which don't related with any master
   */
  generalWarningMessages: [],

  /**
   * true if validation has any general (which is not related with concrete host) error message
   */
  anyGeneralErrors: Em.computed.or('errorMessage','generalErrorMessages.length'),

  /**
   * true if validation has any general (which is not related with concrete host) warning message
   */
  anyGeneralWarnings: Em.computed.gt('generalWarningMessages.length', 0),

  /**
   * true if validation has any general (which is not related with concrete host) error or warning message
   */
  anyGeneralIssues: Em.computed.or('anyGeneralErrors', 'anyGeneralWarnings'),

  anyHostErrors: function () {
    return this.get('hosts').some(function(h) { return h.errorMessages ? h.errorMessages.length > 0 : false;});
  }.property('hosts.@each.errorMessages'),

  /**
   * true if validation has any error message (general or host specific)
   */
  anyErrors: Em.computed.or('anyGeneralErrors', 'anyHostErrors'),

  anyHostWarnings: function () {
    return this.get('hosts').some(function(h) { return h.warnMessages ? h.warnMessages.length > 0 : false;});
  }.property('hosts.@each.warnMessages'),

  /**
   * true if validation has any warning message (general or host specific)
   */
  anyWarnings: Em.computed.or('anyGeneralWarnings', 'anyHostWarnings'),

  /**
   * Verify condition that at least one checkbox of each component was checked
   * @method clearError
   */
  clearError: function () {
    var self = this;
    var isError = false;
    var err = false;
    var hosts = this.get('hosts');
    var headers = this.get('headers');
    var headersMap = headers.toWickMapByProperty('name');

    hosts.forEach(function (host) {
      host.checkboxes.forEach(function (checkbox) {
        if (headersMap[checkbox.component]) {
          headersMap[checkbox.component] = !checkbox.checked;
        }
      });
    });
    for (var i in headersMap) {
      err |= headersMap[i];
    }

    if (!err) {
      this.set('errorMessage', '');
    }

    if (this.get('isAddHostWizard')) {
      hosts.forEach(function (host) {
        isError = false;
        headers.forEach(function (header) {
          isError |= host.checkboxes.findProperty('title', header.get('label')).checked;
        });
        isError = !isError;
        if (!isError) {
          self.set('errorMessage', '');
        }
      });
    }
  },

  /**
   * Clear Step6 data like <code>hosts</code>, <code>headers</code> etc
   * @method clearStep
   */
  clearStep: function () {
    this.set('hosts', []);
    this.set('headers', []);
    this.clearError();
    this.set('isLoaded', false);
    this.set('validationInProgress', false);
  },

  /**
   * Enable some service for all hosts
   * @param {object} event
   * @method selectAllNodes
   */
  selectAllNodes: function (event) {
    var name = Em.get(event, 'context.name');
    if (name) {
      this.setAllNodes(name, true);
    }
  },

  /**
   * Disable some services for all hosts
   * @param {object} event
   * @method deselectAllNodes
   */
  deselectAllNodes: function (event) {
    var name = Em.get(event, 'context.name');
    if (name) {
      this.setAllNodes(name, false);
    }
  },

  /**
   * Enable/disable some service for all hosts
   * @param {String} component - component name
   * @param {bool} checked - true - enable, false - disable
   * @method setAllNodes
   */
  setAllNodes: function (component, checked) {
    this.get('hosts').forEach(function (host) {
      host.checkboxes.filterProperty('isInstalled', false).forEach(function (checkbox) {
        if (checkbox.component === component) {
          Em.set(checkbox, 'checked', checked);
        }
      });
    });
    this.checkCallback(component);
    this.hostsChanged();
  },

  /**
   * Checkbox check callback
   * Verify if all/none checkboxes for current component are checked
   * @param {String} component
   * @method checkCallback
   */
  checkCallback: function (component) {
    var header = this.get('headers').findProperty('name', component);
    if (header) {
      var hosts = this.get('hosts');
      var allTrue = true;
      var allFalse = true;
      hosts.forEach(function (host) {
        host.checkboxes.forEach(function (checkbox) {
          if (checkbox.component === component && !checkbox.isInstalled) {
            allTrue = allTrue && checkbox.checked;
            allFalse = allFalse && !checkbox.checked;
          }
        });
      });
      header.set('allChecked', allTrue);
      header.set('noChecked', allFalse);
    }
    this.clearError();
  },

  /**
   * Init step6 data
   * @method loadStep
   */
  loadStep: function () {
    this.clearStep();

    var parentController = App.router.get(this.get('content.controllerName'));
    if (parentController && parentController.get('content.componentsFromConfigs')) {
      parentController.clearConfigActionComponents();
    }

    var selectedServices = App.StackService.find().filterProperty('isSelected');
    var installedServices = App.StackService.find().filterProperty('isInstalled');
    var services;
    if (this.get('isInstallerWizard')) services = selectedServices;
    else if (this.get('isAddHostWizard')) services = installedServices;
    else if (this.get('isAddServiceWizard')) services = installedServices.concat(selectedServices);

    var headers = Em.A([]);
    services.forEach(function (stackService) {
      stackService.get('serviceComponents').forEach(function (serviceComponent) {
        if (serviceComponent.get('isShownOnInstallerSlaveClientPage')) {
          headers.pushObject(Em.Object.create({
            name: serviceComponent.get('componentName'),
            label: App.format.role(serviceComponent.get('componentName'), false),
            allChecked: false,
            isRequired: serviceComponent.get('isRequired'),
            noChecked: true,
            isDisabled: installedServices.someProperty('serviceName', stackService.get('serviceName')) && this.get('isAddServiceWizard'),
            allId: 'all-' + serviceComponent.get('componentName'),
            noneId: 'none-' + serviceComponent.get('componentName')
          }));
        }
      }, this);
    }, this);
    if (this.get('content.clients') && !!this.get('content.clients').length) {
      headers.pushObject(Em.Object.create({
        name: 'CLIENT',
        label: App.format.role('CLIENT', false),
        allChecked: false,
        noChecked: true,
        isDisabled: false,
        allId: 'all-CLIENT',
        noneId: 'none-CLIENT'
      }));
    }
    this.get('headers').pushObjects(headers);

    this.render();
    if (this.get('content.skipSlavesStep')) {
      App.router.send('next');
    }
  },

  /**
   * Returns list of new hosts
   *
   * @param {object[]} [allHosts=null]
   * @return {object[]}
   */
  getNewHosts: function(allHosts) {
    var hosts = allHosts || this.getAllHosts();
    return hosts.filterProperty('isInstalled', false);
  },

  /**
   * Returns list of registered hosts
   *
   * @return {object[{hostName, isInstalled}]}
   */
  getAllHosts: function() {
    var self = this;
    var hosts = self.get('content.hosts');
    return Em.keys(this.get('content.hosts')).reduce(function(res, hostName) {
      var host = hosts[hostName];
      if (Em.get(host, 'bootStatus') !== 'REGISTERED') {
        return res;
      }
      return res.concat({
        hostName: hostName,
        isInstalled: Em.getWithDefault(host, 'isInstalled', false)
      });
    }, []);
  },
  /**
   * Load all data needed for this module. Then it automatically renders in template
   * @method render
   */
  render: function () {
    var hostsObj = [],
      masterHosts = [],
      headers = this.get('headers'),
      masterHostNames = this.get('content.masterComponentHosts').mapProperty('hostName').uniq(),
      masterHostNamesMap = masterHostNames.toWickMap(),
      hosts = this.get('isAddHostWizard') ? this.getNewHosts() : this.getAllHosts();

    var i = 1;
    hosts.mapProperty('hostName').forEach(function (_hostName) {
      var hasMaster = masterHostNamesMap[_hostName];
      var obj = {
        hostName: _hostName,
        hasMaster: hasMaster,
        checkboxes: headers.map(function (header, index) {
          return {
            component: header.name,
            title: header.label,
            checked: false,
            isInstalled: false,
            isDisabled: header.get('isDisabled'),
            uId: _hostName + '-checkbox-' + index,
            dataQaAttr: header.name === 'CLIENT' ? 'client-component' : ''
          };
        }),
        isLast: i === hosts.length ? true : false
      };

      if (hasMaster) {
        masterHosts.pushObject(obj);
      } else {
        hostsObj.pushObject(obj);
      }
      i++;
    });
    //hosts with master components should be in the beginning of list
    hostsObj.unshift.apply(hostsObj, masterHosts);
    hostsObj = this.renderSlaves(hostsObj);
    this.set('hosts', hostsObj);
    headers.forEach(function (header) {
      this.checkCallback(header.get('name'));
    }, this);
    this.set('isLoaded', true);
  },

  /**
   * Set checked values for slaves checkboxes
   * @param {Array} hostsObj
   * @return {Array}
   * @method renderSlaves
   */
  renderSlaves: function (hostsObj) {
    var slaveComponents = this.get('content.slaveComponentHosts');

    if (Em.isNone(slaveComponents)) { // we are at this page for the first time
      this.selectRecommendedComponents(hostsObj);
      this.setInstalledComponents(hostsObj);
    } else {
     this.restoreComponentsSelection(hostsObj, slaveComponents);
    }
    if (this.get('isAddServiceWizard')) {
      this.enableCheckboxesForDependentComponents(hostsObj);
    }
    this.selectClientHost(hostsObj);
    return hostsObj;
  },

  /**
   * set installed flag of host-components
   * @param {Array} hostsObj
   * @returns {boolean}
   */
  setInstalledComponents: function(hostsObj) {
    if (Em.isNone(this.get('content.installedHosts'))) return false;
    var hosts = this.get('content.installedHosts');

    hostsObj.forEach(function(host) {
      var installedHost = hosts[host.hostName];
      var installedComponents = installedHost ? installedHost.hostComponents.mapProperty('HostRoles.component_name') : [];

      host.checkboxes.forEach(function(checkbox) {
        checkbox.isInstalled = installedComponents.contains(checkbox.component);
        if (checkbox.isInstalled) {
          checkbox.checked = true;
        }
      });
    });
  },

  /**
   * Enable checkboxes for dependent components of already installed services, that can be added
   * @param hostsObj
   */
  enableCheckboxesForDependentComponents: function (hostsObj) {
    var dependentSlaves = {};
    App.StackService.find().filterProperty('isSelected').forEach(function (service) {
      service.get('serviceComponents').forEach(function (component) {
        component.get('dependencies').forEach(function (dependency) {
          var dependentService = App.StackService.find().findProperty('serviceName', dependency.serviceName);
          if (!dependentService) {
            return;
          }
          var dependentComponent = dependentService.get('serviceComponents').findProperty('componentName', dependency.componentName);
          if (dependentComponent.get('isSlave') && dependentService.get('isInstalled')) {
            dependentSlaves[dependentComponent.get('componentName')] = [];
          }
        });
      });
    });

    if (!Em.keys(dependentSlaves)) return false;

    hostsObj.forEach(function (hostObj) {
      hostObj.checkboxes.forEach(function (checkbox) {
        if (dependentSlaves[checkbox.component] && !checkbox.isInstalled) {
          dependentSlaves[checkbox.component].push(checkbox);
        }
      });
    });

    for (var component in dependentSlaves) {
      if (dependentSlaves.hasOwnProperty(component)) {
        var maxToInstall = App.StackServiceComponent.find().findProperty('componentName', component).get('maxToInstall');
        maxToInstall = maxToInstall === Infinity ? hostsObj.length : maxToInstall;
        if (maxToInstall > hostsObj.length - dependentSlaves[component].length) {
          dependentSlaves[component].setEach('isDisabled', false);
        }
      }
    }
    return true;
  },

  /**
   * restore previous component selection
   * @param {Array} hostsObj
   * @param {Array} slaveComponents
   */
  restoreComponentsSelection: function(hostsObj, slaveComponents) {
    var slaveComponentsMap = slaveComponents.toMapByProperty('componentName');
    var hostsObjMap = hostsObj.toMapByProperty('hostName');

    this.get('headers').forEach(function (header) {
      var slaveComponent = slaveComponentsMap[header.get('name')];
      if (slaveComponent) {
        slaveComponent.hosts.forEach(function (_node) {
          var node = hostsObjMap[_node.hostName];
          if (node) {
            Em.set(node.checkboxes.findProperty('title', header.get('label')), 'checked', true);
            Em.set(node.checkboxes.findProperty('title', header.get('label')), 'isInstalled', _node.isInstalled);
          }
        });
      }
    });
  },

  /**
   * select component which should be checked according to recommendations
   * @param hostsObj
   */
  selectRecommendedComponents: function(hostsObj) {
    var recommendations = this.get('content.recommendations'),
        recommendedMap = {},
        clientComponentsMap = App.get('components.clients').toWickMap();

    recommendations.blueprint.host_groups.forEach(function(hostGroup) {
      var group = recommendations.blueprint_cluster_binding.host_groups.findProperty('name', hostGroup.name);
      var hosts = group.hosts || [];

      hosts.forEach(function (host) {
        recommendedMap[host.fqdn] = hostGroup.components.mapProperty('name');
      });
    });

    hostsObj.forEach(function (host) {
      var checkboxes = host.checkboxes;
      var hostComponents = recommendedMap[host.hostName] || [];
      checkboxes.forEach(function (checkbox) {
        var checked;
        if (!checkbox.isDisabled) {
          checked = hostComponents.contains(checkbox.component);

          if (checkbox.component === 'CLIENT' && !checked) {
            checked = hostComponents.some(function (componentName) {
              return clientComponentsMap[componentName];
            });
          }
          checkbox.checked = checked;
        }
      });
    });
  },

  /**
   * For clients - select first non-master host, if all has masters then last host
   * @param hostsObj
   */
  selectClientHost: function (hostsObj) {
    if (!this.get('isClientsSet')) {
      var nonMasterHost = hostsObj.findProperty('hasMaster', false);
      var clientHost = !!nonMasterHost ? nonMasterHost : hostsObj[hostsObj.length - 1]; // last host
      var clientCheckBox = clientHost.checkboxes.findProperty('component', 'CLIENT');
      if (clientCheckBox) {
        Em.set(clientCheckBox, 'checked', true);
      }
      this.set('isClientsSet', true);
    }
  },

  /**
   * Select checkboxes which correspond to master components
   *
   * @param {Array} hostsObj
   * @return {Array}
   * @method selectMasterComponents
   */
  selectMasterComponents: function (hostsObj) {
    var masterComponentHosts = this.get('content.masterComponentHosts');

    if (masterComponentHosts) {
      masterComponentHosts.forEach(function (item) {
        var host = hostsObj.findProperty('hostName', item.hostName);
        if (host) {
          var checkbox = host.get('checkboxes').findProperty('component', item.component);
          if (checkbox) {
            checkbox.set('checked', true);
          }
        }
      });
    }
    return hostsObj;
  },

  /**
   * Return list of master components for specified <code>hostname</code>
   * @param {string} hostName
   * @return {string[]}
   * @method getMasterComponentsForHost
   */
  getMasterComponentsForHost: function (hostName) {
    return this.get('content.masterComponentHosts').filterProperty('hostName', hostName).mapProperty('component');
  }
});
