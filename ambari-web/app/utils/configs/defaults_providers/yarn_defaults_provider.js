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
    'mapreduce.task.io.sort.mb': null,
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

  GB: 1024,
  /**
   *  Minimum container size (in RAM).
   *  This value is dependent on the amount of RAM available, as in smaller memory nodes the minimum container size should also be smaller
   *
   *  Value in MB!
   */
  recommendedMinimumContainerSize: function () {
    if (!this.clusterDataIsValid()) return null;
    var ram = this.get('clusterData.ram');
    switch (true) {
      case (ram <=4 ):
        return 256;
      case (ram <= 8):
        return 512;
      case (ram <= 24):
        return 1024;
      default:
        return 2048;
    }
  }.property('clusterData.ram'),

  /**
   * Maximum number of containers allowed per node
   * max (2*cores,min (1.8*DISKS,(Total available RAM) / MIN_CONTAINER_SIZE)))
   * min (2*CORES, 1.8*DISKS, (Total available RAM) / MIN_CONTAINER_SIZE)
   */
  containers: function () {
    if (!this.clusterDataIsValid()) return null;
    var cpu = this.get('clusterData.cpu');
    var disk = this.get('clusterData.disk');
    var ram = this.get('clusterData.ram');
    var containerSize = this.get('recommendedMinimumContainerSize');
    cpu *= 2;
    disk = Math.ceil(disk * 1.8);
    ram = (ram - this.get('reservedRam'));
    if (this.get('clusterData.hBaseInstalled')) {
      ram -= this.get('hBaseRam')
    }
    if (ram < 1) {
      ram = 1;
    }
    ram *= this.get('GB');
    ram /= containerSize;
    return Math.round(Math.min(cpu, Math.min(disk, ram)));
  }.property('clusterData.cpu', 'clusterData.ram', 'clusterData.hBaseInstalled', 'clusterData.disk', 'reservedRam', 'hBaseRam', 'recommendedMinimumContainerSize'),

  /**
   * amount of RAM per container
   * RAM-per-container = abs(MIN_CONTAINER_SIZE, (Total Available RAM) / containers))
   *
   * Value in MB!
   */
  ramPerContainer: function () {
    var containers = this.get('containers');
    var ram = this.get('clusterData.ram');
    ram = (ram - this.get('reservedRam'));
    if (this.get('clusterData.hBaseInstalled')) {
      ram -= this.get('hBaseRam')
    }
    if (ram < 1) {
      ram = 1;
    }
    ram *= this.get('GB');
    var container_ram = Math.abs(ram / containers);
    return container_ram > this.get('GB') ? container_ram / (512 * 512) : container_ram;
  }.property('recommendedMinimumContainerSize', 'containers', 'clusterData.ram', 'clusterData.hBaseInstalled', 'hBaseRam', 'reservedRam'),

  mapMemory: function () {
    return this.get('ramPerContainer');
  }.property('ramPerContainer'),

  reduceMemory: function () {
    var ramPerContainer = this.get('ramPerContainer');
    return ramPerContainer <= 2048 ? 2 * ramPerContainer : ramPerContainer;
  }.property('ramPerContainer'),

  amMemory: function () {
    return Math.max(this.get('mapMemory'), this.get('reduceMemory'));
  }.property('mapMemory', 'reduceMemory'),

  /**
   * Reserved for HBase and system memory is based on total available memory
   */



  reservedStackRecommendations: function () {
    var memory = this.get('clusterData.ram');
    var reservedStack = { 4: 1, 8: 2, 16: 2, 24: 4, 48: 6, 64: 8, 72: 8, 96: 12,
      128: 24, 256: 32, 512: 64};

    if (memory in reservedStack) {
      this.set('reservedRam', reservedStack[memory]);
    }
    if (memory <= 4)
      this.set('reservedRam', 1);
    else if (memory >= 512)
      this.set('reservedRam', 64);
    else
      this.set('reservedRam', 1);
  }.observes('clusterData.ram'),

  hbaseMemRecommendations: function () {
    var memory = this.get('clusterData.ram');
    var reservedHBase = {4:1, 8:1, 16:2, 24:4, 48:8, 64:8, 72:8, 96:16,
      128:24, 256:32, 512:64};

    if (memory in reservedHBase) {
      this.set('reservedRam', reservedHBase[memory]);
    }
    if (memory <= 4)
      this.set('hBaseRam', 1);
    else if (memory >= 512)
      this.set('hBaseRam', 64);
    else
      this.set('hBaseRam', 2);

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
    configs['yarn.nodemanager.resource.memory-mb'] = Math.round(this.get('containers') * this.get('ramPerContainer'));
    configs['yarn.scheduler.minimum-allocation-mb'] = Math.round(this.get('ramPerContainer'));
    configs['yarn.scheduler.maximum-allocation-mb'] = Math.round(this.get('containers') * this.get('ramPerContainer'));
    configs['yarn.app.mapreduce.am.resource.mb'] = Math.round(this.get('amMemory'));
    configs['yarn.app.mapreduce.am.command-opts'] = "-Xmx" + Math.round(0.8 * this.get('amMemory')) + "m";
    configs['mapreduce.map.memory.mb'] = Math.round(this.get('mapMemory'));
    configs['mapreduce.reduce.memory.mb'] = Math.round(this.get('reduceMemory'));
    configs['mapreduce.map.java.opts'] = "-Xmx" + Math.round(0.8 * this.get('mapMemory')) + "m";
    configs['mapreduce.reduce.java.opts'] = "-Xmx" + Math.round(0.8 * this.get('reduceMemory')) + "m";
    configs['mapreduce.task.io.sort.mb'] = Math.round(0.4 * this.get('mapMemory'));
    return configs;
  },

  /**
   * Calculate needed cluster data (like disk count, cpu count, ram (in MB!) and hbase availability)
   * @param {object} localDB Object with information about hosts and master/slave components
   */
  getClusterData: function (localDB) {
    this._super();
    var components = ['NODEMANAGER'];
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
   */
  clusterDataIsValid: function () {
    if (!this.get('clusterData')) return false;
    if (this.get('clusterData.ram') == null || this.get('clusterData.cpu') == null || this.get('clusterData.disk') == null || this.get('clusterData.hBaseInstalled') == null) return false;
    return true;
  }

});
