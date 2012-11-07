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
 *   hostSlaveComponents: App.db.hostSlaveComponents (hosts-to-slave-components mapping the user selected in Steo 6)
 *   slaveComponentHosts: App.db.slaveComponentHosts (slave-components-to-hosts mapping the user selected in Step 6)
 *
 */
App.WizardStep6Controller = Em.Controller.extend({

  hosts: [],

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
  }.property('content'),

  /**
   * Return whether MapReduce service was selected or not.
   * Calculate this information on <code>content.services</code> variable
   * @return Boolean
   */
	isMrSelected: function () {
    return this.get('content.services').findProperty('serviceName', 'MAPREDUCE').get('isSelected');
	}.property('content'),

  clearError: function () {
    if (this.get('isNoDataNodes') === false &&
        (this.get('isNoTaskTrackers') === false || this.get('isMrSelected') === false) &&
        (this.get('isNoRegionServers') === false || this.get('isHbSelected') === false) &&
        this.get('isNoClients') === false) {
      this.set('errorMessage', '');
    }
  }.observes('isNoDataNodes', 'isNoTaskTrackers', 'isNoRegionServers', 'isNoClients'),

  /**
   * Check whether current host is currently selected as master
   * @param hostname
   * @return {Boolean}
   */
  hasMasterComponents: function (hostname) {
    return this.get('content.masterComponentHosts').someProperty('hostName', hostname);
  },

  selectAllDataNodes: function () {
    this.get('hosts').setEach('isDataNode', true);
  },

  selectAllTaskTrackers: function () {
    this.get('hosts').setEach('isTaskTracker', true);
  },

  selectAllRegionServers: function () {
    this.get('hosts').setEach('isRegionServer', true);
  },

  selectAllClients: function () {
    this.get('hosts').setEach('isClient', true);
  },

  deselectAllDataNodes: function () {
    this.get('hosts').setEach('isDataNode', false);
  },

  deselectAllTaskTrackers: function () {
    this.get('hosts').setEach('isTaskTracker', false);
  },

  deselectAllRegionServers: function () {
    this.get('hosts').setEach('isRegionServer', false);
  },

  deselectAllClients: function () {
    this.get('hosts').setEach('isClient', false);
  },

  clearStep: function () {
    this.set('hosts', []);
    this.clearError();
  },

  loadStep: function () {
    console.log("WizardStep6Controller: Loading step6: Assign Slaves");
    this.clearStep();
    this.renderSlaveHosts();
  },

  /**
   * Get active host names
   * @return {Array}
   */
  getHostNames: function () {
    var hostInfo = this.get('content.hostsInfo');
    var hostNames = [];
    for (var index in hostInfo) {
      if (hostInfo[index].bootStatus === 'success' || true) //TODO: remove true after integration with bootstrap
        hostNames.push(hostInfo[index].name);
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
    var slaveHosts = this.get('content.slaveComponentHosts');

    allHosts.forEach(function (_hostName) {
      hostsObj.push(Em.Object.create({
        hostname: _hostName,
        isMaster: false,
        isDataNode: false,
        isTaskTracker: false,
        isRegionServer: false,
        isClient: false
      }));
    });

    if (!slaveHosts) { // we are at this page for the first time

      hostsObj.forEach(function (host) {
        host.isMaster = this.hasMasterComponents(host.hostname);
        host.isDataNode = host.isTaskTracker
            = host.isRegionServer = !host.isMaster;
      }, this);

      if (hostsObj.someProperty('isDataNode', true)) {
        hostsObj.findProperty('isDataNode', true).set('isClient', true);
      }

    } else {

			var dataNodes = slaveHosts.findProperty('componentName', 'DATANODE');
      dataNodes.hosts.forEach(function (_dataNode) {
        var dataNode = hostsObj.findProperty('hostname', _dataNode.hostname);
        if (dataNode) {
          dataNode.set('isDataNode', true);
        }
      });

      if(this.get('isMrSelected')) {
        var taskTrackers = slaveHosts.findProperty('componentName', 'TASKTRACKER');
        taskTrackers.hosts.forEach(function (_taskTracker) {
          var taskTracker = hostsObj.findProperty('hostname', _taskTracker.hostname);
          if (taskTracker) {
            taskTracker.set('isTaskTracker', true);
          }
        });
      }

      if (this.get('isHbSelected')) {
				var regionServers = slaveHosts.findProperty('componentName', 'HBASE_REGIONSERVER');
        regionServers.hosts.forEach(function (_regionServer) {
          var regionServer = hostsObj.findProperty('hostname', _regionServer.hostname);
          if (regionServer) {
            regionServer.set('isRegionServer', true);
          }
        });
      }

      var clients = slaveHosts.findProperty('componentName', 'CLIENT');
      clients.hosts.forEach(function (_client) {
        var client = hostsObj.findProperty('hostname', _client.hostname);
        if (client) {
          client.set('isClient', true);
        }
      }, this);

      allHosts.forEach(function (_hostname) {
        var host = hostsObj.findProperty('hostname', _hostname);
        if (host) {
          host.set('isMaster', this.hasMasterComponents(_hostname));
        }
      }, this);

    }

    hostsObj.forEach(function(host){
      this.get('hosts').pushObject(host);
    }, this);
  },

  /**
   * Return list of master components for specified <code>hostname</code>
   * @param hostname
   * @return {*}
   */
  getMasterComponentsForHost: function (hostname) {
    var hostInfo = this.get('content.hostToMasterComponent').findProperty('hostname', hostname);
    if(hostInfo){
      return hostInfo.components;
    }

    return false;
  },


  /**
   * Validate form. Return do we have errors or not
   * @return {Boolean}
   */
  validate: function () {
    var isError =  this.get('isNoDataNodes') || this.get('isNoClients')
      || ( this.get('isMrSelected') && this.get('isNoTaskTrackers'))
      || ( this.get('isHbSelected') &&this.get('isNoRegionServers'));

    if(isError){
      this.set('errorMessage', Ember.I18n.t('installer.step6.error.mustSelectOne'));
    }
    return !isError;
  }

});
