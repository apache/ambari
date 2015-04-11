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

App.MainChartsHeatmapController = Em.Controller.extend({
  name: 'mainChartsHeatmapController',
  rackMap: [],
  modelRacks: [],
  rackViews: [],
  loadRacksUrlParams: 'fields=Hosts/rack_info,Hosts/host_name,Hosts/public_host_name,Hosts/os_type,Hosts/ip,host_components,metrics/disk,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free&minimal_response=true',

  racks: function () {
    return this.get('modelRacks');
  }.property('modelRacks.@each.isLoaded'),

  /**
   * get hosts from server
   */
  loadRacks: function () {
    this.get('modelRacks').clear();
    this.get('rackMap').clear();
    var urlParams = this.get('loadRacksUrlParams');
    App.ajax.send({
      name: 'hosts.heatmaps',
      sender: this,
      data: {
        urlParams: urlParams
      },
      success: 'getHostsSuccessCallback'
    });
  },

  getHostsSuccessCallback: function (data, opt, params) {
    var hosts = [];
    data.items.forEach(function (item) {
      hosts.push({
        hostName: item.Hosts.host_name,
        publicHostName: item.Hosts.public_host_name,
        osType: item.Hosts.os_type,
        ip: item.Hosts.ip,
        rack: item.Hosts.rack_info,
        diskTotal: item.metrics ? item.metrics.disk.disk_total : 0,
        diskFree: item.metrics ? item.metrics.disk.disk_free : 0,
        cpuSystem: item.metrics ? item.metrics.cpu.cpu_system : 0,
        cpuUser: item.metrics ? item.metrics.cpu.cpu_user : 0,
        memTotal: item.metrics ? item.metrics.memory.mem_total : 0,
        memFree: item.metrics ? item.metrics.memory.mem_free : 0,
        hostComponents: item.host_components.mapProperty('HostRoles.component_name')
      });
    });
    var rackMap = this.indexByRackId(hosts);
    var modelRacks = this.toList(rackMap);
    //this list has an empty host array property
    this.set('rackMap', rackMap);
    this.set('modelRacks', modelRacks);
  },

  indexByRackId: function (hosts) {
    var rackMap = [];
    hosts.forEach(function (host) {
      var rackId = host.rack;
      if(!rackMap[rackId]) {
        rackMap[rackId] =
          Em.Object.create({
            name: rackId,
            rackId: rackId,
            hosts: [host]
          });
      } else {
        rackMap[rackId].hosts.push(host);
      }
    });
    return rackMap;
  },

  toList: function (rackMap) {
    var racks = [];
    var i = 0;
    for (var rackKey in rackMap) {
      if (rackMap.hasOwnProperty(rackKey)) {
        racks.push(
          Em.Object.create({
            name: rackKey,
            rackId: rackKey,
            hosts: [],
            isLoaded: false,
            index: i++
          })
        );
      }
    }
    return racks;
  },

  allMetrics: function () {
    var metrics = [];

    // Display host heatmaps if the stack definition has a host metrics service to display it.
    if(App.get('services.hostMetrics').length) {
      metrics.pushObjects([
        App.MainChartHeatmapDiskSpaceUsedMetric.create(),
        App.MainChartHeatmapMemoryUsedMetric.create(),
        App.MainChartHeatmapCpuWaitIOMetric.create()
      ]);
    }

    return metrics;
  }.property(),

  selectedMetric: null,

  inputMaximum: '',

  validation: function () {
    if (this.get('selectedMetric')) {
      if (/^\d+$/.test(this.get('inputMaximum'))) {
        $('#inputMaximum').removeClass('error');
        this.set('selectedMetric.maximumValue', this.get('inputMaximum'));
      } else {
        $('#inputMaximum').addClass('error');
      }
    }
  }.observes('inputMaximum'),


  addRackView: function (view) {
    this.get('rackViews').push(view);
    if (this.get('rackViews').length == this.get('modelRacks').length) {
      this.displayAllRacks();
    }
  },

  displayAllRacks: function () {
    if (this.get('rackViews').length) {
      this.get('rackViews').pop().displayHosts();
      this.displayAllRacks();
    }
  },

  showHeatMapMetric: function (event) {
    var metricItem = event.context;
    if (metricItem) {
      this.set('selectedMetric', metricItem);
    }
  },

  hostToSlotMap: function () {
    return this.get('selectedMetric.hostToSlotMap');
  }.property('selectedMetric.hostToSlotMap'),

  loadMetrics: function () {
    var selectedMetric = this.get('selectedMetric');
    var hostNames = [];
    if (selectedMetric && this.get('racks').everyProperty('isLoaded', true)) {
      this.get('racks').forEach(function (rack) {
        hostNames = hostNames.concat(rack.hosts.mapProperty('hostName'));
      });
      selectedMetric.refreshHostSlots(hostNames);
    }
    this.set('inputMaximum', this.get('selectedMetric.maximumValue'));
  }.observes('selectedMetric'),

  /**
   * return class name for to be used for containing each rack.
   *
   * @this App.MainChartsHeatmapController
   */
  rackClass: function () {
    var rackCount = this.get('racks.length');
    if (rackCount < 2) {
      return "span12";
    } else if (rackCount == 2) {
      return "span6";
    } else {
      return "span4";
    }
  }.property('racks.length')
});
