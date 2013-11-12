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
  componentServiceMap: {
    'NAMENODE': 'HDFS',
    'SECONDARY_NAMENODE': 'HDFS',
    'DATANODE': 'HDFS',
    'HDFS_CLIENT': 'HDFS',
    'JOBTRACKER': 'MAPREDUCE',
    'TASKTRACKER': 'MAPREDUCE',
    'MAPREDUCE_CLIENT': 'MAPREDUCE',
    'MAPREDUCE2_CLIENT': 'MAPREDUCE2',
    'HISTORYSERVER': 'MAPREDUCE2',
    'TEZ_CLIENT': 'TEZ',
    'RESOURCEMANAGER': 'YARN',
    'YARN_CLIENT': 'YARN',
    'NODEMANAGER': 'YARN',
    'ZOOKEEPER_SERVER': 'ZOOKEEPER',
    'ZOOKEEPER_CLIENT': 'ZOOKEEPER',
    'HBASE_MASTER': 'HBASE',
    'HBASE_REGIONSERVER': 'HBASE',
    'HBASE_CLIENT': 'HBASE',
    'PIG': 'PIG',
    'SQOOP': 'SQOOP',
    'OOZIE_SERVER': 'OOZIE',
    'OOZIE_CLIENT': 'OOZIE',
    'HIVE_SERVER': 'HIVE',
    'HIVE_METASTORE': 'HIVE',
    'HIVE_CLIENT': 'HIVE',
    'MYSQL_SERVER': 'HIVE',
    'HCAT': 'HCATALOG',
    'WEBHCAT_SERVER': 'WEBHCAT',
    'NAGIOS_SERVER': 'NAGIOS',
    'GANGLIA_SERVER': 'GANGLIA',
    'GANGLIA_MONITOR': 'GANGLIA',
    'KERBEROS_SERVER': 'KERBEROS',
    'KERBEROS_ADMIN_CLIENT': 'KERBEROS',
    'KERBEROS_CLIENT': 'KERBEROS',
    'HUE_SERVER': 'HUE',
    'HCFS_CLIENT': 'HCFS'
  },

  map: function (json) {
    console.time('App.statusMapper execution time');
    if (json.items) {
      var hostsCache = App.cache['Hosts'];
      var hostStatuses = {};
      var hostComponentStatuses = {};
      var addedHostComponents = [];
      var componentServiceMap = this.get('componentServiceMap');
      var currentComponentStatuses = {};
      var currentHostStatuses = {};
      var previousHostStatuses = App.cache['previousHostStatuses'];
      var previousComponentStatuses = App.cache['previousComponentStatuses'];
      var hostComponentsOnService = {};

      json.items.forEach(function (host) {
        //update hosts, which have status changed
        if (previousHostStatuses[host.Hosts.host_name] !== host.Hosts.host_status) {
          hostStatuses[host.Hosts.host_name] = host.Hosts.host_status;
        }
        currentHostStatuses[host.Hosts.host_name] = host.Hosts.host_status;
        var hostComponentsOnHost = [];
        host.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
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
              host_id: host.Hosts.host_name,
              service_id: service
            });
            //update host-components only on adding due to Ember Data features
            if (hostsCache[host.Hosts.host_name]) hostsCache[host.Hosts.host_name].is_modified = true;
          }
          currentComponentStatuses[host_component.id] = host_component.HostRoles.state;

          //host-components to host relations
          hostComponentsOnHost.push(host_component.id);
          //host-component to service relations
          if (!hostComponentsOnService[service]) {
            hostComponentsOnService[service] = {
              host_components: [],
              running_host_components: [],
              unknown_host_components: []
            };
          }
          if (host_component.HostRoles.state === App.HostComponentStatus.started) {
            hostComponentsOnService[service].running_host_components.push(host_component.id);
          }
          if (host_component.HostRoles.state === App.HostComponentStatus.unknown) {
            hostComponentsOnService[service].unknown_host_components.push(host_component.id);
          }
          hostComponentsOnService[service].host_components.push(host_component.id);


        }, this);
        /**
         * updating relation between Host and his host-components
         */
        if (hostsCache[host.Hosts.host_name]) {
          hostsCache[host.Hosts.host_name].host_components = hostComponentsOnHost;
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
            hostComponent.deleteRecord();
            App.store.commit();
            hostComponent.get('stateManager').transitionTo('loading');
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
