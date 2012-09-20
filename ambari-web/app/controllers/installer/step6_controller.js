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
App.InstallerStep6Controller = Em.Controller.extend({

  hosts: [],
  // TODO: hook up with user host selection
  rawHosts: require('data/mock/hosts'),
  selectedServiceNames: null,
  masterComponentHosts: require('data/mock/master_component_hosts'),

  hasMasterComponents: function(hostname) {
    var hasMaster = false;
    this.get('masterComponentHosts').forEach(function(masterComponent) {
      if (masterComponent.hosts.contains(hostname)) {
        hasMaster = true;
      }
    });
    return hasMaster;
  },

  isAllDataNodes: function() {
    return this.get('hosts').everyProperty('isDataNode', true);
  }.property('hosts.@each.isDataNode'),

  isAllTaskTrackers: function() {
    return this.get('hosts').everyProperty('isTaskTracker', true);
  }.property('hosts.@each.isTaskTracker'),

  isAllRegionServers: function() {
    return this.get('hosts').everyProperty('isRegionServer', true);
  }.property('hosts.@each.isRegionServer'),

  isNoDataNodes: function() {
    return this.get('hosts').everyProperty('isDataNode', false);
  }.property('hosts.@each.isDataNode'),

  isNoTaskTrackers: function() {
    return this.get('hosts').everyProperty('isTaskTracker', false);
  }.property('hosts.@each.isTaskTracker'),

  isNoRegionServers: function() {
    return this.get('hosts').everyProperty('isRegionServer', false);
  }.property('hosts.@each.isRegionServer'),

  selectAllDataNodes: function() {
    this.get('hosts').setEach('isDataNode', true);
  },

  selectAllTaskTrackers: function() {
    this.get('hosts').setEach('isTaskTracker', true);
  },

  selectAllRegionServers: function() {
    this.get('hosts').setEach('isRegionServer', true);
  },

  deselectAllDataNodes: function() {
    this.get('hosts').setEach('isDataNode', false);
  },

  deselectAllTaskTrackers: function() {
    this.get('hosts').setEach('isTaskTracker', false);
  },

  deselectAllRegionServers: function() {
    this.get('hosts').setEach('isRegionServer', false);
  },

  init: function() {
    this._super();
    this.get('rawHosts').forEach(function(host) {
      host.isDataNode = host.isTaskTracker = host.isRegionServer = !this.hasMasterComponents(host.hostname);
      this.get('hosts').pushObject(Ember.Object.create(host));
    }, this);
  },

  validate: function() {
    return !(this.get('isNoDataNodes') || this.get('isNoTaskTrackers') || this.get('isNoRegionServers'));
  },

  submit: function() {
    if (!this.validate()) {
      this.set('errorMessage', Ember.I18n.t('installer.step6.error.mustSelectOne'));
      return;
    }
    App.db.setHostSlaveComponents(this.get('host'));

    var dataNodeHosts = [];
    var taskTrackerHosts = [];
    var regionServerHosts = [];

    this.get('hosts').forEach(function (host) {
      if (host.get('isDataNode')) {
        dataNodeHosts.push({
          hostname: host.hostname,
          group: 'Default'
        });
      }
      if (host.get('isTaskTracker')) {
        taskTrackerHosts.push({
          hostname: host.hostname,
          group: 'Default'
        });
      }
      if (host.get('isRegionServer')) {
        regionServerHosts.push({
          hostname: host.hostname,
          group: 'Default'
        });
      }
    });

    var slaveComponentHosts = [];
    slaveComponentHosts.push({
      componentName: 'DataNode',
      hosts: dataNodeHosts
    });
    slaveComponentHosts.push({
      componentName: 'TaskTracker',
      hosts: taskTrackerHosts
    });
    slaveComponentHosts.push({
      componentName: 'RegionServer',
      hosts: regionServerHosts
    });

    App.db.setSlaveComponentHosts(slaveComponentHosts);

    App.router.transitionTo('step7');

  }
});