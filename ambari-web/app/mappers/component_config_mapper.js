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

App.componentConfigMapper = App.QuickDataMapper.create({
  model: App.HostComponent,
  config: {
    id: 'id',
    work_status: 'state',
    passive_state: 'maintenance_state',
    component_name: 'component_name',
    $ha_status: 'none',
    $display_name_advanced: 'none',
    stale_configs: 'stale_configs',
    host_id: 'host_name',
    service_id: 'service_name'
  },
  map: function (json) {
    console.time('App.componentConfigMapper execution time');
    var hostComponents = [];
    var serviceToHostComponentIdMap = {};
    json.items.forEach(function (item) {
      item.host_components.forEach(function (host_component) {
        host_component = host_component.HostRoles;
        host_component.id = host_component.component_name + '_' + host_component.host_name;
        hostComponents.push(this.parseIt(host_component, this.get('config')));
        if (!serviceToHostComponentIdMap[host_component.service_name]) {
          serviceToHostComponentIdMap[host_component.service_name] = [];
        }
        serviceToHostComponentIdMap[host_component.service_name].push(host_component.id);
      }, this);
    }, this);
    App.store.loadMany(this.get('model'), hostComponents);
    for (var serviceName in serviceToHostComponentIdMap) {
      var service = App.cache['services'].findProperty('ServiceInfo.service_name', serviceName);
      if (service) {
        service.host_components.pushObjects(serviceToHostComponentIdMap[serviceName]);
      }
    }
    console.timeEnd('App.componentConfigMapper execution time');
  }
});
