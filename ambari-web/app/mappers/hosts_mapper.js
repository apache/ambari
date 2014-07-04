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

App.hostsMapper = App.QuickDataMapper.create({

  model: App.Host,
  config: {
    id: 'Hosts.host_name',
    host_name: 'Hosts.host_name',
    public_host_name: 'Hosts.public_host_name',
    cluster_id: 'cluster_id',// Hosts.cluster_name
    rack: 'Hosts.rack_info',
    host_components_key: 'host_components',
    host_components_type: 'array',
    host_components: {
      item: 'id'
    },
    critical_alerts_count: 'critical_alerts_count',
    cpu: 'Hosts.cpu_count',
    cpu_physical: 'Hosts.ph_cpu_count',
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
    ip: 'Hosts.ip',
    passive_state: 'Hosts.maintenance_state',
    index: 'index'
  },
  hostComponentConfig: {
    component_name: 'HostRoles.component_name',
    service_id: 'HostRoles.service_name',
    passive_state: 'HostRoles.maintenance_state',
    work_status: 'HostRoles.state',
    stale_configs: 'HostRoles.stale_configs',
    host_name: 'host_name'
  },
  map: function (json, returnMapped) {
    returnMapped = !!returnMapped;
    console.time('App.hostsMapper execution time');
    if (json.items) {
      var hostsWithFullInfo = [];
      var hostIds = {};
      var components = [];
      var componentsIdMap = {};
      var cacheServices = App.cache['services'];
      var loadedServiceComponentsMap = App.get('componentConfigMapper').buildServiceComponentMap(cacheServices);
      var serviceToHostComponentIdMap = {};

      json.items.forEach(function (item, index) {
        item.host_components = item.host_components || [];
        item.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + item.Hosts.host_name;
          var component = this.parseIt(host_component, this.hostComponentConfig);
          var serviceName = host_component.HostRoles.service_name;

          component.id = host_component.HostRoles.component_name + "_" + item.Hosts.host_name;
          component.host_id = item.Hosts.host_name;
          component.host_name = item.Hosts.host_name;
          components.push(component);
          componentsIdMap[component.id] = component;
          if (!serviceToHostComponentIdMap[serviceName]) {
            serviceToHostComponentIdMap[serviceName] = [];
          }
          serviceToHostComponentIdMap[serviceName].push(component.id);
        }, this);
        item.critical_alerts_count = (item.alerts) ? item.alerts.summary.CRITICAL + item.alerts.summary.WARNING : 0;
        item.cluster_id = App.get('clusterName');
        item.index = index;


        var parsedItem = this.parseIt(item, this.config);
        parsedItem.is_requested = true;

        hostIds[item.Hosts.host_name] = parsedItem;

        hostsWithFullInfo.push(parsedItem);
      }, this);

      if(returnMapped){
        return hostsWithFullInfo;
      }

      hostsWithFullInfo = hostsWithFullInfo.sortProperty('public_host_name');

      App.Host.find().forEach(function (host) {
        if (!hostIds[host.get('hostName')]) {
          host.set('isRequested', false);
        }
      });
      App.HostComponent.find().filterProperty('isMaster').forEach(function(component) {
        if (componentsIdMap[component.get('id')]) componentsIdMap[component.get('id')].display_name_advanced = component.get('displayNameAdvanced');
      });
      App.store.commit();
      App.store.loadMany(App.HostComponent, components);
      App.store.loadMany(App.Host, hostsWithFullInfo);
      var itemTotal = parseInt(json.itemTotal);
      if (!isNaN(itemTotal)) {
        App.router.set('mainHostController.filteredCount', itemTotal);
      }
      //bind host-components with service records
      App.get('componentConfigMapper').addNewHostComponents(loadedServiceComponentsMap, serviceToHostComponentIdMap, cacheServices);
    }
    console.timeEnd('App.hostsMapper execution time');
  }
});
