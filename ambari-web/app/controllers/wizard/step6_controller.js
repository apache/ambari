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
App.WizardStep6Controller = Em.Controller.extend({

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
      host.get('checkboxes').forEach(function (checkbox) {
        if (headersMap[checkbox.get('component')]) {
          headersMap[checkbox.get('component')] = !checkbox.get('checked');
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
          isError |= host.get('checkboxes').findProperty('title', header.get('label')).checked;
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
      host.get('checkboxes').filterProperty('isInstalled', false).forEach(function (checkbox) {
        if (checkbox.get('component') === component) {
          checkbox.set('checked', checked);
        }
      });
    });
    this.checkCallback(component);
  },

  /**
   * Return whether service was selected or not
   * @param {string} name serviceName
   * @return {bool}
   * @method isServiceSelected
   */
  isServiceSelected: function (name) {
    var serviceName = this.get('content.services').findProperty('serviceName', name);
    if (!serviceName) {
      return !!serviceName;
    }
    return serviceName.get('isSelected') || serviceName.get('isInstalled');
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
        host.get('checkboxes').forEach(function (checkbox) {
          if (checkbox.get('component') === component && !checkbox.get('isInstalled')) {
            allTrue = allTrue && checkbox.get('checked');
            allFalse = allFalse && !checkbox.get('checked');
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

    var self = this;

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
            label: serviceComponent.get('displayName'),
            allChecked: false,
            noChecked: true
          }));
        }
      }, this);
    }, this);

    headers.pushObject(Em.Object.create({
      name: 'CLIENT',
      label: App.format.role('CLIENT'),
      allChecked: false,
      noChecked: true
    }));

    this.get('headers').pushObjects(headers);

    this.render();
    if (this.get('content.skipSlavesStep')) {
      App.router.send('next');
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
          if(!getUninstalledHosts || !hostInfo[index].isInstalled) {
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
      masterHostNames = this.get('content.masterComponentHosts').mapProperty('hostName').uniq();

    this.getHostNames().forEach(function (_hostName) {
      var hasMaster = masterHostNames.contains(_hostName);

      var obj = Em.Object.create({
        hostName: _hostName,
        hasMaster: hasMaster,
        checkboxes: []
      });

      headers.forEach(function (header) {
        obj.checkboxes.pushObject(Em.Object.create({
          component: header.name,
          title: header.label,
          checked: false,
          isInstalled: false
        }));
      });

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
    var headers = this.get('headers');
    var slaveComponents = this.get('content.slaveComponentHosts');
    if (!slaveComponents) { // we are at this page for the first time
      var client_is_set = false;
      hostsObj.forEach(function (host) {
        var checkboxes = host.get('checkboxes');
        checkboxes.setEach('checked', !host.hasMaster);
        checkboxes.setEach('isInstalled', false);
        checkboxes.findProperty('title', headers.findProperty('name', 'CLIENT').get('label')).set('checked', false);
        // First not Master should have Client (only first!)
        if (!client_is_set) {
          var dfs = App.StackService.find().findProperty('isPrimaryDFS');
          if (dfs.get('isSelected') || dfs.get('isInstalled')) {
            var checkboxServiceComponent = checkboxes.findProperty('title', headers.findProperty('name', dfs.get('serviceComponents').
              findProperty('isShownOnInstallerSlaveClientPage').get('componentName')).get('label'));
            if (checkboxServiceComponent && checkboxServiceComponent.get('checked')) {
              checkboxes.findProperty('title', headers.findProperty('name', 'CLIENT').get('label')).set('checked', true);
              client_is_set = true;
            }
          }
        }
      });

      if (this.get('isInstallerWizard') && hostsObj.everyProperty('hasMaster', true)) {
        var lastHost = hostsObj[hostsObj.length - 1];
        lastHost.get('checkboxes').setEach('checked', true);
      }
    } else {
      this.get('headers').forEach(function (header) {
        var nodes = slaveComponents.findProperty('componentName', header.get('name'));
        if (nodes) {
          nodes.hosts.forEach(function (_node) {
            var node = hostsObj.findProperty('hostName', _node.hostName);
            if (node) {
              node.get('checkboxes').findProperty('title', header.get('label')).set('checked', true);
              node.get('checkboxes').findProperty('title', header.get('label')).set('isInstalled', _node.isInstalled);
            }
          });
        }
      });
    }
    return hostsObj;
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


  /**
   * Validate form. Return do we have errors or not
   * @return {bool}
   * @method validate
   */
  validate: function () {

    if (this.get('isAddHostWizard')) {
      return this.validateEachHost(Em.I18n.t('installer.step6.error.mustSelectOneForHost'));
    }
    else {
      if (this.get('isInstallerWizard')) {
        return this.validateEachComponent() && this.validateEachHost(Em.I18n.t('installer.step6.error.mustSelectOneForSlaveHost'));
      }
      else {
        if (this.get('isAddServiceWizard')) {
          return this.validateEachComponent();
        }
        return true;
      }
    }
  },

  /**
   * Validate all components for each host. Return do we have errors or not
   * @return {bool}
   * @method validateEachHost
   */
  validateEachHost: function (errorMsg) {

    var isError = false;
    var hosts = this.get('hosts');
    var headers = this.get('headers');
    for (var i = 0; i < hosts.length; i++) {
      if (this.get('isInstallerWizard') && this.get('content.masterComponentHosts').someProperty('hostName', hosts[i].hostName)) {
        continue;
      }
      var checkboxes = hosts[i].get('checkboxes');
      isError = false;
      headers.forEach(function (header) {
        isError = isError || checkboxes.findProperty('title', header.get('label')).checked;
      });
      isError = !isError;
      if (isError) {
        this.set('errorMessage', errorMsg);
        break;
      }
    }
    return !isError;
  },

  /**
   * Validate a component for all hosts. Return do we have errors or not
   * @return {bool}
   * @method validateEachComponent
   */
  validateEachComponent: function () {
    var isError = false;
    var hosts = this.get('hosts');
    var headers = this.get('headers');
    headers.forEach(function (header) {
      var all_false = true;
      hosts.forEach(function (host) {
        var checkboxes = host.get('checkboxes');
        all_false = all_false && !checkboxes.findProperty('title', header.get('label')).checked;
      });
      isError = isError || all_false;
    });
    if (isError) {
      this.set('errorMessage', Em.I18n.t('installer.step6.error.mustSelectOne'));
    }
    return !isError;
  }

});
