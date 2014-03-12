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

App.HostComponent = DS.Model.extend({
  workStatus: DS.attr('string'),
  passiveState: DS.attr('string'),
  componentName: DS.attr('string'),
  haStatus: DS.attr('string'),
  displayNameAdvanced: DS.attr('string'),
  staleConfigs: DS.attr('boolean'),
  host: DS.belongsTo('App.Host'),
  service: DS.belongsTo('App.Service'),
  /**
   * Determine if component is client
   * @returns {bool}
   */
  isClient:function () {
    if(['PIG', 'SQOOP', 'HCAT', 'MAPREDUCE2_CLIENT'].contains(this.get('componentName'))){
      return true;
    }

    return Boolean(this.get('componentName').match(/_client/gi));
  }.property('componentName'),
  /**
   * Determine if component is running now
   * Based on <code>workStatus</code>
   * @returns {bool}
   */
  isRunning: function(){
    return (this.get('workStatus') == 'STARTED' || this.get('workStatus') == 'STARTING');
  }.property('workStatus'),

  /**
   * Formatted <code>componentName</code>
   * @returns {String}
   */
  displayName: function () {
    return App.format.role(this.get('componentName'));
  }.property('componentName'),

  /**
   * Determine if component is master
   * @returns {bool}
   */
  isMaster: function () {
    switch (this.get('componentName')) {
      case 'NAMENODE':
      case 'SECONDARY_NAMENODE':
      case 'SNAMENODE':
      case 'JOURNALNODE':
      case 'JOBTRACKER':
      case 'ZOOKEEPER_SERVER':
      case 'HIVE_SERVER':
      case 'HIVE_METASTORE':
      case 'MYSQL_SERVER':
      case 'HBASE_MASTER':
      case 'NAGIOS_SERVER':
      case 'GANGLIA_SERVER':
      case 'OOZIE_SERVER':
      case 'WEBHCAT_SERVER':
      case 'HUE_SERVER':
      case 'HISTORYSERVER':
      case 'FLUME_SERVER':
      case 'FALCON_SERVER':
      case 'NIMBUS':
      case 'STORM_UI_SERVER':
      case 'LOGVIEWER_SERVER':
      case 'DRPC_SERVER':
      case 'STORM_REST_API':
      case 'RESOURCEMANAGER':
      case 'APP_TIMELINE_SERVER':
        return true;
      default:
        return false;
    }
  }.property('componentName'),

  /**
   * Determine if component is slave
   * @returns {bool}
   */
  isSlave: function(){
    switch (this.get('componentName')) {
      case 'DATANODE':
      case 'TASKTRACKER':
      case 'HBASE_REGIONSERVER':
      case 'GANGLIA_MONITOR':
      case 'NODEMANAGER':
      case 'ZKFC':
      case 'SUPERVISOR':
        return true;
      default:
        return false;
    }
  }.property('componentName'),
  /**
   * Only certain components can be deleted.
   * They include some from master components, 
   * some from slave components, and rest from 
   * client components.
   * @returns {bool}
   */
  isDeletable: function() {
    var canDelete = false;
    switch (this.get('componentName')) {
      case 'DATANODE':
      case 'TASKTRACKER':
      case 'ZOOKEEPER_SERVER':
      case 'HBASE_REGIONSERVER':
      case 'GANGLIA_MONITOR':
      case 'SUPERVISOR':
      case 'NODEMANAGER':
        canDelete = true;
        break;
      default:
    }
    if (!canDelete) {
      canDelete = this.get('isClient');
    }
    return canDelete;
  }.property('componentName', 'isClient'),
  /**
   * A host-component is decommissioning when it is in HDFS service's list of
   * decomNodes.
   * @returns {bool}
   */
  isDecommissioning: function () {
    var decommissioning = false;
    var hostName = this.get('host.hostName');
    var componentName = this.get('componentName');
    var hdfsSvc = App.HDFSService.find().objectAt(0);
    if (componentName === 'DATANODE' && hdfsSvc) {
      var decomNodes = hdfsSvc.get('decommissionDataNodes');
      var decomNode = decomNodes != null ? decomNodes.findProperty("hostName", hostName) : null;
      decommissioning = decomNode != null;
    }
    return decommissioning;
  }.property('componentName', 'host.hostName', 'App.router.clusterController.isLoaded', 'App.router.updateController.isUpdated'),
  /**
   * User friendly host component status
   * @returns {String}
   */
  isActive: function() {
    return (this.get('passiveState') == 'OFF');
  }.property('passiveState'),

  passiveTooltip: function() {
    if (!this.get('isActive')) {
      return Em.I18n.t('hosts.component.passive.mode');
    }
  }.property('isActive'),

  statusClass: function() {
    return this.get('isActive') ? this.get('workStatus') : 'icon-medkit';
  }.property('workStatus','isActive'),

  componentTextStatus: function () {
    return App.HostComponentStatus.getTextStatus(this.get("workStatus"));
  }.property('workStatus','isDecommissioning')
});

App.HostComponent.FIXTURES = [];

App.HostComponentStatus = {
  started: "STARTED",
  starting: "STARTING",
  stopped: "INSTALLED",
  stopping: "STOPPING",
  install_failed: "INSTALL_FAILED",
  installing: "INSTALLING",
  upgrade_failed: "UPGRADE_FAILED",
  unknown: "UNKNOWN",
  disabled: "DISABLED",
  init: "INIT",

  /**
   * Get host component status in "machine" format
   * @param {String} value
   * @returns {String}
   */
  getKeyName:function(value){
    switch(value){
      case this.started:
        return 'started';
      case this.starting:
        return 'starting';
      case this.stopped:
        return 'installed';
      case this.stopping:
        return 'stopping';
      case this.install_failed:
        return 'install_failed';
      case this.installing:
        return 'installing';
      case this.upgrade_failed:
        return 'upgrade_failed';
      case this.disabled:
      case this.unknown:
        return 'unknown';
    }
    return 'unknown';
  },

  /**
   * Get user-friendly host component status
   * @param {String} value
   * @returns {String}
   */
  getTextStatus: function (value) {
    switch (value) {
      case this.installing:
        return 'Installing...';
      case this.install_failed:
        return 'Install Failed';
      case this.stopped:
        return 'Stopped';
      case this.started:
        return 'Started';
      case this.starting:
        return 'Starting...';
      case this.stopping:
        return 'Stopping...';
      case this.unknown:
        return 'Heartbeat lost...';
      case this.upgrade_failed:
        return 'Upgrade Failed';
      case this.disabled:
        return 'Disabled';
      case this.init:
        return 'Install Pending...';
    }
    return 'Unknown';
  },

  /**
   * Get list of possible <code>App.HostComponent</code> statuses
   * @returns {String[]}
   */
  getStatusesList: function() {
    var ret = [];
    for (var st in this) {
      if (this.hasOwnProperty(st) && Em.typeOf(this[st]) == 'string') {
        ret.push(this[st]);
      }
    }
    return ret;
  }
};

