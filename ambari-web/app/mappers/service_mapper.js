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

App.servicesMapper = App.QuickDataMapper.create({
  servicesSortOrder: [
    'HDFS',
    'YARN',
    'MAPREDUCE',
    'MAPREDUCE2',
    'TEZ',
    'HBASE',
    'HIVE',
    'HCATALOG',
    'WEBHCAT',
    'FLUME',
    'OOZIE',
    'GANGLIA',
    'NAGIOS',
    'ZOOKEEPER',
    'PIG',
    'SQOOP',
    'HUE'
  ],
  sortByOrder: function (sortOrder, array) {
    var sorted = [];
    for (var i = 0; i < sortOrder.length; i++)
      for (var j = 0; j < array.length; j++) {
        if (sortOrder[i] == array[j].id) {
          sorted.push(array[j]);
        }
      }
    return sorted;
  },

  model: App.Service,
  config: {
    id: 'ServiceInfo.service_name',
    service_name: 'ServiceInfo.service_name',
    work_status: 'ServiceInfo.state',
    $rand: Math.random(),
    $alerts: [ 1, 2, 3 ],
    host_components: 'host_components'
  },
  hdfsConfig: {
    version: 'nameNodeComponent.ServiceComponentInfo.Version',
    name_node_id: 'nameNodeComponent.host_components[0].HostRoles.host_name',
    sname_node_id: 'snameNodeComponent.host_components[0].HostRoles.host_name',
    data_nodes: 'data_nodes',
    journal_nodes: 'journal_nodes',
    name_node_start_time: 'nameNodeComponent.ServiceComponentInfo.StartTime',
    jvm_memory_heap_used: 'nameNodeComponent.host_components[0].metrics.jvm.memHeapUsedM',
    jvm_memory_heap_committed: 'nameNodeComponent.host_components[0].metrics.jvm.memHeapCommittedM',
    live_data_nodes: 'live_data_nodes',
    dead_data_nodes: 'dead_data_nodes',
    decommission_data_nodes: 'decommission_data_nodes',
    capacity_used: 'nameNodeComponent.ServiceComponentInfo.CapacityUsed',
    capacity_total: 'nameNodeComponent.ServiceComponentInfo.CapacityTotal',
    capacity_remaining: 'nameNodeComponent.ServiceComponentInfo.CapacityRemaining',
    dfs_total_blocks: 'nameNodeComponent.ServiceComponentInfo.BlocksTotal',
    dfs_corrupt_blocks: 'nameNodeComponent.ServiceComponentInfo.CorruptBlocks',
    dfs_missing_blocks: 'nameNodeComponent.ServiceComponentInfo.MissingBlocks',
    dfs_under_replicated_blocks: 'nameNodeComponent.ServiceComponentInfo.UnderReplicatedBlocks',
    dfs_total_files: 'nameNodeComponent.ServiceComponentInfo.TotalFiles',
    upgrade_status: 'nameNodeComponent.ServiceComponentInfo.UpgradeFinalized',
    safe_mode_status: 'nameNodeComponent.ServiceComponentInfo.Safemode',
    name_node_cpu: 'nameNodeComponent.host_components[0].metrics.cpu.cpu_wio',
    name_node_rpc: 'nameNodeComponent.host_components[0].metrics.rpc.RpcQueueTime_avg_time'
  },
  yarnConfig: {
    version: 'resourceManagerComponent.ServiceComponentInfo.Version',
    resource_manager_node_id: 'resourceManagerComponent.host_components[0].HostRoles.host_name',
    node_manager_nodes: 'node_manager_nodes',
    node_manager_live_nodes: 'node_manager_live_nodes',
    yarn_client_nodes: 'yarn_client_nodes',
    resource_manager_start_time: 'resourceManagerComponent.ServiceComponentInfo.StartTime',
    jvm_memory_heap_used: 'resourceManagerComponent.host_components[0].metrics.jvm.memHeapUsedM',
    jvm_memory_heap_committed: 'resourceManagerComponent.host_components[0].metrics.jvm.memHeapCommittedM',
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
    node_managers_count_lost: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.lostNMcount',
    node_managers_count_unhealthy: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.unhealthyNMcount',
    node_managers_count_rebooted: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.rebootedNMcount',
    node_managers_count_decommissioned: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.decommissionedNMcount',
    allocated_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AllocatedMB',
    available_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.AvailableMB',
    reserved_memory: 'resourceManagerComponent.host_components[0].metrics.yarn.Queue.root.ReservedMB',
    queue: 'resourceManagerComponent.queue'
  },
  mapReduce2Config: {
    version: 'jobHistoryServerComponent.ServiceComponentInfo.Version',
    job_history_server_id: 'jobHistoryServerComponent.host_components[0].HostRoles.host_name',
    map_reduce2_clients: 'map_reduce2_clients'
  },
  mapReduceConfig: {
    version: 'jobTrackerComponent.ServiceComponentInfo.Version',
    job_tracker_id: 'jobTrackerComponent.host_components[0].HostRoles.host_name',
    task_trackers: 'task_trackers',
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
    job_tracker_rpc: 'jobTrackerComponent.host_components[0].metrics.rpc.RpcQueueTime_avg_time'
  },
  hbaseConfig: {
    version: 'masterComponent.ServiceComponentInfo.Version',
    master_id: 'masterComponent.host_components[0].HostRoles.host_name',
    region_servers: 'region_servers',
    master_start_time: 'masterComponent.ServiceComponentInfo.MasterStartTime',
    master_active_time: 'masterComponent.ServiceComponentInfo.MasterActiveTime',
    average_load: 'masterComponent.ServiceComponentInfo.AverageLoad',
    regions_in_transition: 'regions_in_transition',
    revision: 'masterComponent.ServiceComponentInfo.Revision',
    heap_memory_used: 'masterComponent.ServiceComponentInfo.HeapMemoryUsed',
    heap_memory_max: 'masterComponent.ServiceComponentInfo.HeapMemoryMax'
  },

  model3: App.HostComponent,
  config3: {
    id: 'id',
    work_status: 'HostRoles.state',
    desired_status: 'HostRoles.desired_state',
    component_name: 'HostRoles.component_name',
    ha_status: 'HostRoles.ha_status',
    host_id: 'HostRoles.host_name',
    $service_id: 'none' /* will be set outside of parse function */
  },

  map: function (json) {
    if (!this.get('model')) {
      return;
    }

    var start = new Date().getTime();
    console.log('in service mapper');

    if (json.items) {
      var result = [];
      json.items.forEach(function (item) {
        var finalConfig = jQuery.extend({}, this.config);
        var finalJson = [];
        item.host_components = [];
        item.components.forEach(function (component) {
          component.host_components.forEach(function (host_component) {
            host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
            item.host_components.push(host_component.id);
          }, this);
        }, this);
        item.host_components.sort();

        if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HDFS") {
          finalJson = this.hdfsMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.HDFSService, finalJson);
        }else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE") {
          finalJson = this.mapreduceMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.MapReduceService, finalJson);
        }else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HBASE") {
          finalJson = this.hbaseMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.HBaseService, finalJson);
        }else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "FLUME") {
          finalJson = this.flumeMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          if(finalJson.nodeObjs){
            finalJson.nodeObjs.forEach(function(no){
              App.store.load(App.FlumeNode, no);
            });
          }
          App.store.load(App.FlumeService, finalJson);
        }else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "YARN") {
          finalJson = this.yarnMapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.YARNService, finalJson);
        }else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE2") {
          finalJson = this.mapreduce2Mapper(item);
          finalJson.rand = Math.random();
          result.push(finalJson);
          App.store.load(App.MapReduce2Service, finalJson);
        }else {
          finalJson = this.parseIt(item, this.config);
          finalJson.rand = Math.random();
          this.mapQuickLinks(finalJson, item);
          result.push(finalJson);
        }
      }, this);


      result = this.sortByOrder(this.get('servicesSortOrder'), result);
      App.store.loadMany(this.get('model'), result);

      // Host components
      result = [];
      var hostComponentToActualConfigsMap = {};
      json.items.forEach(function(item){
        item.components.forEach(function(component){
          var service = component.ServiceComponentInfo.service_name;
          component.host_components.forEach(function(host_component){
            hostComponentToActualConfigsMap[host_component.id] = host_component.HostRoles.actual_configs;
            var comp = this.parseIt(host_component, this.config3);
            comp.service_id = service;
            result.push(comp);
          }, this)
        }, this)
      }, this);

      result.forEach(function(hcJson){
        this.calculateState(hcJson);
      }, this);

      var oldHostComponents = App.HostComponent.find();
      var item;
      var currentHCWithComponentNames = {};
      var currentComponentNameHostNames = {};
      for ( var i = 0; i < oldHostComponents.content.length; i++) {
        item = oldHostComponents.objectAt(i);
        if (item && !result.findProperty('id', item.get('id'))) {
          item.deleteRecord();
        } else {
          var componentName = item.get('componentName');
          if (componentName) {
            currentHCWithComponentNames[item.get('id')] = item.get('id');
          }
          if (!currentComponentNameHostNames[componentName]) {
            currentComponentNameHostNames[componentName] = [];
          }
          currentComponentNameHostNames[componentName].pushObject(item.get('host.hostName'));
        }
      }
      result.forEach(function (item) {
        if (currentHCWithComponentNames[item.id] != null && 
            !currentComponentNameHostNames[item.component_name].contains(item.host_id)) {
          item.id = (new Date).getTime();
        }
      });
      
      App.store.loadMany(this.get('model3'), result);
      for(var hostComponentId in hostComponentToActualConfigsMap){
        var hostComponentObj = App.HostComponent.find(hostComponentId);
        var actualConfigs = [];
        // Create actual_configs
        for(var site in hostComponentToActualConfigsMap[hostComponentId]){
          var tag = hostComponentToActualConfigsMap[hostComponentId][site].tag;
          var configObj = App.ConfigSiteTag.create({
            site: site,
            tag: tag,
            hostOverrides: {}
          });
          var overrides = hostComponentToActualConfigsMap[hostComponentId][site].host_overrides;
          if(overrides!=null){
            var hostOverridesArray = {};
            overrides.forEach(function(override){
              var hostname = override.host_name;
              var tag = override.tag;
              hostOverridesArray[hostname] = tag;
            });
            configObj.set('hostOverrides', hostOverridesArray);
          }
          actualConfigs.push(configObj);
        }
        hostComponentObj.set('actualConfigs', actualConfigs);
      }
    }
    console.log('out service mapper.  Took ' + (new Date().getTime() - start) + 'ms');
  },

  /**
   * Map quick links to services:OOZIE,GANGLIA,NAGIOS,HUE
   * @param finalJson
   * @param item
   */
  mapQuickLinks: function (finalJson, item){
    if(item && item.ServiceInfo && item.ServiceInfo.service_name == "OOZIE"){
      finalJson.quick_links = [19];
    }else if(item && item.ServiceInfo && item.ServiceInfo.service_name == "GANGLIA"){
      finalJson.quick_links = [20];
    }else if(item && item.ServiceInfo && item.ServiceInfo.service_name == "NAGIOS"){
      finalJson.quick_links = [21];
    }else if(item && item.ServiceInfo && item.ServiceInfo.service_name == "HUE"){
      finalJson.quick_links = [22];
    }
  },

  hdfsMapper: function (item) {
    var finalConfig = jQuery.extend({}, this.config);
    // Change the JSON so that it is easy to map
    var hdfsConfig = this.hdfsConfig;
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "NAMENODE") {
        item.nameNodeComponent = component;
        finalConfig = jQuery.extend(finalConfig, hdfsConfig);
        // Get the live, dead & decommission nodes from string json
        var liveNodesJson = App.parseJSON(component.ServiceComponentInfo.LiveNodes);
        var deadNodesJson = App.parseJSON(component.ServiceComponentInfo.DeadNodes);
        var decommissionNodesJson = App.parseJSON(component.ServiceComponentInfo.DecomNodes);
        item.live_data_nodes = [];
        item.dead_data_nodes = [];
        item.decommission_data_nodes = [];
        for (var ln in liveNodesJson) {
          item.live_data_nodes.push(ln);
        }
        for (var dn in deadNodesJson) {
          item.dead_data_nodes.push(dn);
        }
        for (var dcn in decommissionNodesJson) {
          item.decommission_data_nodes.push(dcn);
        }
      }
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "SECONDARY_NAMENODE") {
        item.snameNodeComponent = component;
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
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "DATANODE") {
        if (!item.data_nodes) {
          item.data_nodes = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            item.data_nodes.push(hc.HostRoles.host_name);
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
        // live nodes calculation
        var nmList = [];
        if (component.ServiceComponentInfo.rm_metrics && component.ServiceComponentInfo.rm_metrics.cluster && component.ServiceComponentInfo.rm_metrics.cluster.nodeManagers) {
          nmList = App.parseJSON(component.ServiceComponentInfo.rm_metrics.cluster.nodeManagers);
        }
        nmList.forEach(function (nm) {
          if (nm.State === "RUNNING") {
            if (!item.node_manager_live_nodes) {
              item.node_manager_live_nodes = [];
            }
            item.node_manager_live_nodes.push(nm.HostName);
          }
        });

        if (component.host_components[0].metrics && component.host_components[0].metrics.yarn) {
          var root = component.host_components[0].metrics.yarn.Queue.root;
          var queue = JSON.stringify({
            'root': self.parseObject(root)
          });
          component.queue = queue;
        }
        // extend config
        finalConfig = jQuery.extend(finalConfig, yarnConfig);
      }
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "NODEMANAGER") {
        if (!item.node_manager_nodes) {
          item.node_manager_nodes = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            item.node_manager_nodes.push(hc.HostRoles.host_name);
          });
        }
      }
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "YARN_CLIENT") {
        if (!item.yarn_client_nodes) {
          item.yarn_client_nodes = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            item.yarn_client_nodes.push(hc.HostRoles.host_name);
          });
        }
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
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "MAPREDUCE2_CLIENT") {
        if (!item.map_reduce2_clients) {
          item.map_reduce2_clients = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            item.map_reduce2_clients.push(hc.HostRoles.host_name);
          });
        }
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
            item.alive_trackers.push(nj.hostname);
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
            item.gray_list_trackers.push(nj.hostname);
          });
        }
        if (blackNodesJson != null) {
          blackNodesJson.forEach(function (nj) {
            item.black_list_trackers.push(nj.hostname);
          });
        }
      }
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "TASKTRACKER") {
        if (!item.task_trackers) {
          item.task_trackers = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            item.task_trackers.push(hc.HostRoles.host_name);
          });
        }
      }
    });
    // Map
    finalJson = this.parseIt(item, finalConfig);
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
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "HBASE_REGIONSERVER") {
        if (!item.region_servers) {
          item.region_servers = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            item.region_servers.push(hc.HostRoles.host_name);
          });
        }
      }
    });
    // Map
    finalJson = this.parseIt(item, finalConfig);
    finalJson.average_load = parseFloat(finalJson.average_load).toFixed(2);
    finalJson.quick_links = [13, 14, 15, 16, 17, 18];
    return finalJson;
  },
  
  /**
   * Flume is different from other services, in that the important
   * data is in customizeable channels. Hence we directly transfer 
   * data into the JSON object.
   */
  flumeMapper: function (item) {
    var finalConfig = jQuery.extend({}, this.config);
    var finalJson = this.parseIt(item, finalConfig);
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "FLUME_SERVER") {
        if (!finalJson.nodes) {
          finalJson.nodes = [];
        }
        if (!finalJson.nodeObjs) {
          finalJson.nodeObjs = [];
        }
        if (component.host_components) {
          component.host_components.forEach(function (hc) {
            var fnode = {};
            fnode.id = hc.HostRoles.host_name;
            fnode.host_id = hc.HostRoles.host_name;
            fnode.channels = "";
            fnode.sources = "";
            fnode.sinks = "";
            if (hc.metrics != null && hc.metrics.flume && hc.metrics.flume.flume && hc.metrics.flume.flume) {
              if (hc.metrics.flume.flume.CHANNEL) {
                for ( var c in hc.metrics.flume.flume.CHANNEL) {
                  if (fnode.channels.length < 1) {
                    fnode.channels += c;
                  } else {
                    fnode.channels += ("," + c);
                  }
                }
              }
              if (hc.metrics.flume.flume.SINK) {
                for ( var c in hc.metrics.flume.flume.SINK) {
                  if (fnode.sinks.length < 1) {
                    fnode.sinks += c;
                  } else {
                    fnode.sinks += ("," + c);
                  }
                }
              }
              if (hc.metrics.flume.flume.SOURCE) {
                for ( var c in hc.metrics.flume.flume.SOURCE) {
                  if (fnode.sources.length < 1) {
                    fnode.sources += c;
                  } else {
                    fnode.sources += ("," + c);
                  }
                }
              }
            }
            finalJson.nodeObjs.push(fnode);
            finalJson.nodes.push(hc.HostRoles.host_name);
          });
        }
      }
    });
    return finalJson;
  }
});
