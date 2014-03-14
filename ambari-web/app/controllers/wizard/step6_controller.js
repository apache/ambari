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
var lazyloading = require('utils/lazy_loading');

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
   * number of checkboxes for detecting time point when they are completely inserted into the view
   */
  checkboxesCount: 0,
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

  clearError: function () {
    var self = this;
    var isError = false;
    var err = true;
    var hosts = this.get('hosts');
    var headers = this.get('headers');
    headers.forEach(function (header) {
      var all_false = true;
      hosts.forEach(function (host) {
        var checkboxes = host.get('checkboxes');
        all_false &= !checkboxes.findProperty('title', header.get('label')).checked;
      });
      err &= all_false;
    });

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

  /**
   * Check whether current host is currently selected as master
   * @param hostName
   * @return {Boolean}
   */
  hasMasterComponents: function (hostName) {
    return this.get('content.masterComponentHosts').someProperty('hostName', hostName);
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
      host.get('checkboxes').filterProperty('isInstalled', false).forEach(function (cb) {
        if (cb.get('component') === component) {
          allTrue &= cb.get('checked');
          allFalse &= !cb.get('checked');
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
    this.get('hosts').sort(function(a, b){return a.isMaster < b.isMaster;});
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
    var allHosts = this.getHostNames();

    var self = this;
    allHosts.forEach(function (_hostName) {

      var obj = Em.Object.create({
        hostName: _hostName,
        isMaster: false,
        checkboxes: []
      });

      self.get('headers').forEach(function (header) {
        obj.checkboxes.pushObject(Em.Object.create({
          component: header.name,
          title: header.label,
          checked: false,
          isInstalled: false,
          id: header.name + "_DELIMITER_" + _hostName
        }));
      });

      hostsObj.push(obj);
    });
    this.set('checkboxesCount', (allHosts.length * self.get('headers').length));

    if (this.get('isMasters')) {
      hostsObj = this.renderMasters(hostsObj);
    }
    else {
      hostsObj = this.renderSlaves(hostsObj);
    }

    if(hostsObj.length > 100) {
      lazyloading.run({
        destination: this.get('hosts'),
        source: hostsObj,
        context: this,
        initSize: 50,
        chunkSize: 100,
        delay: 50
      });
    } else {
      hostsObj.forEach(function (host) {
        this.get('hosts').pushObject(host);
      }, this);
      this.set('isLoaded', true);
    }
    this.get('headers').forEach(function (header) {
      self.checkCallback(header.get('name'));
    });
  },

  /**
   *
   * @param hostsObj
   * @return {*}
   */
  renderSlaves: function (hostsObj) {
    var self = this;
    var allHosts = this.getHostNames();
    var headers = this.get('headers');
    var slaveComponents = this.get('content.slaveComponentHosts');
    if (!slaveComponents) { // we are at this page for the first time
      var client_is_set = false;
      hostsObj.forEach(function (host) {
        host.isMaster = self.hasMasterComponents(host.hostName);
        var checkboxes = host.get('checkboxes');
        checkboxes.setEach('checked', !host.isMaster);
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

      if (this.get('isInstallerWizard') && hostsObj.everyProperty('isMaster', true)) {
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
      allHosts.forEach(function (_hostname) {
        var host = hostsObj.findProperty('hostName', _hostname);
        if (host) {
          host.set('isMaster', this.hasMasterComponents(_hostname));
        }
      }, this);
    }
    return hostsObj;
  },

  /**
   *
   * @param hostsObj
   * @return {*}
   */
  renderMasters: function (hostsObj) {
    var self = this;
    var masterComponentHosts = this.get('content.masterComponentHosts');
    console.warn('masterComponentHosts', masterComponentHosts);

    if (masterComponentHosts) {
      masterComponentHosts.forEach(function (item) {
        var host = hostsObj.findProperty('hostName', item.hostName);
        if (host) {
          var checkbox = host.get('checkboxes').findProperty('title', item.display_name);
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
