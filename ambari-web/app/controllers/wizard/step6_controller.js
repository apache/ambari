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
var db = require('utils/db');
var stringUtils = require('utils/string_utils');
var blueprintUtils = require('utils/blueprint');
var validationUtils = require('utils/validator');

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
App.WizardStep6Controller = Em.Controller.extend(App.BlueprintMixin, {

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
  submitDisabled: false,

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
  isAddHostWizard: function () {
    return this.get('content.controllerName') === 'addHostController';
  }.property('content.controllerName'),

  /**
   * Check if <code>installerWizard</code> used
   * @type {bool}
   */
  isInstallerWizard: function () {
    return this.get('content.controllerName') === 'installerController';
  }.property('content.controllerName'),

  /**
   * Check if <code>addServiceWizard</code> used
   * @type {bool}
   */
  isAddServiceWizard: function () {
    return this.get('content.controllerName') === 'addServiceController';
  }.property('content.controllerName'),

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
  anyGeneralErrors: function() {
    var messages = this.get('generalErrorMessages');
    return this.get('errorMessage') || (messages && messages.length > 0);
  }.property('generalErrorMessages', 'generalErrorMessages.@each', 'errorMessage'),

  /**
   * true if validation has any general (which is not related with concrete host) warning message
   */
  anyGeneralWarnings: function() {
    var messages = this.get('generalWarningMessages');
    return messages && messages.length > 0;
  }.property('generalWarningMessages', 'generalWarningMessages.@each'),

  /**
   * true if validation has any general (which is not related with concrete host) error or warning message
   */
  anyGeneralIssues: function () {
    return this.get('anyGeneralErrors') || this.get('anyGeneralWarnings');
  }.property('anyGeneralErrors', 'anyGeneralWarnings'),

  /**
   * true if validation has any error message (general or host specific)
   */
  anyErrors: function() {
    return this.get('anyGeneralErrors') || this.get('hosts').some(function(h) { return h.errorMessages.length > 0; });
  }.property('anyGeneralErrors', 'hosts.@each.errorMessages'),

  /**
   * true if validation has any warning message (general or host specific)
   */
  anyWarnings: function() {
    return this.get('anyGeneralWarnings') || this.get('hosts').some(function(h) { return h.warnMessages.length > 0; });
  }.property('anyGeneralWarnings', 'hosts.@each.warnMessages'),

  openSlavesAndClientsIssues: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step6.validationSlavesAndClients.popup.header'),
      bodyClass: Em.View.extend({
        controller: this,
        templateName: require('templates/wizard/step6/step6_issues_popup')
      }),
      secondary: null
    });
  },

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
    var headersMap = {};

    headers.forEach(function (header) {
      headersMap[header.name] = true;
    });
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
      this.callValidation();
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
      this.callValidation();
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

    console.log("WizardStep6Controller: Loading step6: Assign Slaves");
    this.clearStep();
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
    } else {
      this.callValidation();
    }
  },

  /**
   * Get active host names
   * @return {string[]}
   * @method getHostNames
   */
  getHostNames: function () {
    var hostInfo = this.get('content.hosts');
    var hostNames = [];
    //flag identify whether get all hosts or only uninstalled(newly added) hosts
    var getUninstalledHosts = (this.get('content.controllerName') !== 'addServiceController');

    for (var index in hostInfo) {
      if (hostInfo.hasOwnProperty(index)) {
        if (hostInfo[index].bootStatus === 'REGISTERED') {
          if (!getUninstalledHosts || !hostInfo[index].isInstalled) {
            hostNames.push(hostInfo[index].name);
          }
        }
      }
    }
    return hostNames;
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
      masterHostNamesMap = {};
    masterHostNames.forEach(function(hostName) {
      masterHostNamesMap[hostName] = true;
    });

    this.getHostNames().forEach(function (_hostName) {
      var hasMaster = masterHostNamesMap[_hostName];

      var obj = {
        hostName: _hostName,
        hasMaster: hasMaster,
        checkboxes: headers.map(function (header) {
          return{
            component: header.name,
            title: header.label,
            checked: false,
            isInstalled: false,
            isDisabled: header.get('isDisabled')
          };
        })
      };

      if (hasMaster) {
        masterHosts.pushObject(obj)
      } else {
        hostsObj.pushObject(obj);
      }
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
      var installedComponents = (installedHost) ?
        installedHost.hostComponents.mapProperty('HostRoles.component_name') : [];

      host.checkboxes.forEach(function(checkbox) {
        checkbox.isInstalled = installedComponents.contains(checkbox.component);
      });
    });
  },

  /**
   * restore previous component selection
   * @param {Array} hostsObj
   * @param {Array} slaveComponents
   */
  restoreComponentsSelection: function(hostsObj, slaveComponents) {
    var slaveComponentsMap = {};
    var hostsObjMap = {};

    slaveComponents.forEach(function(c) {
      slaveComponentsMap[c.componentName] = c;
    });
    hostsObj.forEach(function(h) {
      hostsObjMap[h.hostName] = h;
    });

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
      clientComponentsMap = {};

    App.get('components.clients').forEach(function(componentName) {
      clientComponentsMap[componentName] = true;
    });

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
        var checked = hostComponents.contains(checkbox.component);

        if (checkbox.component === 'CLIENT' && !checked) {
          checked = hostComponents.some(function(componentName) {
            return clientComponentsMap[componentName];
          });
        }
        checkbox.checked = checked;
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
    console.log('Master components selected on:', masterComponentHosts.mapProperty('hostName').uniq().join(", "));

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
  },

  callValidation: function (successCallback) {
    var self = this;
    clearTimeout(this.get('timer'));
    if (this.get('validationInProgress')) {
      this.set('timer', setTimeout(function () {
        self.callValidation(successCallback);
      }, 700));
    } else {
      this.callServerSideValidation(successCallback);
    }
  },

  /**
   * Update submit button status
   * @method callServerSideValidation
   */
  callServerSideValidation: function (successCallback) {
    var self = this;
    this.set('validationInProgress', true);

    var selectedServices = App.StackService.find().filterProperty('isSelected').mapProperty('serviceName');
    var installedServices = App.StackService.find().filterProperty('isInstalled').mapProperty('serviceName');
    var services = installedServices.concat(selectedServices).uniq();

    var hostNames = self.get('hosts').mapProperty('hostName');
    var slaveBlueprint = self.getCurrentBlueprint();
    var masterBlueprint = null;
    var invisibleSlavesAndClients = App.StackServiceComponent.find().filter(function (component) {
      return component.get("isSlave") && component.get("isShownOnInstallerSlaveClientPage") === false ||
        component.get("isClient") && component.get("isRequiredOnAllHosts");
    }).mapProperty("componentName");

    if (this.get('isInstallerWizard') || this.get('isAddServiceWizard')) {
      masterBlueprint = self.getCurrentMastersBlueprint();

      var invisibleMasters = [];
      if (this.get('isInstallerWizard')) {
        invisibleMasters = App.StackServiceComponent.find().filterProperty("isMaster").filterProperty("isShownOnInstallerAssignMasterPage", false).mapProperty("componentName");
      }
      else
        if (this.get('isAddServiceWizard')) {
          invisibleMasters = App.StackServiceComponent.find().filterProperty("isMaster").filterProperty("isShownOnAddServiceAssignMasterPage", false).mapProperty("componentName");
        }

      var selectedClientComponents = self.get('content.clients').mapProperty('component_name');
      var alreadyInstalledClients = App.get('components.clients').reject(function (c) {
        return selectedClientComponents.contains(c);
      });

      var invisibleComponents = invisibleMasters.concat(invisibleSlavesAndClients).concat(alreadyInstalledClients);

      var invisibleBlueprint = blueprintUtils.filterByComponents(this.get('content.recommendations'), invisibleComponents);
      masterBlueprint = blueprintUtils.mergeBlueprints(masterBlueprint, invisibleBlueprint);
    }
    else
      if (this.get('isAddHostWizard')) {
        masterBlueprint = self.getCurrentMasterSlaveBlueprint();
        hostNames = hostNames.concat(App.Host.find().mapProperty("hostName")).uniq();
        slaveBlueprint = blueprintUtils.addComponentsToBlueprint(slaveBlueprint, invisibleSlavesAndClients);
      }

    var bluePrintsForValidation = blueprintUtils.mergeBlueprints(masterBlueprint, slaveBlueprint);
    this.set('content.recommendationsHostGroups', bluePrintsForValidation);

    return App.ajax.send({
      name: 'config.validations',
      sender: self,
      data: {
        stackVersionUrl: App.get('stackVersionURL'),
        hosts: hostNames,
        services: services,
        validate: 'host_groups',
        recommendations: bluePrintsForValidation
      },
      success: 'updateValidationsSuccessCallback',
      error: 'updateValidationsErrorCallback'
    }).
      then(function () {
        self.set('validationInProgress', false);
        if (!self.get('submitDisabled') && successCallback) {
          successCallback();
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
    //data = JSON.parse(data); // temporary fix

    var clientComponents = App.get('components.clients');

    this.set('generalErrorMessages', []);
    this.set('generalWarningMessages', []);
    this.get('hosts').setEach('warnMessages', []);
    this.get('hosts').setEach('errorMessages', []);
    this.get('hosts').setEach('anyMessage', false);
    this.get('hosts').forEach(function (host) {
      host.checkboxes.setEach('hasWarnMessage', false);
      host.checkboxes.setEach('hasErrorMessage', false);
    });
    var anyErrors = false;
    var anyGeneralClientErrors = false; // any error/warning for any client component (under "CLIENT" alias)

    var validationData = validationUtils.filterNotInstalledComponents(data);
    validationData.filterProperty('type', 'host-component').filter(function (i) {
      return !(i['component-name'] && App.StackServiceComponent.find().findProperty('componentName', i['component-name']).get('isMaster'));
    }).forEach(function (item) {
      var checkboxWithIssue = null;
      var isGeneralClientValidationItem = clientComponents.contains(item['component-name']); // it is an error/warning for any client component (under "CLIENT" alias)
      var host = self.get('hosts').find(function (h) {
        return h.hostName === item.host && h.checkboxes.some(function (checkbox) {
          var isClientComponent = checkbox.component === "CLIENT" && isGeneralClientValidationItem;
          if (checkbox.component === item['component-name'] || isClientComponent) {
            checkboxWithIssue = checkbox;
            return true;
          } else {
            return false;
          }
        });
      });
      if (host) {
        Em.set(host, 'anyMessage', true);

        if (item.level === 'ERROR') {
          anyErrors = true;
          host.errorMessages.pushObject(item.message);
          Em.set(checkboxWithIssue, 'hasErrorMessage', true);
        }
        else
          if (item.level === 'WARN') {
            host.warnMessages.pushObject(item.message);
            Em.set(checkboxWithIssue, 'hasWarnMessage', true);
          }
      }
      else {
        var component;
        if (isGeneralClientValidationItem) {
          if (!anyGeneralClientErrors) {
            anyGeneralClientErrors = true;
            component = "Client";
          }
        }
        else {
          component = item['component-name'];
        }

        if (component || !item['component-name']) {
          var details = "";
          if (item.host) {
            details += " (" + item.host + ")";
          }

          if (item.level === 'ERROR') {
            anyErrors = true;
            self.get('generalErrorMessages').push(item.message + details);
          }
          else
            if (item.level === 'WARN') {
              self.get('generalWarningMessages').push(item.message + details);
            }
        }
      }
    });

    // use this.set('submitDisabled', anyErrors); is validation results should block next button
    // It's because showValidationIssuesAcceptBox allow use accept validation issues and continue
    // this.set('submitDisabled', false);
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
   * Composes selected values of comboboxes into blueprint format
   */
  getCurrentBlueprint: function () {
    var self = this;

    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    var clientComponents = self.get('content.clients').mapProperty('component_name');
    var mapping = self.get('hosts');

    mapping.forEach(function (item, i) {
      var group_name = 'host-group-' + (i+1);

      var host_group = {
        name: group_name,
        components: item.checkboxes.filterProperty('checked', true).map(function (checkbox) {
          if (checkbox.component === "CLIENT") {
            return clientComponents.map(function (client) {
              return { name: client };
            });
          } else {
            return { name: checkbox.component };
          }
        })
      };

      host_group.components = [].concat.apply([], host_group.components);

      var binding = {
        name: group_name,
        hosts: [
          { fqdn: item.hostName }
        ]
      };

      res.blueprint.host_groups.push(host_group);
      res.blueprint_cluster_binding.host_groups.push(binding);
    });

    return res;
  },

  /**
   * Create blueprint from assigned master components to appropriate hosts
   * @returns {Object}
   * @method getCurrentMastersBlueprint
   */
  getCurrentMastersBlueprint: function () {
    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    var masters = this.get('content.masterComponentHosts');
    var hosts = this.get('content.hosts');

    Em.keys(hosts).forEach(function (host, i) {
      var group_name = 'host-group-' + (i + 1);
      var components = [];
      masters.forEach(function (master) {
        if (master.hostName === host) {
          components.push({
            name: master.component
          });
        }
      });
      res.blueprint.host_groups.push({
        name: group_name,
        components: components
      });
      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: [
          {
            fqdn: host
          }
        ]
      });
    }, this);
    return blueprintUtils.mergeBlueprints(res, this.getCurrentSlaveBlueprint());
  },

  /**
   * In case of any validation issues shows accept dialog box for user which allow cancel and fix issues or continue anyway
   * @metohd submit
   */
  showValidationIssuesAcceptBox: function(callback) {
    var self = this;

    if (self.get('anyWarnings') || self.get('anyErrors')) {
      App.ModalPopup.show({
        primary: Em.I18n.t('common.continueAnyway'),
        header: Em.I18n.t('installer.step6.validationIssuesAttention.header'),
        body: Em.I18n.t('installer.step6.validationIssuesAttention'),
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
