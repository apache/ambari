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

  isAddHostWizard: function(){
    return this.get('content.controllerName') === 'addHostController';
  }.property('content.controllerName'),

  isAllDataNodes: function () {
    return this.get('hosts').everyProperty('isDataNode', true);
  }.property('hosts.@each.isDataNode'),

  isAllTaskTrackers: function () {
    return this.get('hosts').everyProperty('isTaskTracker', true);
  }.property('hosts.@each.isTaskTracker'),

  isAllRegionServers: function () {
    return this.get('hosts').everyProperty('isRegionServer', true);
  }.property('hosts.@each.isRegionServer'),

  isAllClients: function () {
    return this.get('hosts').everyProperty('isClient', true);
  }.property('hosts.@each.isClient'),

  isNoDataNodes: function () {
    return this.get('hosts').everyProperty('isDataNode', false);
  }.property('hosts.@each.isDataNode'),

  isNoTaskTrackers: function () {
    return this.get('hosts').everyProperty('isTaskTracker', false);
  }.property('hosts.@each.isTaskTracker'),

  isNoRegionServers: function () {
    return this.get('hosts').everyProperty('isRegionServer', false);
  }.property('hosts.@each.isRegionServer'),

  isNoClients: function () {
    return this.get('hosts').everyProperty('isClient', false);
  }.property('hosts.@each.isClient'),

  /**
   * Return whether Hbase service was selected or not.
   * Calculate this information on <code>content.services</code> variable
   * @return Boolean
   */
  isHbSelected: function () {
    return this.get('content.services').findProperty('serviceName', 'HBASE').get('isSelected');
  }.property('content.services'),

  /**
   * Return whether MapReduce service was selected or not.
   * Calculate this information on <code>content.services</code> variable
   * @return Boolean
   */
  isMrSelected: function () {
    return this.get('content.services').findProperty('serviceName', 'MAPREDUCE').get('isSelected');
  }.property('content.services'),

  clearError: function () {
    var isError = false;
    var hosts = this.get('hosts');
    if (this.get('isNoDataNodes') === false &&
      (this.get('isNoTaskTrackers') === false || this.get('isMrSelected') === false) &&
      (this.get('isNoRegionServers') === false || this.get('isHbSelected') === false) &&
      this.get('isNoClients') === false) {
      this.set('errorMessage', '');
    }
    if(this.get('isAddHostWizard')){
      for(var i = 0; i < hosts.length; i++){
        isError = !(hosts[i].get('isDataNode') || hosts[i].get('isClient')
          || ( this.get('isMrSelected') && hosts[i].get('isTaskTracker'))
          || ( this.get('isHbSelected') && hosts[i].get('isRegionServer')));
        if (isError) {
          break;
        } else {
          this.set('errorMessage', '');
        }
      }
    }
  }.observes('isNoDataNodes', 'isNoTaskTrackers', 'isNoRegionServers', 'isNoClients'),

  /**
   * Check whether current host is currently selected as master
   * @param hostName
   * @return {Boolean}
   */
  hasMasterComponents: function (hostName) {
    return this.get('content.masterComponentHosts').someProperty('hostName', hostName);
  },

  selectAllDataNodes: function () {
    var forFilter = this.get('hosts').filterProperty('isDataNodeInstalled', false);
    forFilter.setEach('isDataNode', true);
  },

  selectAllTaskTrackers: function () {
    var forFilter = this.get('hosts').filterProperty('isTaskTrackerInstalled', false);
    forFilter.setEach('isTaskTracker', true);
  },

  selectAllRegionServers: function () {
    var forFilter = this.get('hosts').filterProperty('isRegionServerInstalled', false);
    forFilter.setEach('isRegionServer', true);
  },

  selectAllClients: function () {
    var forFilter = this.get('hosts').filterProperty('isClientInstalled', false);
    forFilter.setEach('isClient', true);
  },

  deselectAllDataNodes: function () {
    var forFilter = this.get('hosts').filterProperty('isDataNodeInstalled', false);
    forFilter.setEach('isDataNode', false);
  },

  deselectAllTaskTrackers: function () {
    var forFilter = this.get('hosts').filterProperty('isTaskTrackerInstalled', false);
    forFilter.setEach('isTaskTracker', false);
  },

  deselectAllRegionServers: function () {
    var forFilter = this.get('hosts').filterProperty('isRegionServerInstalled', false);
    forFilter.setEach('isRegionServer', false);
  },

  deselectAllClients: function () {
    var forFilter = this.get('hosts').filterProperty('isClientInstalled', false);
    forFilter.setEach('isClient', false);
  },

  clearStep: function () {
    this.set('hosts', []);
    this.clearError();
  },

  loadStep: function () {
    console.log("WizardStep6Controller: Loading step6: Assign Slaves");
    this.clearStep();
    this.renderSlaveHosts();

    if(this.get('content.missSlavesStep')){
      App.router.send('next');
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
   * @return {Ember.Set}
   */
  renderSlaveHosts: function () {
    var hostsObj = Em.Set.create();
    var allHosts = this.getHostNames();
    // TODO - Hard coding should be removed.
    var maxNoofHostComponents = 11;
    var slaveComponents = this.get('content.slaveComponentHosts');

    allHosts.forEach(function (_hostName) {
      hostsObj.push(Em.Object.create({
        hostName: _hostName,
        isMaster: false,
        isDataNode: false,
        isTaskTracker: false,
        isRegionServer: false,
        isClient: false,
        isDataNodeInstalled: false,
        isTaskTrackerInstalled: false,
        isRegionServerInstalled: false,
        isClientInstalled: false
      }));
    });

    if (!slaveComponents) { // we are at this page for the first time
      if (allHosts.length > 3) {             //multiple nodes scenario
        hostsObj.forEach(function (host) {
          host.isMaster = this.hasMasterComponents(host.hostName);
          host.isDataNode = host.isTaskTracker
            = host.isRegionServer = !host.isMaster;
        }, this);

        if (hostsObj.someProperty('isDataNode', true)) {
          hostsObj.findProperty('isDataNode', true).set('isClient', true);
        }
      } else {
        var masterObj = {
          host: null,
          masterComponents: maxNoofHostComponents
        };
        hostsObj.forEach(function (host) {
          host.isMaster = this.hasMasterComponents(host.hostName);
          var countMasterComp = this.getMasterComponentsForHost(host.hostName).length;
          if (countMasterComp <= masterObj.masterComponents) {
            masterObj.masterComponents = countMasterComp;
            masterObj.host = host;
          }
        }, this);
        masterObj.host.set('isClient', true);
        masterObj.host.set('isDataNode', true);
        masterObj.host.set('isTaskTracker', true);
        masterObj.host.set('isRegionServer', true);

      }

    } else {

      var dataNodes = slaveComponents.findProperty('componentName', 'DATANODE');
      dataNodes.hosts.forEach(function (_dataNode) {
        var dataNode = hostsObj.findProperty('hostName', _dataNode.hostName);
        if (dataNode) {
          dataNode.set('isDataNode', true);
          dataNode.set('isDataNodeInstalled', _dataNode.isInstalled);
        }
      });

      if (this.get('isMrSelected')) {
        var taskTrackers = slaveComponents.findProperty('componentName', 'TASKTRACKER');
        taskTrackers.hosts.forEach(function (_taskTracker) {
          var taskTracker = hostsObj.findProperty('hostName', _taskTracker.hostName);
          if (taskTracker) {
            taskTracker.set('isTaskTracker', true);
            taskTracker.set('isTaskTrackerInstalled', _taskTracker.isInstalled);
          }
        });
      }

      if (this.get('isHbSelected')) {
        var regionServers = slaveComponents.findProperty('componentName', 'HBASE_REGIONSERVER');
        regionServers.hosts.forEach(function (_regionServer) {
          var regionServer = hostsObj.findProperty('hostName', _regionServer.hostName);
          if (regionServer) {
            regionServer.set('isRegionServer', true);
            regionServer.set('isRegionServerInstalled', _regionServer.isInstalled);
          }
        });
      }

      var clients = slaveComponents.findProperty('componentName', 'CLIENT');
      clients.hosts.forEach(function (_client) {
        var client = hostsObj.findProperty('hostName', _client.hostName);
        if (client) {
          client.set('isClient', true);
          client.set('isClientInstalled', _client.isInstalled);
        }
      }, this);

      allHosts.forEach(function (_hostname) {
        var host = hostsObj.findProperty('hostName', _hostname);
        if (host) {
          host.set('isMaster', this.hasMasterComponents(_hostname));
        }
      }, this);

    }

    hostsObj.forEach(function (host) {
      this.get('hosts').pushObject(host);
    }, this);
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
    var isError = false;
    var hosts = this.get('hosts');
    if(this.get('isAddHostWizard')){
      for(var i = 0; i < hosts.length; i++){
        isError = !(hosts[i].get('isDataNode') || hosts[i].get('isClient')
          || ( this.get('isMrSelected') && hosts[i].get('isTaskTracker'))
          || ( this.get('isHbSelected') && hosts[i].get('isRegionServer')));
        if (isError) {
          this.set('errorMessage', Ember.I18n.t('installer.step6.error.mustSelectOneForHost'));
          break;
        }
      }
    } else {
      isError = this.get('isNoDataNodes') || this.get('isNoClients')
        || ( this.get('isMrSelected') && this.get('isNoTaskTrackers'))
        || ( this.get('isHbSelected') && this.get('isNoRegionServers'));
      if (isError) {
        this.set('errorMessage', Ember.I18n.t('installer.step6.error.mustSelectOne'));
      }
    }

    return !isError;
  }

});
