/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/defaults_providers/defaultsProvider');

App.YARNDefaultsProvider = App.DefaultsProvider.create({

  /**
   * List of the configs that should be calculated
   */
  configsTemplate: {
    'yarn.nodemanager.resource.memory-mb': null,
    'yarn.scheduler.minimum-allocation-mb': null,
    'yarn.scheduler.maximum-allocation-mb': null,
    'mapreduce.map.memory.mb': null,
    'mapreduce.reduce.memory.mb': null,
    'mapreduce.map.java.opts': null,
    'mapreduce.reduce.java.opts': null,
    'yarn.app.mapreduce.am.resource.mb': null,
    'yarn.app.mapreduce.am.command-opts': null
  },

  /**
   * Information about ram, disk count, cpu count and hbase availability
   * Example:
   * {
   *   disk: 12,
   *   ram: 48 * 1024, // MB
   *   cpu: 12,
   *   hBaseInstalled: false
   * }
   */
  clusterData: null,

  /**
   * Reserved for system memory
   *
   * Value in MB!
   */
  reservedRam: null,

  /**
   * Reserved for HBase memory
   *
   * Value in MB!
   */
  hBaseRam: null,

  /**
   *  Minimum container size (in RAM).
   *  This value is dependent on the amount of RAM available, as in smaller memory nodes the minimum container size should also be smaller
   *
   *  Value in MB!
   */
  recommendedMinimumContainerSize: function() {
    if (!this.clusterDataIsValid()) return null;
    var ram = this.get('clusterData.ram');
    switch(true) {
      case (ram < 4*1024): return 256;
      case (ram >= 4*1024 && ram < 8*1024): return 512;
      case (ram >= 8*1024 && ram < 24*1024): return 1024;
      case (ram >= 24*1024):
      default: return 2048;
    }
  }.property('clusterData.ram'),

  /**
   * Maximum number of containers allowed per node
   * min (2*CORES, 1.8*DISKS, (Total available RAM) / MIN_CONTAINER_SIZE)
   */
  containers: function() {
    if (!this.clusterDataIsValid()) return null;
    var cpu = this.get('clusterData.cpu');
    var disk = this.get('clusterData.disk');
    var ram = this.get('clusterData.ram');
    var containerSize = this.get('recommendedMinimumContainerSize');
    cpu *= 2;
    disk *= 1.8;
    ram = (ram - this.get('reservedRam'));
    if (this.get('clusterData.hBaseInstalled')) {
      ram -= this.get('hBaseRam')
    }
    ram /= containerSize;
    if (cpu < disk) {
      if (cpu < ram) {
        return cpu;
      }
      return ram;
    }
    else {
      if (disk < ram) {
        return parseInt(disk);
      }
      return ram;
    }
  }.property('clusterData.cpu', 'clusterData.ram', 'clusterData.disk', 'recommendedMinimumContainerSize'),

  /**
   * amount of RAM per container
   * RAM-per-container = max(MIN_CONTAINER_SIZE, (Total Available RAM) / containers))
   *
   * Value in MB!
   */
  ramPerContainer: function() {
    var containerSize = this.get('recommendedMinimumContainerSize');
    var containers = this.get('containers');
    if (!containerSize || !containers) {
      return null;
    }
    var s = this.get('clusterData.ram') - this.get('reservedRam');
    if (this.get('clusterData.hBaseInstalled')) {
      s -= this.get('hBaseRam');
    }
    s /= containers;
    return (containerSize > s) ? containerSize : s;
  }.property('recommendedMinimumContainerSize', 'containers'),

  /**
   * Reserved for HBase and system memory is based on total available memory
   */
  reservedMemoryRecommendations: function() {
    var table = [
      {os:1,hbase:1},
      {os:2,hbase:1},
      {os:2,hbase:2},
      {os:4,hbase:4},
      {os:6,hbase:8},
      {os:8,hbase:8},
      {os:8,hbase:8},
      {os:12,hbase:16},
      {os:24,hbase:24},
      {os:32,hbase:32},
      {os:64,hbase:64}
    ];
    var ram = this.get('clusterData.ram') / 1024;
    var index = 0;
    switch (true) {
      case (ram <= 4): index = 0; break;
      case (ram > 4 && ram <= 8): index = 1; break;
      case (ram > 8 && ram <= 16): index = 2; break;
      case (ram > 16 && ram <= 24): index = 3; break;
      case (ram > 24 && ram <= 48): index = 4; break;
      case (ram > 48 && ram <= 64): index = 5; break;
      case (ram > 64 && ram <= 72): index = 6; break;
      case (ram > 72 && ram <= 96): index = 7; break;
      case (ram > 96 && ram <= 128): index = 8; break;
      case (ram > 128 && ram <= 256): index = 9; break;
      case (ram > 256 && ram <= 512): index = 10; break;
      default: index = 10; break;
    }
    this.set('reservedRam', table[index].os * 1024);
    this.set('hBaseRam', table[index].hbase * 1024);
  }.observes('clusterData.ram'),

  /**
   * Provide an object where keys are property-names and values are the recommended defaults
   * @param {object} localDB Object with information about hosts and master/slave components
   * Example:
   *  <code>
   *    {
   *       "hosts": {
   *           "host1": {
   *               "name": "host1",
   *               "cpu": 1,
   *               "memory": "6123683.00",
   *               "disk_info": [{
   *                   ....
   *               },...]
   *           },...
   *       },
   *       "masterComponentHosts": [{
   *           "component": "NAMENODE",
   *           "hostName": "host1",
   *           "serviceId": "HDFS"
   *       },...],
   *       "slaveComponentHosts": [{
   *           "componentName": "DATANODE",
   *           "hosts": [{
   *               "hostName": "host2"
   *           }]
   *       },...]
   *   }
   *  </code>
   * @return {object}
   */
  getDefaults: function(localDB) {
    this._super();
    this.getClusterData(localDB);
    var configs = {};
    jQuery.extend(configs, this.get('configsTemplate'));
    configs['yarn.nodemanager.resource.memory-mb'] = this.get('containers') * this.get('ramPerContainer');
    configs['yarn.scheduler.minimum-allocation-mb'] = this.get('ramPerContainer');
    configs['yarn.scheduler.maximum-allocation-mb'] = this.get('containers') * this.get('ramPerContainer');
    configs['mapreduce.map.memory.mb'] = this.get('ramPerContainer');
    configs['mapreduce.reduce.memory.mb'] = 2 * this.get('ramPerContainer');
    configs['mapreduce.map.java.opts'] = Math.round(0.8 * this.get('ramPerContainer'));
    configs['mapreduce.reduce.java.opts'] = Math.round(0.8 *2 * this.get('ramPerContainer'));
    configs['yarn.app.mapreduce.am.resource.mb'] = 2 * this.get('ramPerContainer');
    configs['yarn.app.mapreduce.am.command-opts'] = Math.round(0.8 * 2 * this.get('ramPerContainer'));
    return configs;
  },

  /**
   * Calculate needed cluster data (like disk count, cpu count, ram (in MB!) and hbase availability)
   * @param {object} localDB Object with information about hosts and master/slave components
   */
  getClusterData: function(localDB) {
    this._super();
    var components = ['RESOURCEMANAGER', 'NODEMANAGER'];
    var hosts = [];
    if (!localDB.hosts || !(localDB.masterComponentHosts || localDB.slaveComponentHosts)) return;
    var hBaseInstalled = !!localDB.masterComponentHosts.filterProperty('component', 'HBASE_MASTER').length;
    components.forEach(function(component) {
      var mc = localDB.masterComponentHosts.findProperty('component', component);
      if (mc) {
        if (!hosts.contains(mc.hostName)) {
          hosts.push(mc.hostName);
        }
      }
      else {
        var sc = localDB.slaveComponentHosts.findProperty('componentName', component);
        if (sc) {
          sc.hosts.map(function(host) {
            if (!hosts.contains(host.hostName)) {
              hosts.push(host.hostName);
            }
          });
        }
      }
    });
    var clusterData = {
      cpu: 0,
      disk: 0,
      ram: 0,
      hBaseInstalled: hBaseInstalled
    };
    hosts.forEach(function(hostName) {
      var host = localDB.hosts[hostName];
      if (host) {
        clusterData.cpu += parseInt(host.cpu);
        clusterData.disk += host.disk_info.length;
        clusterData.ram += Math.round(parseFloat(host.memory) / 1024);
      }
    });
    this.set('clusterData', clusterData);
  },

  /**
   * Verify <code>clusterData</code> - check if all properties are defined
   */
  clusterDataIsValid: function() {
    if (!this.get('clusterData')) return false;
    if (this.get('clusterData.ram') == null || this.get('clusterData.cpu') == null || this.get('clusterData.disk') == null  || this.get('clusterData.hBaseInstalled') == null) return false;
    return true;
  }

});
