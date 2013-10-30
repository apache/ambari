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
  componentName: DS.attr('string'),
  haStatus: DS.attr('string'),
  staleConfigs: DS.attr('boolean'),
  host: DS.belongsTo('App.Host'),
  service: DS.belongsTo('App.Service'),
  isClient:function () {
    if(['PIG', 'SQOOP', 'HCAT', 'MAPREDUCE2_CLIENT'].contains(this.get('componentName'))){
      return true;
    }

    return Boolean(this.get('componentName').match(/_client/gi));
  }.property('componentName'),
  isRunning: function(){
    return (this.get('workStatus') == 'STARTED' || this.get('workStatus') == 'STARTING');
  }.property('workStatus'),
  displayName: function () {
    return App.format.role(this.get('componentName'));
  }.property('componentName'),
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
      case 'RESOURCEMANAGER':
        return true;
      default:
        return false;
    }
  }.property('componentName'),
  isSlave: function(){
    switch (this.get('componentName')) {
      case 'DATANODE':
      case 'TASKTRACKER':
      case 'HBASE_REGIONSERVER':
      case 'GANGLIA_MONITOR':
      case 'NODEMANAGER':
      case 'ZKFC':
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
   */
  isDeletable: function() {
    var canDelete = false;
    switch (this.get('componentName')) {
      case 'DATANODE':
      case 'TASKTRACKER':
      case 'ZOOKEEPER_SERVER':
      case 'HBASE_REGIONSERVER':
      case 'GANGLIA_MONITOR':
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
   */
  componentTextStatus: function () {
    var value = this.get("workStatus");

    switch(value){
      case "INSTALLING":
        return 'Installing...';
      case "INSTALL_FAILED":
        return 'Install Failed';
      case "INSTALLED":
        return 'Stopped';
      case "STARTED":
        return 'Started';
      case "STARTING":
        return 'Starting...';
      case "STOPPING":
        return 'Stopping...';
      case "UNKNOWN":
        return 'Heartbeat lost...';
      case "UPGRADE_FAILED":
        return 'Upgrade Failed';
    }
    return 'Unknown';
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
  maintenance: "MAINTENANCE",
  unknown: "UNKNOWN",

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
      case this.maintenance:
        return 'maintenance';
      case this.unknown:
        return 'unknown';
    }
    return 'none';
  }
};

