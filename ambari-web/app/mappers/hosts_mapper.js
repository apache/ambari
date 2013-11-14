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

var stringUtils = require('utils/string_utils');
/**
 * previousResponse contains only mutable properties of host
 * unlike App.cache.Hosts, which contains immutable properties
 */
var previousResponse = {};

App.hostsMapper = App.QuickDataMapper.create({

  model: App.Host,
  config: {
    id: 'Hosts.host_name',
    host_name: 'Hosts.host_name',
    public_host_name: 'Hosts.public_host_name',
    cluster_id: 'Hosts.cluster_name',// 1
    rack: 'Hosts.rack_info',
    host_components_key: 'host_components',
    host_components_type: 'array',
    host_components: {
      item: 'id'
    },
    cpu: 'Hosts.cpu_count',
    memory: 'Hosts.total_mem',
    disk_info: 'Hosts.disk_info',
    disk_total: 'metrics.disk.disk_total',
    disk_free: 'metrics.disk.disk_free',
    health_status: 'Hosts.host_status',
    load_one: 'metrics.load.load_one',
    load_five: 'metrics.load.load_five',
    load_fifteen: 'metrics.load.load_fifteen',
    cpu_system: 'metrics.cpu.cpu_system',
    cpu_user: 'metrics.cpu.cpu_user',
    mem_total: 'metrics.memory.mem_total',
    mem_free: 'metrics.memory.mem_free',
    last_heart_beat_time: "Hosts.last_heartbeat_time",
    os_arch: 'Hosts.os_arch',
    os_type: 'Hosts.os_type',
    ip: 'Hosts.ip'
  },
  map: function (json) {
    console.time('App.hostsMapper execution time');
    if (json.items) {
      var hostsWithFullInfo = [];
      var modifiedHosts = [];
      var hostIds = {};
      var cacheData = App.cache['Hosts'];
      var currentHostStatuses = {};
      var hostsData = {};
      var isModelLoaded = false;

      json.items.forEach(function (item) {
        //receive host_components when hosts were added
        item.host_components = item.host_components || [];
        item.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
        }, this);

        hostIds[item.Hosts.host_name] = true;
        currentHostStatuses[item.Hosts.host_name] = item.Hosts.host_status;

        var parsedItem = this.parseIt(item, this.config);
        var hostCache = cacheData[item.Hosts.host_name];

        hostsWithFullInfo.push(parsedItem);
        if (hostCache) {
          if (hostCache.is_modified) {
            modifiedHosts.push(parsedItem);
            delete hostCache.is_modified;
          } else {
            hostsData[item.Hosts.host_name] = this.getDiscrepancies(parsedItem, previousResponse[item.Hosts.host_name]);
          }
        }
        previousResponse[item.Hosts.host_name] = parsedItem;
      }, this);

      App.cache['previousHostStatuses'] = currentHostStatuses;
      hostsWithFullInfo = this.sortByPublicHostName(hostsWithFullInfo);

      var clientHosts = App.Host.find();

      if (clientHosts) {
        // hosts were added
        if (clientHosts.get('length') < hostsWithFullInfo.length) {
          hostsWithFullInfo.forEach(function (host) {
            cacheData[host.id] = {
              ip: host.ip,
              os_arch: host.os_arch,
              os_type: host.os_type,
              public_host_name: host.public_host_name,
              memory: host.memory,
              cpu: host.cpu,
              host_components: host.host_components
            };
          });
          App.store.loadMany(this.get('model'), hostsWithFullInfo);
          isModelLoaded = true;
        } else {
          //restore properties from cache instead of asking them from server
          modifiedHosts.forEach(function (host) {
            var cacheHost = cacheData[host.id];
            if (cacheHost) {
              host.ip = cacheHost.ip;
              host.os_arch = cacheHost.os_arch;
              host.os_type = cacheHost.os_type;
              host.public_host_name = cacheHost.public_host_name;
              host.memory = cacheHost.memory;
              host.cpu = cacheHost.cpu;
              host.host_components = cacheHost.host_components;
            }
          });
          App.store.loadMany(this.get('model'), modifiedHosts);
        }
        // hosts were deleted
        if (clientHosts.get('length') > hostsWithFullInfo.length) {
          clientHosts.forEach(function (host) {
            if (host && !hostIds[host.get('hostName')]) {
              // Delete old ones as new ones will be
              // loaded by loadMany().
              host.deleteRecord();
              App.store.commit();
              host.get('stateManager').transitionTo('loading');
              delete cacheData[host.get('id')];
            }
          });
        }
      }
      if (!isModelLoaded) {
        App.Host.find().forEach(function (_host) {
          var hostData = hostsData[_host.get('id')];
          if (hostData) {
            for (var i in hostData) {
              _host.set(stringUtils.underScoreToCamelCase(i), hostData[i]);
            }
          }
        });
      }
    }
    console.timeEnd('App.hostsMapper execution time');
  },
  /**
   * check mutable fields whether they have been changed and if positive
   * return host object only with properties, that contains new value
   * @param current
   * @param previous
   * @return {*}
   */
  getDiscrepancies: function (current, previous) {
    var result = {};
    var fields = ['disk_total', 'disk_free', 'health_status', 'load_one', 'cpu_system', 'cpu_user', 'mem_total', 'mem_free'];
    if (previous) {
      fields.forEach(function (field) {
        if (current[field] != previous[field]) result[field] = current[field];
      });
      if (JSON.stringify(current.disk_info) !== JSON.stringify(previous.disk_info)) {
        result.disk_info = current.disk_info;
      }
      if (JSON.stringify(current.host_components) !== JSON.stringify(previous.host_components)) {
        result.host_components = current.host_components;
      }
      result.last_heart_beat_time = current.last_heart_beat_time;
      return result;
    }
    return current;
  },

  /**
   * Default data sorting by public_host_name field
   * @param data
   * @return {Array}
   */
  sortByPublicHostName: function(data) {
    data.sort(function(a, b) {
      var ap = a.public_host_name;
      var bp = b.public_host_name;
      if (ap > bp) return 1;
      if (ap < bp) return -1;
      return 0;
    });
    return data;
  }

});
