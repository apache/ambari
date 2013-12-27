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

App.statusMapper = App.QuickDataMapper.create({
  model: App.HostComponent,
  map: function (json) {
    console.time('App.statusMapper execution time');
    if (json.items) {
      var hostsCache = App.cache['Hosts'];
      var hostStatuses = {};
      var hostComponentStatuses = {};
      var addedHostComponents = [];
      var componentServiceMap = App.QuickDataMapper.componentServiceMap;
      var currentComponentStatuses = {};
      var currentHostStatuses = {};
      var previousHostStatuses = App.cache['previousHostStatuses'];
      var previousComponentStatuses = App.cache['previousComponentStatuses'];
      var hostComponentsOnService = {};

      json.items.forEach(function (host) {
        var hostName = host.Hosts.host_name;
        //update hosts, which have status changed
        if (previousHostStatuses[hostName] !== host.Hosts.host_status) {
          hostStatuses[hostName] = host.Hosts.host_status;
        }
        currentHostStatuses[hostName] = host.Hosts.host_status;
        var hostComponentsOnHost = [];
        host.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + hostName;
          var existedComponent = previousComponentStatuses[host_component.id];
          var service = componentServiceMap[host_component.HostRoles.component_name];

          if (existedComponent) {
            //update host-components, which have status changed
            if (existedComponent !== host_component.HostRoles.state) {
              hostComponentStatuses[host_component.id] = host_component.HostRoles.state;
            }
          } else {
            addedHostComponents.push({
              id: host_component.id,
              component_name: host_component.HostRoles.component_name,
              work_status: host_component.HostRoles.state,
              host_id: hostName,
              service_id: service
            });
            //update host-components only on adding due to Ember Data features
            if (hostsCache[hostName]) hostsCache[hostName].is_modified = true;
          }
          currentComponentStatuses[host_component.id] = host_component.HostRoles.state;

          //host-components to host relations
          hostComponentsOnHost.push(host_component.id);
          //host-component to service relations
          if (!hostComponentsOnService[service]) {
            hostComponentsOnService[service] = {
              host_components: []
            };
          }
          hostComponentsOnService[service].host_components.push(host_component.id);
        }, this);
        /**
         * updating relation between Host and his host-components
         */
        if (hostsCache[hostName]) {
          hostsCache[hostName].host_components = hostComponentsOnHost;
        }
      }, this);

      var hostComponents = App.HostComponent.find();
      var hosts = App.Host.find();

      hostComponents.forEach(function (hostComponent) {
        if (hostComponent) {
          var status = currentComponentStatuses[hostComponent.get('id')];
          //check whether component present in current response
          if (status) {
            //check whether component has status changed
            if (hostComponentStatuses[hostComponent.get('id')]) {
              hostComponent.set('workStatus', status);
            }
          } else {
            this.deleteRecord(hostComponent);
          }
        }
      }, this);

      if (addedHostComponents.length) {
        App.store.loadMany(this.get('model'), addedHostComponents);
      }

      App.cache['previousHostStatuses'] = currentHostStatuses;
      App.cache['previousComponentStatuses'] = currentComponentStatuses;
      App.cache['hostComponentsOnService'] = hostComponentsOnService;

      hosts.forEach(function (host) {
        var status = hostStatuses[host.get('id')];
        if (status) {
          host.set('healthStatus', status);
        }
      });
    }
    console.timeEnd('App.statusMapper execution time');
  }
});
