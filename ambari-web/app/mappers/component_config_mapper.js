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
    display_name: 'HostRoles.display_name',
    host_name: 'HostRoles.host_name',
    public_host_name: 'HostRoles.public_host_name',
    $ha_status: '',
    $display_name_advanced: '',
    stale_configs: 'HostRoles.stale_configs',
    host_id: 'HostRoles.host_name',
    service_id: 'HostRoles.service_name',
    admin_state: 'HostRoles.desired_admin_state'
  },
  map: function (json) {
    console.time('App.componentConfigMapper execution time');
    var staleConfigHostsMap = App.cache.staleConfigsComponentHosts;
    var componentsNeedRestart = json.items.mapProperty('ServiceComponentInfo.component_name');
    var components = App.MasterComponent.find().toArray()
      .concat(App.ClientComponent.find().toArray())
      .concat(App.SlaveComponent.find().toArray());

    //clear stale config hosts of component after restart
    components.forEach(function(component) {
      if (!componentsNeedRestart.contains(component.get('componentName'))) {
        staleConfigHostsMap[component.get('componentName')] = [];
        component.set('staleConfigHosts', []);
      }
    });

    json.items.forEach(function(item) {
      var componentName = item.ServiceComponentInfo.component_name;
      var hosts = item.host_components.mapProperty('HostRoles.host_name') || [];
      staleConfigHostsMap[componentName] = hosts;
      if (App.HostComponent.isMaster(componentName)) {
        App.MasterComponent.find(componentName).set('staleConfigHosts', hosts);
      } else if (App.HostComponent.isSlave(componentName)) {
        App.SlaveComponent.find(componentName).set('staleConfigHosts', hosts);
      } else if (App.HostComponent.isClient(componentName)) {
        App.ClientComponent.find(componentName).set('staleConfigHosts', hosts);
      }
    });
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
