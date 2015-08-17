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
    service_id: 'HostRoles.service_name',
    admin_state: 'HostRoles.desired_admin_state'
  },
  map: function (json) {
    console.time('App.componentConfigMapper execution time');
    var hostComponents = [];
    var newHostComponentsMap = {};
    var cacheServices = App.cache['services'];
    var currentServiceComponentsMap = this.buildServiceComponentMap(cacheServices);
    var mapConfig = this.get('config');
    // We do not want to parse JSON if there is no need to
    var hostComponentJsonMap = {};
    var hostComponentJsonIds = [];

    if (json.items.length > 0 || this.get('model').find().someProperty('staleConfigs', true)) {
      json.items.forEach(function (item) {
        item.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + '_' + host_component.HostRoles.host_name;
          hostComponentJsonIds.push(host_component.id);
          hostComponentJsonMap[host_component.id] = host_component;
        });
      });
      this.get('model').find().forEach(function (hostComponent) {
        var id = hostComponent.get('id');
        var hostComponentJson = hostComponentJsonMap[id];
        var currentStaleConfigsState = Boolean(hostComponentJson);
        var stateChanged = hostComponent.get('staleConfigs') !== currentStaleConfigsState;

        if (stateChanged) {
          hostComponent.set('staleConfigs', currentStaleConfigsState);
        }
        //delete loaded host-components, so only new ones left
        delete hostComponentJsonMap[id];
      });
      hostComponentJsonIds.forEach(function (hcId) {
        var newHostComponent = hostComponentJsonMap[hcId];
        if (newHostComponent) {
          var serviceName = newHostComponent.HostRoles.service_name;
          hostComponents.push(this.parseIt(newHostComponent, mapConfig));
          if (!newHostComponentsMap[serviceName]) {
            newHostComponentsMap[serviceName] = [];
          }
          if (currentServiceComponentsMap[serviceName] && !currentServiceComponentsMap[serviceName][newHostComponent.id]) {
            newHostComponentsMap[serviceName].push(newHostComponent.id);
          }
        }
      }, this);
      if (hostComponents.length > 0) {
        App.store.commit();
        App.store.loadMany(this.get('model'), hostComponents);
        this.addNewHostComponents(newHostComponentsMap, cacheServices);
      }
    }
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
   * @param {object} newHostComponentsMap
   * @param {Array} cacheServices
   * @return {boolean}
   */
  addNewHostComponents: function (newHostComponentsMap, cacheServices) {
    if (!newHostComponentsMap || !cacheServices) return false;
    cacheServices.forEach(function (service) {
      if (newHostComponentsMap[service.ServiceInfo.service_name]) {
        newHostComponentsMap[service.ServiceInfo.service_name].forEach(function (componentId) {
          service.host_components.push(componentId)
        });
      }
    }, this);
    return true;
  }
});
