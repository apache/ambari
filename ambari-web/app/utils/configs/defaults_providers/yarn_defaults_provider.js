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
require('utils/configs/defaults_providers/defaultsProvider');

App.YARNDefaultsProvider = App.DefaultsProvider.extend({

  slaveHostDependency: ['NODEMANAGER'],

  /**
   * List of the configs that should be calculated
   * @type {Object}
   */
  configsTemplate: {
    'yarn.nodemanager.resource.memory-mb': null,
    'yarn.scheduler.minimum-allocation-mb': null,
    'yarn.scheduler.maximum-allocation-mb': null,
    'mapreduce.map.memory.mb': null,
    'mapreduce.reduce.memory.mb': null,
    'mapreduce.map.java.opts': null,
    'mapreduce.reduce.java.opts': null,
    'mapreduce.task.io.sort.mb': null,
    'yarn.app.mapreduce.am.resource.mb': null,
    'yarn.app.mapreduce.am.command-opts': null
  },

  /**
   * Information about ram, disk count, cpu count and hbase availability
   * @type {{disk: number, ram: number, cpu: number, hBaseInstalled: bool}}
   */
  clusterData: null,

  /**
   * Reserved for system memory
   * @type {number}
   */
  reservedRam: null,

  /**
   * Reserved for HBase memory
   * @type {number}
   */
  hBaseRam: null,

  GB: 1024,

  /**
   *  Minimum container size (in RAM).
   *  This value is dependent on the amount of RAM available, as in smaller memory nodes the minimum container size should also be smaller
   *
   *  Value in MB!
   *
   *  @type {number}
   */
  recommendedMinimumContainerSize: function () {
    if (!this.clusterDataIsValid()) return null;
    var ram = this.get('clusterData.ram');
    switch(true) {
      case (ram <= 4): return 256;
      case (ram > 4 && ram <= 8): return 512;
      case (ram > 8 && ram <= 24): return 1024;
      case (ram > 24):
      default: return 2048;
    }
  }.property('clusterData.ram'),

  /**
   * Maximum number of containers allowed per node
   * max(3, min (2*cores,min (1.8*DISKS,(Total available RAM) / MIN_CONTAINER_SIZE))))
   * @type {number}
   */
  containers: function () {
    if (!this.clusterDataIsValid()) return null;
    var cpu = this.get('clusterData.cpu');
    var disk = this.get('clusterData.disk');
    var ram = this.get('clusterData.ram');
    var containerSize = this.get('recommendedMinimumContainerSize');
    cpu *= 2;
    disk = Math.ceil(disk * 1.8);
    ram -= this.get('reservedRam');
    if (this.get('clusterData.hBaseInstalled')) {
      ram -= this.get('hBaseRam');
    }
    // On low memory systems, memory left over after
    // removing reserved-RAM and HBase might be
    // less than 2GB (even negative). If so, we force
    // a 2GB value relying on virtual memory.
    if (ram < 2) {
      ram = 2;
    }
    ram *= this.get('GB');
    ram /= containerSize;
    return Math.round(Math.max(3, Math.min(cpu, Math.min(disk, ram))));
  }.property('clusterData.cpu', 'clusterData.ram', 'clusterData.hBaseInstalled', 'clusterData.disk', 'reservedRam', 'hBaseRam', 'recommendedMinimumContainerSize'),

  /**
   * Amount of RAM per container.
   * Calculated to be max(2GB, RAM - reservedRam - hBaseRam) / containers
   *
   * @return {number} Memory per container in MB. If greater than 1GB,
   *          value will be in multiples of 512. 
   */
  ramPerContainer: function () {
    var containers = this.get('containers');
    if (!containers) {
      return null;
    }
    var ram = this.get('clusterData.ram');
    ram = (ram - this.get('reservedRam'));
    if (this.get('clusterData.hBaseInstalled')) {
      ram -= this.get('hBaseRam');
    }
    // On low memory systems, memory left over after
    // removing reserved-RAM and HBase might be
    // less than 2GB (even negative). If so, we force
    // a 2GB value relying on virtual memory.
    if (ram < 2) {
      ram = 2;
    }
    ram *= this.get('GB');
    var container_ram = Math.abs(ram / containers);
    // If container memory is greater than 1GB, 
    // we use multiples of 512 as value
    return container_ram > this.get('GB') ? (Math.floor(container_ram / 512) * 512) : container_ram;
  }.property('containers', 'clusterData.ram', 'clusterData.hBaseInstalled', 'hBaseRam', 'reservedRam'),

  /**
   * Memory for Map
   * @type {number}
   */
  mapMemory: function () {
    return Math.floor(this.get('ramPerContainer'));
  }.property('ramPerContainer'),

  /**
   * Memory for Reduce
   * @type {number}
   */
  reduceMemory: function () {
    return this.get('ramPerContainer');
  }.property('ramPerContainer'),

  /**
   * @type {number}
   */
  amMemory: function () {
    return Math.max(this.get('mapMemory'), this.get('reduceMemory'));
  }.property('mapMemory', 'reduceMemory'),

  /**
   * Reserved for HBase and system memory is based on total available memory
   * @type {number}
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
    var ram = this.get('clusterData.ram');
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
    this.set('reservedRam', table[index].os);
    this.set('hBaseRam', table[index].hbase);
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
  getDefaults: function (localDB) {
    this._super();
    this.getClusterData(localDB);
    var configs = {};
    jQuery.extend(configs, this.get('configsTemplate'));
    if (!this.clusterDataIsValid()) {
      return configs;
    }
    configs['yarn.nodemanager.resource.memory-mb'] = Math.round(this.get('containers') * this.get('ramPerContainer'));
    configs['yarn.scheduler.minimum-allocation-mb'] = Math.floor(this.get('ramPerContainer'));
    configs['yarn.scheduler.maximum-allocation-mb'] = Math.round(this.get('containers') * this.get('ramPerContainer'));
    configs['yarn.app.mapreduce.am.resource.mb'] = Math.floor(this.get('amMemory'));
    configs['yarn.app.mapreduce.am.command-opts'] = "-Xmx" + Math.round(0.8 * this.get('amMemory')) + "m";
    configs['mapreduce.map.memory.mb'] = Math.floor(this.get('mapMemory'));
    configs['mapreduce.reduce.memory.mb'] = Math.floor(this.get('reduceMemory'));
    configs['mapreduce.map.java.opts'] = "-Xmx" + Math.round(0.8 * this.get('mapMemory')) + "m";
    configs['mapreduce.reduce.java.opts'] = "-Xmx" + Math.round(0.8 * this.get('reduceMemory')) + "m";
    configs['mapreduce.task.io.sort.mb'] = Math.round(Math.min(0.4 * this.get('mapMemory'), 1024));
    return configs;
  },

  /**
   * Calculate needed cluster data (like disk count, cpu count, ram (in MB!) and hbase availability)
   * @param {object} localDB Object with information about hosts and master/slave components
   */
  getClusterData: function (localDB) {
    this._super();
    var components = this.get('slaveHostDependency').concat(this.get('masterHostDependency'));
    var hosts = [];
    if (!localDB.hosts || !(localDB.masterComponentHosts || localDB.slaveComponentHosts)) return;
    var hBaseInstalled = !!localDB.masterComponentHosts.filterProperty('component', 'HBASE_MASTER').length;
    components.forEach(function (component) {
      var mc = localDB.masterComponentHosts.findProperty('component', component);
      if (mc) {
        if (!hosts.contains(mc.hostName)) {
          hosts.push(mc.hostName);
        }
      }
      else {
        var sc = localDB.slaveComponentHosts.findProperty('componentName', component);
        if (sc) {
          sc.hosts.map(function (host) {
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
    var host = hosts[0] && localDB.hosts[hosts[0]];
    if (host) {
      clusterData.cpu = parseInt(host.cpu);
      var length = 0;
      host.disk_info.forEach(function(disk) {
        //invalid mountpoints
        if (!(disk.mountpoint.startsWith('/home/') || disk.mountpoint.startsWith('/homes/') || 
        		disk.mountpoint.startsWith('/dev/') || disk.mountpoint.startsWith('/tmp/'))) {
          length++;
        }
      },this);
      clusterData.disk = length;
      clusterData.ram = Math.round(parseFloat(host.memory) / (1024 * 1024));
    }
    this.set('clusterData', clusterData);
  },

  /**
   * Verify <code>clusterData</code> - check if all properties are defined
   * @return {bool}
   */
  clusterDataIsValid: function () {
    if (!this.get('clusterData')) return false;
    return !(this.get('clusterData.ram') == null || this.get('clusterData.cpu') == null || this.get('clusterData.disk') == null || this.get('clusterData.hBaseInstalled') == null);
  }

});
