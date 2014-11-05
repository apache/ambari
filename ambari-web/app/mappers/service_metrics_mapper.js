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
var misc = require('utils/misc');
var stringUtils = require('utils/string_utils');
var dateUtils = require('utils/date');
var previousResponse = [];

App.serviceMetricsMapper = App.QuickDataMapper.create({

  model: App.Service,
  config: {
    id: 'ServiceInfo.service_name',
    service_name: 'ServiceInfo.service_name',
    work_status: 'ServiceInfo.state',
    passive_state: 'ServiceInfo.passive_state',
    critical_alerts_count: 'ServiceInfo.critical_alerts_count',
    $rand: Math.random(),
    $alerts: [ 1, 2, 3 ],
    host_components: 'host_components',
    tool_tip_content: 'tool_tip_content',
    installed_clients: 'installed_clients',
    client_components: 'client_components',
    slave_components: 'slave_components'
  },
  hdfsConfig: {
    version: 'nameNodeComponent.host_components[0].metrics.dfs.namenode.Version',
    active_name_node_id: 'active_name_node_id',
    standby_name_node_id: 'standby_name_node_id',
    standby_name_node2_id: 'standby_name_node2_id',
    journal_nodes: 'journal_nodes',
    name_node_start_time: 'nameNodeComponent.host_components[0].metrics.runtime.StartTime',
    jvm_memory_heap_used: 'nameNodeComponent.host_components[0].metrics.jvm.HeapMemoryUsed',
    jvm_memory_heap_max: 'nameNodeComponent.host_components[0].metrics.jvm.HeapMemoryMax',
    decommission_data_nodes: 'decommission_data_nodes',
    capacity_used: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CapacityUsed',
    capacity_total: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CapacityTotal',
    capacity_remaining: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CapacityRemaining',
    dfs_total_blocks: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.BlocksTotal',
    dfs_corrupt_blocks: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.CorruptBlocks',
    dfs_missing_blocks: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.MissingBlocks',
    dfs_under_replicated_blocks: 'nameNodeComponent.host_components[0].metrics.dfs.FSNamesystem.UnderReplicatedBlocks',
    dfs_total_files: 'nameNodeComponent.host_components[0].metrics.dfs.namenode.TotalFiles',
    upgrade_status: 'nameNodeComponent.host_components[0].metrics.dfs.namenode.UpgradeFinalized',
    safe_mode_status: 'nameNodeComponent.host_components[0].metrics.dfs.namenode.Safemode',
    name_node_cpu: 'nameNodeComponent.host_components[0].metrics.cpu.cpu_wio',
    name_node_rpc: 'nameNodeComponent.host_components[0].metrics.rpc.RpcQueueTime_avg_time',
    data_nodes_started: 'data_nodes_started',
    data_nodes_installed: 'data_nodes_installed',
    data_nodes_total: 'data_nodes_total'
  },
  yarnConfig: {
    version: 'resourceManagerComponent.ServiceComponentInfo.Version',
    resource_manager_start_time: 'resourceManagerComponent.ServiceComponentInfo.StartTime',
    jvm_memory_heap_used: 'resourceManagerComponent.host_components[0].metrics.jvm.HeapMemoryUsed',
    jvm_memory_heap_max: 'resourceManagerComponent.host_components[0].metrics.jvm.HeapMemoryMax',
    containers_allocated: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AllocatedContainers',
    containers_pending: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.PendingContainers',
    containers_reserved: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.ReservedContainers',
    apps_submitted: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsSubmitted',
    apps_running: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsRunning',
    apps_pending: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsPending',
    apps_completed: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsCompleted',
    apps_killed: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsKilled',
    apps_failed: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AppsFailed',
    node_managers_count_active: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.activeNMcount',
    //node_managers_count_lost: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.lostNMcount',
    node_managers_count_unhealthy: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.unhealthyNMcount',
    node_managers_count_rebooted: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.rebootedNMcount',
    node_managers_count_decommissioned: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.decommissionedNMcount',
    allocated_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AllocatedMB',
    available_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AvailableMB',
    reserved_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.ReservedMB',
    queue: 'resourceManagerComponent.queue',
    node_managers_started: 'node_managers_started',
    node_managers_installed: 'node_managers_installed',
    node_managers_total: 'node_managers_total'
  },
  mapReduce2Config: {
    version: 'jobHistoryServerComponent.ServiceComponentInfo.Version',
    map_reduce2_clients: 'map_reduce2_clients'
  },
  mapReduceConfig: {
    version: 'jobTrackerComponent.ServiceComponentInfo.Version',
    job_tracker_start_time: 'jobTrackerComponent.ServiceComponentInfo.StartTime',
    job_tracker_heap_used: 'jobTrackerComponent.ServiceComponentInfo.HeapMemoryUsed',
    job_tracker_heap_max: 'jobTrackerComponent.ServiceComponentInfo.HeapMemoryMax',
    alive_trackers: 'alive_trackers',
    black_list_trackers: 'black_list_trackers',
    gray_list_trackers: 'gray_list_trackers',
    map_slots: 'map_slots',
    reduce_slots: 'reduce_slots',
    jobs_submitted: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.jobs_submitted',
    jobs_completed: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.jobs_completed',
    jobs_running: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.jobs_running',
    map_slots_occupied: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.occupied_map_slots',
    map_slots_reserved: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.reserved_map_slots',
    reduce_slots_occupied: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.occupied_reduce_slots',
    reduce_slots_reserved: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.reserved_reduce_slots',
    maps_running: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.running_maps',
    maps_waiting: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.waiting_maps',
    reduces_running: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.running_reduces',
    reduces_waiting: 'jobTrackerComponent.ServiceComponentInfo.jobtracker.waiting_reduces',
    trackers_decommissioned: 'jobTrackerComponent.host_components[0].metrics.mapred.jobtracker.trackers_decommissioned',
    job_tracker_cpu: 'jobTrackerComponent.host_components[0].metrics.cpu.cpu_wio',
    job_tracker_rpc: 'jobTrackerComponent.host_components[0].metrics.rpc.RpcQueueTime_avg_time',
    task_trackers_started: 'task_trackers_started',
    task_trackers_installed: 'task_trackers_installed',
    task_trackers_total: 'task_trackers_total'
  },
  hbaseConfig: {
    version: 'masterComponent.ServiceComponentInfo.Version',
    master_start_time: 'masterComponent.ServiceComponentInfo.MasterStartTime',
    master_active_time: 'masterComponent.ServiceComponentInfo.MasterActiveTime',
    average_load: 'masterComponent.ServiceComponentInfo.AverageLoad',
    regions_in_transition: 'regions_in_transition',
    revision: 'masterComponent.ServiceComponentInfo.Revision',
    heap_memory_used: 'masterComponent.ServiceComponentInfo.HeapMemoryUsed',
    heap_memory_max: 'masterComponent.ServiceComponentInfo.HeapMemoryMax',
    region_servers_started: 'region_servers_started',
    region_servers_installed: 'region_servers_installed',
    region_servers_total: 'region_servers_total'
  },
  stormConfig: {
    total_tasks: 'restApiComponent.tasksTotal',
    total_slots: 'restApiComponent.slotsTotal',
    free_slots: 'restApiComponent.slotsFree',
    used_slots: 'restApiComponent.slotsUsed',
    topologies: 'restApiComponent.topologies',
    total_executors: 'restApiComponent.executorsTotal',
    nimbus_uptime: 'restApiComponent.nimbusUptime',
    super_visors_started: 'super_visors_started',
    super_visors_installed: 'super_visors_installed',
    super_visors_total: 'super_visors_total'
  },
  flumeConfig: {
    flume_handlers_total: 'flume_handlers_total'
  },
  flumeAgentConfig: {
    name: 'HostComponentProcess.name',
    status: 'HostComponentProcess.status',
    host_id: 'HostComponentProcess.host_name',
    host_name: 'HostComponentProcess.host_name',
    channels_count: 'HostComponentProcess.channels_count',
    sources_count: 'HostComponentProcess.sources_count',
    sinks_count: 'HostComponentProcess.sinks_count'
  },

  model3: App.HostComponent,
  config3: {
    id: 'id',
    work_status: 'HostRoles.state',
    passive_state:'HostRoles.maintenance_state',
    desired_status: 'HostRoles.desired_state',
    component_name: 'HostRoles.component_name',
    host_id: 'HostRoles.host_name',
    host_name: 'HostRoles.host_name',
    stale_configs: 'HostRoles.stale_configs',
    ha_status: 'HostRoles.ha_state',
    display_name_advanced: 'display_name_advanced',
    $service_id: 'none' /* will be set outside of parse function */
  },

  map: function (json) {
    console.time('App.serviceMetricsMapper execution time');
    if (json.items) {

      // Host components
      var hostComponents = [];
      var services = App.cache['services'];
      var previousComponentStatuses = App.cache['previousComponentStatuses'];
      var previousComponentPassiveStates = App.cache['previousComponentPassiveStates'];
      var result = [];
      /**
       * services contains constructed service-components structure from components array
       */

      services.setEach('components', []);

      json.items.forEach(function (component) {
        var serviceName = component.ServiceComponentInfo.service_name;
        var service = services.findProperty('ServiceInfo.service_name', serviceName);
        if (service) {
          service.components.push(component);
        }
        component.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
          previousComponentStatuses[host_component.id] = host_component.HostRoles.state;
          previousComponentPassiveStates[host_component.id] = host_component.HostRoles.maintenance_state;
          if (host_component.HostRoles.component_name == "HBASE_MASTER") {
            this.config3.ha_status = 'metrics.hbase.master.IsActiveMaster';
          }
          var comp = this.parseIt(host_component, this.config3);
          comp.service_id = serviceName;
          hostComponents.push(comp);
        }, this);
      }, this);

      this.computeAdditionalRelations(hostComponents, services);
      //load master components to model
      App.HostComponent.find().filterProperty('isMaster').forEach(function (hostComponent) {
        if (hostComponent && !hostComponents.someProperty('id', hostComponent.get('id'))) {
          this.deleteRecord(hostComponent);
          var serviceCache = services.findProperty('ServiceInfo.service_name', hostComponent.get('service.serviceName'));
          if (serviceCache) {
            serviceCache.host_components = serviceCache.host_components.without(hostComponent.get('id'));
          }
        }
      }, this);
      App.store.loadMany(this.get('model3'), hostComponents);

      //parse service metrics from components
      services.forEach(function (item) {
        var finalJson = [];

        hostComponents.filterProperty('service_id', item.ServiceInfo.service_name).mapProperty('id').forEach(function (hostComponent) {
          if (!item.host_components.contains(hostComponent)) {
            item.host_components.push(hostComponent);
          }
        }, this);
        item.host_components.sort();

        if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HDFS") {
          finalJson = this.hdfsMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.HDFSService, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE") {
          finalJson = this.mapreduceMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.MapReduceService, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HBASE") {
          finalJson = this.hbaseMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.HBaseService, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "FLUME") {
          finalJson = this.flumeMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.FlumeService, finalJson);
          App.store.loadMany(App.FlumeAgent, finalJson.agentJsons);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "YARN") {
          finalJson = this.yarnMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.YARNService, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE2") {
          finalJson = this.mapreduce2Mapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.MapReduce2Service, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "STORM") {
          finalJson = this.stormMapper(item);
          finalJson.rand = Math.random();
          this.mapQuickLinks(finalJson, item);
          result.push(finalJson);
          App.store.load(App.StormService, finalJson);
        } else {
          finalJson = this.parseIt(item, this.config);
          finalJson.rand = Math.random();
          this.mapQuickLinks(finalJson, item);
          result.push(finalJson);
        }
      }, this);

      var stackServices = App.StackService.find().mapProperty('serviceName');
      result = misc.sortByOrder(stackServices, result);

      //load services to model
      App.store.loadMany(this.get('model'), result);
      /*if (previousResponse.length !== result.length) {
        App.store.loadMany(this.get('model'), result);
      } else {
        result.forEach(function (serviceJson) {
          var fields = ['passive_state','work_status', 'rand', 'alerts', 'quick_links', 'host_components', 'tool_tip_content', 'critical_alerts_count'];
          var service = this.get('model').find(serviceJson.id);
          var modifiedData = this.getDiscrepancies(serviceJson, previousResponse.findProperty('id', serviceJson.id), fields);
          if (modifiedData.isLoadNeeded) {
            App.store.load(this.get('model'), serviceJson);
          } else {
            for (var property in modifiedData) {
              service.set(stringUtils.underScoreToCamelCase(property), modifiedData[property]);
            }
          }
        }, this)
      }

      previousResponse = result;*/
    }
    console.timeEnd('App.serviceMetricsMapper execution time');
  },
  /**
   * compute display name of host-components
   * compute tooltip content of services by host-components
   * @param hostComponents
   * @param services
   */
  computeAdditionalRelations: function (hostComponents, services) {
    var isSecondaryNamenode = hostComponents.findProperty('component_name', 'SECONDARY_NAMENODE');
    var isRMHAEnabled = hostComponents.filterProperty('component_name', 'RESOURCEMANAGER').length > 1;
    services.setEach('tool_tip_content', '');
    // set tooltip for client-only services
    var clientOnlyServiceNames = App.get('services.clientOnly');
    clientOnlyServiceNames.forEach(function(serviceName) {
      var service = services.findProperty('ServiceInfo.service_name', serviceName);
      if (service) {
        service.tool_tip_content = Em.I18n.t('services.service.summary.clientOnlyService.ToolTip');
      }
    });
    hostComponents.forEach(function (hostComponent) {
      var service = services.findProperty('ServiceInfo.service_name', hostComponent.service_id);
      if (hostComponent) {
        // set advanced nameNode display name for HA, Active NameNode or Standby NameNode
        // this is useful on three places: 1) HDFS health status hover tooltip, 2) HDFS service summary 3) NameNode component on host detail page
        if (hostComponent.component_name === 'NAMENODE' && !isSecondaryNamenode) {
          var hdfs = this.hdfsMapper(service);
          var hostName = hostComponent.host_id;
          var activeNNText = Em.I18n.t('services.service.summary.nameNode.active');
          var standbyNNText = Em.I18n.t('services.service.summary.nameNode.standby');
          if (hdfs) {
            // active_name_node_id format : NAMENODE_c6401.ambari.apache.org
            if (hdfs.active_name_node_id && hdfs.active_name_node_id.contains(hostName)) {
              hostComponent.display_name_advanced = activeNNText;
            } else if ((hdfs.standby_name_node_id && hdfs.standby_name_node_id.contains(hostName)) || ( hdfs.standby_name_node2_id && hdfs.standby_name_node2_id.contains(hostName))) {
              hostComponent.display_name_advanced = standbyNNText;
            } else {
              hostComponent.display_name_advanced = null;
            }
          }
        } else if (hostComponent.component_name === 'HBASE_MASTER') {
          if (hostComponent.work_status === 'STARTED') {
            (hostComponent.ha_status === true || hostComponent.ha_status == 'true') ?
              hostComponent.display_name_advanced = this.t('dashboard.services.hbase.masterServer.active') :
              hostComponent.display_name_advanced = this.t('dashboard.services.hbase.masterServer.standby');
          } else {
            hostComponent.display_name_advanced = null;
          }
        } else if (hostComponent.component_name === 'RESOURCEMANAGER' && isRMHAEnabled && hostComponent.work_status === 'STARTED') {
          switch (hostComponent.ha_status) {
            case 'ACTIVE':
              hostComponent.display_name_advanced = Em.I18n.t('dashboard.services.yarn.resourceManager.active');
              break;
            case 'STANDBY':
              hostComponent.display_name_advanced = Em.I18n.t('dashboard.services.yarn.resourceManager.standby');
              break;
          }
        }
        if (service) {
          if (hostComponent.display_name_advanced) {
            service.tool_tip_content += hostComponent.display_name_advanced + " " + App.HostComponentStatus.getTextStatus(hostComponent.work_status) + "<br/>";
          } else {
            service.tool_tip_content += App.format.role(hostComponent.component_name) + " " + App.HostComponentStatus.getTextStatus(hostComponent.work_status) + "<br/>";
          }
        }
      }
    }, this)
  },
  /**
   * Map quick links to services:OOZIE,GANGLIA,NAGIOS,HUE
   * @param finalJson
   * @param item
   */
  mapQuickLinks: function (finalJson, item){
    if(!(item && item.ServiceInfo)) return;
    var quickLinks = {
      OOZIE: [19],
      GANGLIA: [20],
      NAGIOS: [21],
      HUE: [22],
      STORM: [31],
      FALCON: [32]
    };
    if (quickLinks[item.ServiceInfo.service_name])
      finalJson.quick_links = quickLinks[item.ServiceInfo.service_name];
  },

  hdfsMapper: function (item) {
    var finalConfig = jQuery.extend({}, this.config);
    // Change the JSON so that it is easy to map
    var hdfsConfig = this.hdfsConfig;
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "NAMENODE") {
        //enabled HA
        if ( component.host_components.length == 2) {
          var haState1;
          var haState2;
          if (component.host_components[1].metrics && component.host_components[1].metrics.dfs) {
            haState2 = component.host_components[1].metrics.dfs.FSNamesystem.HAState;
          }
          if (component.host_components[0].metrics && component.host_components[0].metrics.dfs) {
            haState1 = component.host_components[0].metrics.dfs.FSNamesystem.HAState;
          }
          var active_name_node = [];
          var standby_name_nodes = [];
          switch (haState1) {
            case "active":
              active_name_node.push(component.host_components[0].HostRoles.host_name);
              break;
            case "standby":
              standby_name_nodes.push(component.host_components[0].HostRoles.host_name);
              break;
          }
          switch (haState2) {
            case "active":
              active_name_node.push(component.host_components[1].HostRoles.host_name);
              break;
            case "standby":
              standby_name_nodes.push(component.host_components[1].HostRoles.host_name);
              break;
          }
          item.active_name_node_id = null;
          item.standby_name_node_id = null;
          item.standby_name_node2_id = null;
          switch (active_name_node.length) {
            case 1:
              item.active_name_node_id = 'NAMENODE' + '_' + active_name_node[0];
              break;
          }
          switch (standby_name_nodes.length) {
            case 1:
              item.standby_name_node_id = 'NAMENODE' + '_' + standby_name_nodes[0];
              break;
            case 2:
              item.standby_name_node_id = 'NAMENODE' + '_' + standby_name_nodes[0];
              item.standby_name_node2_id = 'NAMENODE' + '_' + standby_name_nodes[1];
              break;
          }
          // important: active nameNode always at host_components[0]; if no active, then any nameNode could work.
          if (haState2 == "active") { // change places for all model bind with host_component[0]
            var tmp = component.host_components[1];
            component.host_components[1] = component.host_components[0];
            component.host_components[0] = tmp;
          }
        }
        item.nameNodeComponent = component;
        finalConfig = jQuery.extend(finalConfig, hdfsConfig);
        // Get the live, dead & decommission nodes from string json
        if (component.host_components[0].metrics && component.host_components[0].metrics.dfs && component.host_components[0].metrics.dfs.namenode) {
          var decommissionNodesJson = App.parseJSON(component.host_components[0].metrics.dfs.namenode.DecomNodes);
        }
        item.decommission_data_nodes = [];
        for (var host in decommissionNodesJson) {
          item.decommission_data_nodes.push('DATANODE'+ '_' + host);
        }
      }
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "JOURNALNODE") {
        if (!item.journal_nodes) {
          item.journal_nodes = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            item.journal_nodes.push(hc.HostRoles.host_name);
          });
        }
      }
    });
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.quick_links = [1, 2, 3, 4];

    return finalJson;
  },
  yarnMapper: function (item) {
    var result = [];
    var self = this;
    var finalConfig = jQuery.extend({}, this.config);
    // Change the JSON so that it is easy to map
    var yarnConfig = this.yarnConfig;
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "RESOURCEMANAGER") {
        item.resourceManagerComponent = component;

        // if YARN has two host components, ACTIVE one should be first in component.host_components array for proper metrics mapping
        if (component.host_components.length === 2) {
          var activeRM = component.host_components.findProperty('HostRoles.ha_state', 'ACTIVE');
          // if "second" RM isn't STARTED his ha_status is null (not STANDBY)
          var standbyRM = component.host_components.filter(function(host_component) {return host_component.HostRoles.ha_state !== 'ACTIVE';})[0];
          if (activeRM && standbyRM) {
            component.host_components = [activeRM, standbyRM];
          }
        }

        if (component.host_components[0].metrics && component.host_components[0].metrics.yarn) {
          var root = component.host_components[0].metrics.yarn.Queue.root;
          component.queue = JSON.stringify({
            'root': self.parseObject(root)
          });
        }
        // extend config
        finalConfig = jQuery.extend(finalConfig, yarnConfig);
      }
    });
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.quick_links = [ 23, 24, 25, 26 ];
    return finalJson;
  },

  parseObject: function(obj) {
    var res = {};
    for (var p in obj) {
      if (obj.hasOwnProperty(p)) {
        if (obj[p] instanceof Object) {
          res[p] = this.parseObject(obj[p]);
        }
      }
    }
    return res;
  },

  mapreduce2Mapper: function (item) {
    var result = [];
    var finalConfig = jQuery.extend({}, this.config);
    // Change the JSON so that it is easy to map
    var mapReduce2Config = this.mapReduce2Config;
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "HISTORYSERVER") {
        item.jobHistoryServerComponent = component;
        finalConfig = jQuery.extend(finalConfig, mapReduce2Config);
      }
    });
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.quick_links = [27, 28, 29, 30];

    return finalJson;
  },
  mapreduceMapper: function (item) {
    // Change the JSON so that it is easy to map
    var result = [];
    var finalConfig = jQuery.extend({}, this.config);
    var mapReduceConfig = this.mapReduceConfig;
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "JOBTRACKER") {
        item.jobTrackerComponent = component;
        finalConfig = jQuery.extend(finalConfig, mapReduceConfig);
        // Get the live, gray & black nodes from string json
        item.map_slots = 0;
        item.reduce_slots = 0;
        var liveNodesJson = App.parseJSON(component.ServiceComponentInfo.AliveNodes);
        var grayNodesJson = App.parseJSON(component.ServiceComponentInfo.GrayListedNodes);
        var blackNodesJson = App.parseJSON(component.ServiceComponentInfo.BlackListedNodes);
        item.alive_trackers = [];
        item.gray_list_trackers = [];
        item.black_list_trackers = [];
        if (liveNodesJson != null) {
          liveNodesJson.forEach(function (nj) {
            item.alive_trackers.push('TASKTRACKER' + '_' + nj.hostname);
            if (nj.slots && nj.slots.map_slots)
              item.map_slots += nj.slots.map_slots;
            if (nj.slots && nj.slots.map_slots_used)
              item.map_slots_used += nj.slots.map_slots_used;
            if (nj.slots && nj.slots.reduce_slots)
              item.reduce_slots += nj.slots.reduce_slots;
            if (nj.slots && nj.slots.reduce_slots_used)
              item.reduce_slots_used += nj.slots.reduce_slots_used;
          });
        }
        if (grayNodesJson != null) {
          grayNodesJson.forEach(function (nj) {
            item.gray_list_trackers.push('TASKTRACKER' + '_' + nj.hostname);
          });
        }
        if (blackNodesJson != null) {
          blackNodesJson.forEach(function (nj) {
            item.black_list_trackers.push('TASKTRACKER' + '_' + nj.hostname);
          });
        }
      } else if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "HISTORYSERVER") {
        item.jobHistoryServerComponent = component;
        finalConfig = jQuery.extend(finalConfig, mapReduceConfig);
      }
    });
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.quick_links = [5, 6, 7, 8, 9, 10, 11, 12];
    return finalJson;
  },
  hbaseMapper: function (item) {
    // Change the JSON so that it is easy to map
    var finalConfig = jQuery.extend({}, this.config);
    var hbaseConfig = this.hbaseConfig;
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "HBASE_MASTER") {
        item.masterComponent = component;
        finalConfig = jQuery.extend(finalConfig, hbaseConfig);
        var regionsArray = App.parseJSON(component.ServiceComponentInfo.RegionsInTransition);
        //regions_in_transition can have various type of value: null, array or int
        if (Array.isArray(regionsArray)) {
          item.regions_in_transition = regionsArray.length;
        } else {
          item.regions_in_transition = regionsArray == null ? 0 : regionsArray;
        }
      }
    });
    // Map
    var finalJson = this.parseIt(item, finalConfig);
    finalJson.average_load = parseFloat(finalJson.average_load).toFixed(2);
    finalJson.quick_links = [13, 14, 15, 16, 17, 18];
    return finalJson;
  },
  
  /**
   * Flume is different from other services, in that the important
   * data is in customizable channels. Hence we directly transfer
   * data into the JSON object.
   */
  flumeMapper: function (item) {
    var self = this;
    var finalConfig = jQuery.extend({}, this.config);
    var flumeConfig = this.flumeConfig;
    finalConfig = jQuery.extend(finalConfig, flumeConfig);
    var finalJson = self.parseIt(item, finalConfig);
    var flumeHandlers = item.components.findProperty('ServiceComponentInfo.component_name', "FLUME_HANDLER");
    flumeHandlers = flumeHandlers ? flumeHandlers.host_components : [];
    finalJson.agents = [];
    finalJson.agentJsons = [];
    flumeHandlers.forEach(function(flumeHandler){
      var hostName = flumeHandler.HostRoles.host_name;
      flumeHandler.processes.forEach(function(process){
        var agentJson = self.parseIt(process, self.flumeAgentConfig);
        var agentId = agentJson.name + "-" + hostName;
        finalJson.agents.push(agentId);
        agentJson.id = agentId;
        finalJson.agentJsons.push(agentJson);
      });
    });
    return finalJson;
  },

  /**
   * Storm mapper
   */
  stormMapper: function(item) {
    var finalConfig = jQuery.extend({}, this.config);
    var stormConfig = this.stormConfig;
    var metricsInfoComponent = /^2.1/.test(App.get('currentStackVersionNumber')) ? 'STORM_REST_API' : 'STORM_UI_SERVER';
    var metricsPath = {
      STORM_REST_API: 'metrics.api.cluster.summary',
      STORM_UI_SERVER: 'metrics.api.v1.cluster.summary'
    }[metricsInfoComponent];

    item.components.forEach(function(component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == metricsInfoComponent) {
        if (Em.get(component, metricsPath)) {
          item.restApiComponent = App.keysDottedToCamelCase(Em.get(component, metricsPath));
          if (metricsInfoComponent == 'STORM_UI_SERVER') {
            item.restApiComponent.topologies = Em.get(component, 'metrics.api.v1.topology.summary.length');
          } else {
            item.restApiComponent.nimbusUptime = dateUtils.timingFormat(item.restApiComponent.nimbusUptime * 1000);
          }
        }
        finalConfig = jQuery.extend({}, finalConfig, stormConfig);
      }
    });
    return this.parseIt(item, finalConfig);
  }
});
