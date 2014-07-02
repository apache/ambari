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
    work_status: 'HostRoles.state',
    passive_state: 'HostRoles.maintenance_state',
    component_name: 'HostRoles.component_name',
    host_name: 'HostRoles.host_name',
    $ha_status: '',
    $display_name_advanced: '',
    stale_configs: 'HostRoles.stale_configs',
    host_id: 'HostRoles.host_name',
    service_id: 'HostRoles.service_name'
  },
  map: function (json) {
    console.time('App.componentConfigMapper execution time');
    var hostComponents = [];
    var serviceToHostComponentIdMap = {};
    var cacheServices = App.cache['services'];
    var loadedServiceComponentsMap = this.buildServiceComponentMap(cacheServices);
    var mapConfig = this.get('config');
    // We do not want to parse JSON if there is no need to
    var hostComponentJsonMap = {};
    var hostComponentJsonIds = [];
    var hostComponentJsonsToRemove = {};
    json.items.forEach(function (item) {
      item.host_components.forEach(function (host_component) {
        host_component.id = host_component.HostRoles.component_name + '_' + host_component.HostRoles.host_name;
        hostComponentJsonIds.push(host_component.id);
        hostComponentJsonMap[host_component.id] = host_component;
      });
    });
    this.get('model').find().forEach(function (hostComponent) {
      var hostComponentJson = hostComponentJsonMap[hostComponent.get('id')];
      if (!hostComponentJson && !hostComponent.get('isMaster')) {
        hostComponent.set('staleConfigs', false);
      }
      if (hostComponentJson!=null && hostComponent.get('staleConfigs') &&
          hostComponentJson.HostRoles.state == hostComponent.get('workStatus') &&
          hostComponentJson.HostRoles.maintenance_state == hostComponent.get('passiveState')) {
        // A component already exists with correct stale_configs flag and other values - no need to load again
        hostComponentJsonsToRemove[hostComponentJson.id] = hostComponentJson;
      }
    });
    hostComponentJsonIds.forEach(function (hcId) {
      if(!hostComponentJsonsToRemove[hcId]){
        var host_component = hostComponentJsonMap[hcId];
        var serviceName = host_component.HostRoles.service_name;
        hostComponents.push(this.parseIt(host_component, mapConfig));
        if (!serviceToHostComponentIdMap[serviceName]) {
          serviceToHostComponentIdMap[serviceName] = [];
        }
        serviceToHostComponentIdMap[serviceName].push(host_component.id);
      }
    }, this);
    App.store.loadMany(this.get('model'), hostComponents);
    this.addNewHostComponents(loadedServiceComponentsMap, serviceToHostComponentIdMap, cacheServices);
    console.timeEnd('App.componentConfigMapper execution time');
  },

  /**
   * build map that include loaded host-components to avoid duplicate loading
   * @param cacheServices
   * @return {Object}
   */
  buildServiceComponentMap: function (cacheServices) {
    var loadedServiceComponentsMap = {};

    cacheServices.forEach(function (cacheService) {
      var componentsMap = {};

      cacheService.host_components.forEach(function (componentId) {
        componentsMap[componentId] = true;
      });
      loadedServiceComponentsMap[cacheService.ServiceInfo.service_name] = componentsMap;
    });
    return loadedServiceComponentsMap;
  },

  /**
   * add only new host-components to every service
   * to update service - host-component relations in model
   * @param loadedServiceComponentsMap
   * @param serviceToHostComponentIdMap
   * @param cacheServices
   * @return {boolean}
   */
  addNewHostComponents: function (loadedServiceComponentsMap, serviceToHostComponentIdMap, cacheServices) {
    if (!loadedServiceComponentsMap || !serviceToHostComponentIdMap || !cacheServices) return false;

    for (var serviceName in serviceToHostComponentIdMap) {
      var loadedService = cacheServices.findProperty('ServiceInfo.service_name', serviceName);

      if (serviceToHostComponentIdMap[serviceName] && loadedService) {
        serviceToHostComponentIdMap[serviceName].forEach(function (componentId) {
          if (!loadedServiceComponentsMap[serviceName][componentId]) {
            loadedService.host_components.push(componentId)
          }
        });
      }
    }
    return true;
  }
});
