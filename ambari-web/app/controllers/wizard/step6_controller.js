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

  hosts: [],
  /**
   * List of components info about selecting/deselecting status for components.
   *
   * @type {Array}
   *  @item {Em.Object}
   *    @property name {String} - component name
   *    @property label {String} - component display name
   *    @property allChecked {Boolean} - all checkboxes are checked
   *    @property noChecked {Boolean} - no checkboxes checked
   */
  headers: [],

  /**
   * true - assign ZK, HB
   * false - slaves and clients
   */
  isMasters: false,
  isLoaded: false,

  components: require('data/service_components'),

  isAddHostWizard: function () {
    return this.get('content.controllerName') === 'addHostController';
  }.property('content.controllerName'),

  isInstallerWizard: function () {
    return this.get('content.controllerName') === 'installerController';
  }.property('content.controllerName'),

  isAddServiceWizard: function() {
    return this.get('content.controllerName') === 'addServiceController';
  }.property('content.controllerName'),

  /**
   * verify condition that at least one checkbox of each component was checked
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
      if (this.get('isMasters')) {
        this.set('errorMessage', '');
      }
      else {
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
    }
  },

  clearStep: function () {
    this.set('hosts', []);
    this.set('headers', []);
    this.clearError();
    this.set('isLoaded', false);
  },

  /**
   * Enable some service for all hosts
   * @param event
   */
  selectAllNodes: function (event) {
    this.setAllNodes(event.context.name, true);
  },

  /**
   * Disable some services for all hosts
   * @param event
   */
  deselectAllNodes: function (event) {
    this.setAllNodes(event.context.name, false);
  },

  /**
   * Enable/disable some service for all hosts
   * @param {String} component - component name
   * @param {Boolean} checked - true - enable, false - disable
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
   * @param name serviceName
   * @return {*}
   */
  isServiceSelected: function (name) {
    return !!(this.get('content.services').findProperty('serviceName', name) &&
      this.get('content.services').findProperty('serviceName', name).get('isSelected'));
  },

  /**
   * Checkbox check callback
   * @param {String} component
   */
  checkCallback: function (component) {
    var header = this.get('headers').findProperty('name', component);
    var hosts = this.get('hosts');
    var allTrue = true;
    var allFalse = true;
    hosts.forEach(function (host) {
      host.get('checkboxes').forEach(function (checkbox) {
        if (checkbox.get('component') === component && !checkbox.get('isInstalled')) {
          allTrue &= checkbox.get('checked');
          allFalse &= !checkbox.get('checked');
        }
      });
    });
    header.set('allChecked', allTrue);
    header.set('noChecked', allFalse);
    this.clearError();
  },

  getComponentDisplayName: function (componentName) {
    return this.get('components').findProperty('component_name', componentName).display_name
  },

  loadStep: function () {

    var self = this;

    console.log("WizardStep6Controller: Loading step6: Assign Slaves");
    this.clearStep();

    var headers = [];

    if (this.get('isMasters')) {
      if (this.isServiceSelected('HBASE') && App.supports.multipleHBaseMasters) {
        headers.pushObject(Em.Object.create({
          name: 'HBASE_MASTER',
          label: self.getComponentDisplayName('HBASE_MASTER')
        }));
      }
      if (this.isServiceSelected('ZOOKEEPER')) {
        headers.pushObject(Em.Object.create({
          name: 'ZOOKEEPER_SERVER',
          label: self.getComponentDisplayName('ZOOKEEPER_SERVER')
        }));
      }
    }
    else {
      if (this.isServiceSelected('HDFS')) {
        headers.pushObject(Ember.Object.create({
          name: 'DATANODE',
          label: self.getComponentDisplayName('DATANODE')
        }));
      }
      if (this.isServiceSelected('MAPREDUCE')) {
        headers.pushObject(Em.Object.create({
          name: 'TASKTRACKER',
          label: self.getComponentDisplayName('TASKTRACKER')
        }));
      }
      if (this.isServiceSelected('YARN')) {
        headers.pushObject(Em.Object.create({
          name: 'NODEMANAGER',
          label: self.getComponentDisplayName('NODEMANAGER')
        }));
      }
      if (this.isServiceSelected('HBASE')) {
        headers.pushObject(Em.Object.create({
          name: 'HBASE_REGIONSERVER',
          label: self.getComponentDisplayName('HBASE_REGIONSERVER')
        }));
      }
      if (this.isServiceSelected('STORM')) {
        headers.pushObject(Em.Object.create({
          name: 'SUPERVISOR',
          label: self.getComponentDisplayName('SUPERVISOR')
        }));
      }
      if (this.isServiceSelected('FLUME')) {
        headers.pushObject(Em.Object.create({
          name: 'FLUME_HANDLER',
          label: self.getComponentDisplayName('FLUME_HANDLER')
        }));
      }
      headers.pushObject(Ember.Object.create({
        name: 'CLIENT',
        label: self.getComponentDisplayName('CLIENT')
      }));
    }

    headers.forEach(function (header) {
      header.setProperties({ allChecked: false, noChecked: true });
    });

    this.get('headers').pushObjects(headers);

    this.render();
    if (this.get('isMasters')) {
      if (this.get('content.skipMasterStep')) {
        App.router.send('next');
      }
    }
    else {
      if (this.get('content.skipSlavesStep')) {
        App.router.send('next');
      }
    }
  },

  /**
   * Get active host names
   * @return {Array}
   */
  getHostNames: function () {
    var hostInfo = this.get('content.hosts');
    var hostNames = [];
    for (var index in hostInfo) {
      if (hostInfo[index].bootStatus === 'REGISTERED') {
        hostNames.push(hostInfo[index].name);
      }
    }
    return hostNames;
  },

  /**
   * Load all data needed for this module. Then it automatically renders in template
   */
  render: function () {
    var hostsObj = [];
    var masterHosts = [];
    var headers = this.get('headers');
    var masterHostNames = this.get('content.masterComponentHosts').mapProperty('hostName').uniq();

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

    if (this.get('isMasters')) {
      hostsObj = this.selectMasterComponents(hostsObj);
    } else {
      hostsObj = this.renderSlaves(hostsObj);
    }

    this.set('hosts', hostsObj);
    headers.forEach(function (header) {
      this.checkCallback(header.get('name'));
    }, this);
    this.set('isLoaded', true);
  },

  /**
   *
   * @param {Array} hostsObj
   * @return {Array}
   */
  renderSlaves: function (hostsObj) {
    var self = this;
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
          if (self.isServiceSelected("HDFS")) {
            var checkboxDatanode = checkboxes.findProperty('title', headers.findProperty('name', 'DATANODE').get('label'));
            if (checkboxDatanode && checkboxDatanode.get('checked')) {
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
    }
    else {
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
   * select checkboxes which correspond to master components
   *
   * @param {Array} hostsObj
   * @return {Array}
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
   * @param hostName
   * @return {*}
   */
  getMasterComponentsForHost: function (hostName) {
    return this.get('content.masterComponentHosts').filterProperty('hostName', hostName).mapProperty('component');
  },


  /**
   * Validate form. Return do we have errors or not
   * @return {Boolean}
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
        if(this.get('isAddServiceWizard')) {
          return this.validateEachComponent();
        }
      }
    }
  },

  /**
   * Validate all components for each host. Return do we have errors or not
   * @return {Boolean}
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
        isError |= checkboxes.findProperty('title', header.get('label')).checked;
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
   * @return {Boolean}
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
