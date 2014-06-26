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
var lazyloading = require('utils/lazy_loading');

App.MainChartsHeatmapRackView = Em.View.extend({
  templateName: require('templates/main/charts/heatmap/heatmap_rack'),
  classNames: ['rack'],
  classNameBindings: ['visualSchema'],

  /** rack status block class */
  statusIndicator: 'statusIndicator',
  /** loaded hosts of rack */
  hosts: [],

  willDestroyElement: function () {
    this.get('hosts').clear();
  },

  /**
   * get hosts from server
   */
  getHosts: function () {
    App.ajax.send({
      name: 'hosts.heatmaps',
      sender: this,
      data: {},
      success: 'getHostsSuccessCallback',
      error: 'getHostsErrorCallback'
    });
  },

  getHostsSuccessCallback: function (data, opt, params) {
    this.pushHostsToRack(data);
    this.displayHosts();
  },

  /**
   * display hosts of rack
   */
  displayHosts: function () {
    var rackHosts = this.get('rack.hosts');

    if (this.get('hosts.length') === 0) {
      if (rackHosts.length > 100) {
        lazyloading.run({
          initSize: 100,
          chunkSize: 200,
          delay: 100,
          destination: this.get('hosts'),
          source: rackHosts,
          context: this.get('rack')
        });
      } else {
        this.set('hosts', rackHosts);
        this.set('rack.isLoaded', true);
      }
    }
  },

  getHostsErrorCallback: function (request, ajaxOptions, error, opt, params) {
    this.set('rack.isLoaded', true);
  },
  /**
   * push hosts to rack
   * @param data
   */
  pushHostsToRack: function (data) {
    var newHostsData = [];
    var rackHosts = this.get('rack.hosts');

    data.items.forEach(function (item) {
      newHostsData.push({
        hostName: item.Hosts.host_name,
        publicHostName: item.Hosts.public_host_name,
        osType: item.Hosts.os_type,
        ip: item.Hosts.ip,
        diskTotal: item.metrics ? item.metrics.disk.disk_total : 0,
        diskFree: item.metrics ? item.metrics.disk.disk_free : 0,
        cpuSystem: item.metrics ? item.metrics.cpu.cpu_system : 0,
        cpuUser: item.metrics ? item.metrics.cpu.cpu_user : 0,
        memTotal: item.metrics ? item.metrics.memory.mem_total : 0,
        memFree: item.metrics ? item.metrics.memory.mem_free : 0,
        hostComponents: item.host_components.mapProperty('HostRoles.component_name')
      })
    });

    if (rackHosts.length > 0) {
      this.updateLoadedHosts(rackHosts, newHostsData);
    } else {
      this.set('rack.hosts', newHostsData);
    }
  },

  updateLoadedHosts: function (rackHosts, newHostsData) {
    var rackHostsMap = {};
    var isNewHosts = false;

    //create map
    rackHosts.forEach(function (host) {
      rackHostsMap[host.hostName] = host;
    });

    newHostsData.forEach(function (item) {
      var currentHostInfo = rackHostsMap[item.hostName];

      if (currentHostInfo) {
        ['diskTotal', 'diskFree', 'cpuSystem', 'cpuUser', 'memTotal', 'memFree', 'hostComponents'].forEach(function (property) {
          currentHostInfo[property] = item[property];
        });
        delete rackHostsMap[item.hostName];
      } else {
        isNewHosts = true;
      }
    }, this);

    //if hosts were deleted or added then reload hosts view
    if (!App.isEmptyObject(rackHostsMap) || isNewHosts) {
      this.redrawHostsView(newHostsData)
    }
  },

  /**
   * reload hosts rack
   * @param newHostsData
   */
  redrawHostsView: function (newHostsData) {
    this.set('rack.isLoaded', false);
    this.get('hosts').clear();
    this.set('rack.hosts', newHostsData);
  },

  /**
   * call metrics update after hosts of rack are loaded
   */
  updateMetrics: function(){
    if (this.get('rack.isLoaded')) {
      this.get('controller').loadMetrics();
    }
  }.observes('rack.isLoaded'),

  didInsertElement: function () {
    this.set('rack.isLoaded', false);
    if (this.get('rack.hosts.length') > 0) {
      this.displayHosts();
    }
    this.getHosts();
  },
  /**
   * Provides the CSS style for an individual host.
   * This can be used as the 'style' attribute of element.
   */
  hostCssStyle: function () {
    var rack = this.get('rack');
    var widthPercent = 100;
    var hostCount = rack.get('hosts.length');
    if (hostCount && hostCount < 11) {
      widthPercent = (100 / hostCount) - 0.5;
    } else {
      widthPercent = 10; // max out at 10%
    }
    return "width:" + widthPercent + "%;float:left;";
  }.property('rack.isLoaded')
});
