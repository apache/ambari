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
      var previousHostStatuses = App.cache['previousHostStatuses'];
      var previousComponentStatuses = App.cache['previousComponentStatuses'];
      var previousComponentPassiveStates = App.cache['previousComponentPassiveStates'];
      var hostComponentRecordsMap = App.cache['hostComponentRecordsMap'];
      var servicesCache = App.cache['services'];
      var hostStatuses = {};
      var addedHostComponents = [];
      var updatedHostComponents = [];
      var componentServiceMap = App.QuickDataMapper.componentServiceMap();
      var currentComponentStatuses = {};
      var currentComponentPassiveStates = {};
      var currentHostStatuses = {};
      var hostComponentsOnService = {};

      json.items.forEach(function (host) {
        var hostName = host.Hosts.host_name;
        //update hosts, which have status changed
        if (previousHostStatuses[hostName] !== host.Hosts.host_status) {
          hostStatuses[hostName] = host.Hosts.host_status;
        }
        //preserve all hosts' status
        currentHostStatuses[hostName] = host.Hosts.host_status;
        var hostComponentsOnHost = [];
        host.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + hostName;
          var existedComponent = previousComponentStatuses[host_component.id];
          var existedPassiveComponent = previousComponentPassiveStates[host_component.id];
          var service = componentServiceMap[host_component.HostRoles.component_name];

          //delete all currently existed host-components to indicate which need to be deleted from model
          delete previousComponentStatuses[host_component.id];
          delete previousComponentPassiveStates[host_component.id];

          if (existedComponent || existedPassiveComponent) {
            //update host-components, which have status changed
            if (existedComponent !== host_component.HostRoles.state || existedPassiveComponent !== host_component.HostRoles.maintenance_state) {
              updatedHostComponents.push(host_component);
            }
          } else {
            addedHostComponents.push({
              id: host_component.id,
              component_name: host_component.HostRoles.component_name,
              passive_state: host_component.HostRoles.maintenance_state,
              work_status: host_component.HostRoles.state,
              host_id: hostName,
              service_id: service
            });
            //update host-components only on adding due to Ember Data features
            if (hostsCache[hostName]) hostsCache[hostName].is_modified = true;
          }
          currentComponentStatuses[host_component.id] = host_component.HostRoles.state;
          currentComponentPassiveStates[host_component.id] = host_component.HostRoles.maintenance_state;
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
        } else {
          hostsCache[hostName] = {};
        }
        //check whether Nagios installed and started
        if (host.alerts) {
          hostsCache[hostName].critical_alerts_count = host.alerts.summary.CRITICAL + host.alerts.summary.WARNING;
        } else {
          hostsCache[hostName].critical_alerts_count = 0;
        }
      }, this);


      for (var id in previousComponentStatuses) {
        this.deleteRecord(hostComponentRecordsMap[id]);
      }

      updatedHostComponents.forEach(function (hostComponent) {
        var hostComponentRecord = hostComponentRecordsMap[hostComponent.id];
        if (hostComponentRecord) {
          hostComponentRecord.set('workStatus', hostComponent.HostRoles.state);
          hostComponentRecord.set('passiveState', hostComponent.HostRoles.maintenance_state);
        }
      }, this);

      var hostRecords = App.Host.find();
      hostRecords.forEach(function (host) {
        var status = hostStatuses[host.get('id')];
        var hostCache = hostsCache[host.get('id')];
        if (status) {
          host.set('healthStatus', status);
        }
        if (hostCache) {
          host.set('criticalAlertsCount', hostCache.critical_alerts_count);
        }
      });

      if (addedHostComponents.length) {
        App.store.loadMany(this.get('model'), addedHostComponents);
        App.HostComponent.find().forEach(function(hostComponent){
          hostComponentRecordsMap[hostComponent.get('id')] = hostComponent;
        });
      }

      // update services workStatus and passiveState
      App.Service.find().forEach(function (service) {
        var cachedServiceData = servicesCache.findProperty('ServiceInfo.service_name', service.get('serviceName'));
        if (cachedServiceData) {
          service.set('workStatus', cachedServiceData.ServiceInfo.state);
          service.set('passiveState', cachedServiceData.ServiceInfo.passive_state);
        }
      }, this);

      App.cache['previousHostStatuses'] = currentHostStatuses;
      App.cache['previousComponentStatuses'] = currentComponentStatuses;
      App.cache['previousComponentPassiveStates'] = currentComponentPassiveStates;
      App.cache['hostComponentsOnService'] = hostComponentsOnService;

    }
    console.timeEnd('App.statusMapper execution time');
    if (!App.router.get('clusterController.isLoaded')) {
      App.hostsMapper.map(json);
    }
  }
});
