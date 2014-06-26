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
  map: function (json, isAll) {
    var self = this;
    console.time('App.hostsMapper execution time');
    if (json.items.length) {
      var hostNames = json.items.mapProperty('Hosts.host_name');
      var realUrl = '/hosts?<parameters>fields=host_components/HostRoles/component_name,host_components/HostRoles/service_name,host_components/HostRoles/stale_configs,host_components/HostRoles/state,host_components/HostRoles/host_name,host_components/HostRoles/maintenance_state&minimal_response=true';
      var hostsUrl = App.apiPrefix + '/clusters/' + App.get('clusterName') + realUrl.replace('<parameters>', 'Hosts/host_name.in(' + hostNames.join(',') + ')&');

      $.getJSON(hostsUrl, function (jsonHostComponents) {
        var hostsWithFullInfo = [];
        var hostIds = {};
        var components = [];
        json.items.forEach(function (item, index) {
          item.host_components = jsonHostComponents.items.findProperty('Hosts.host_name',item.Hosts.host_name).host_components || [];
          item.host_components.forEach(function (host_component) {
            host_component.id = host_component.HostRoles.component_name + "_" + item.Hosts.host_name;
            var component = self.parseIt(host_component, self.hostComponentConfig);
            component.id = host_component.HostRoles.component_name + "_" + item.Hosts.host_name;
            component.host_id = item.Hosts.host_name;
            component.host_name = item.Hosts.host_name;
            components.push(component);
          });
          item.critical_alerts_count = (item.alerts) ? item.alerts.summary.CRITICAL + item.alerts.summary.WARNING : 0;
          item.cluster_id = App.get('clusterName');
          item.index = index;

          var parsedItem = self.parseIt(item, self.config);
          parsedItem.is_requested = !isAll;

          hostIds[item.Hosts.host_name] = parsedItem;

          hostsWithFullInfo.push(parsedItem);
        });

        hostsWithFullInfo = hostsWithFullInfo.sortProperty('public_host_name');

        App.Host.find().forEach(function (host) {
          if (isAll && host.get('isRequested')) {
            hostIds[host.get('hostName')].is_requested = true;
          } else if (!hostIds[host.get('hostName')]) {
            host.set('isRequested', false);
          }
        });
        App.store.loadMany(App.HostComponent, components);
        App.store.loadMany(App.Host, hostsWithFullInfo);
        App.router.set('mainHostController.filteredCount', parseInt(json.itemTotal));
        App.router.set('mainHostController.filteringComplete', true);
      });
    }else{
      App.Host.find().forEach(function (host) {
        host.set('isRequested', false);
      })
      App.router.set('mainHostController.filteredCount', 0);
      App.router.set('mainHostController.filteringComplete', true);
    }
    console.timeEnd('App.hostsMapper execution time');
  }
});
