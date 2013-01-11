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
var stringUtils = require('utils/string_utils');

App.servicesMapper = App.QuickDataMapper.create({
  servicesSortOrder: [
    'HDFS',
    'MAPREDUCE',
    'HBASE',
    'HIVE',
    'HCATALOG',
    'WEBHCAT',
    'OOZIE',
    'GANGLIA',
    'NAGIOS',
    'ZOOKEEPER',
    'PIG',
    'SQOOP'
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
    $service_audit: [ 1, 2, 3 ],
    $alerts: [ 1, 2, 3 ],
    components_key: 'components',
    components_type: 'array',
    components: {
      item: 'ServiceComponentInfo.component_name'
    },
    host_components: 'host_components'
  },
  hdfsConfig: {
    version: 'nameNodeComponent.ServiceComponentInfo.Version',
    name_node_id: 'nameNodeComponent.host_components[0].HostRoles.host_name',
    sname_node_id: 'snameNodeComponent.host_components[0].HostRoles.host_name',
    data_nodes: 'data_nodes',
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
    safe_mode_status: 'nameNodeComponent.ServiceComponentInfo.Safemode'
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
    trackers_decommissioned: 'jobTrackerComponent.host_components[0].metrics.mapred.jobtracker.trackers_decommissioned'
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

  model2: App.Component,
  config2: {
    id: 'ServiceComponentInfo.component_name',
    component_name: 'ServiceComponentInfo.component_name',
    service_id: 'ServiceComponentInfo.service_name',
    // TODO - PROBLEM:
    // below statements are a problem for multiple 
    // host_component components. Because it randomly
    // picks one of the hosts. Especially the host details
    // page must be careful because, it will randomly
    // pick a host.
    work_status: 'host_components[0].HostRoles.state',
    host_id: 'host_components[0].HostRoles.host_name'
  },

  model3: App.HostComponent,
  config3: {
    id: 'id',
    work_status: 'HostRoles.state',
    component_name: 'HostRoles.component_name',
    host_id: 'HostRoles.host_name',
    service_id: 'component[0].ServiceComponentInfo.service_name'
  },

  map: function (json) {
    if (!this.get('model')) {
      return;
    }

    if (json.items) {
      var result = [];
      json.items.forEach(function (item) {
        var finalConfig = jQuery.extend({}, this.config);
        var finalJson = [];
        item.host_components = [];
//        var json = this.parseIt(item, this.config);
//        if(App.Service.find().someProperty('id', json.id)){
//          var fieldsToUpdate = [
//            'work_status',
//            'components',
//            'host_components'
//          ]
//          this.updateRecord(App.Service.find().findProperty('id', json.id), json, fieldsToUpdate);
//          return;
//        }

        if (this.get('model').find(item.ServiceInfo.service_name).get('isLoaded')) {
          this.update(item);
          return;
        }
        item.components.forEach(function (component) {
          component.host_components.forEach(function (host_component) {
            host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
            item.host_components.push(host_component.id);
          }, this)
        }, this);

        if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HDFS") {
          // Change the JSON so that it is easy to map
          finalJson = this.hdfsMapper(item);
          result.push(finalJson);
          App.store.load(App.HDFSService, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE") {
          finalJson = this.mapreduceMapper(item);
          result.push(finalJson);
          App.store.load(App.MapReduceService, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HBASE") {
          finalJson = this.hbaseMapper(item);
          result.push(finalJson);
          App.store.load(App.HBaseService, finalJson);
        } else {
          result.push(this.parseIt(item, this.config));
        }
      }, this);


      result = this.sortByOrder(this.get('servicesSortOrder'), result);
      App.store.loadMany(this.get('model'), result);

      // Service components
      result = [];
      json.items.forEach(function(item){
        item.components.forEach(function(component){
          result.push(this.parseIt(component, this.config2));
        }, this)
      }, this);
      var newComponents = [];
      result.forEach(function(componentJson){
        var component = App.Component.find(componentJson.id);
        if (component && component.get('isLoaded')) { // UPDATE
          if (componentJson.work_status) {
            component.set('workStatus', componentJson.work_status);
          }
          if (componentJson.host_id) {
            component.set('host', App.Host.find(componentJson.host_id));
          }
        } else {
          newComponents.push(componentJson);
        }
      });
      if (newComponents.length > 0) {
        App.store.loadMany(this.get('model2'), newComponents);
      }

      // Host components
      result = [];
      json.items.forEach(function(item){
        item.components.forEach(function(component){
          component.host_components.forEach(function(host_component){
            result.push(this.parseIt(host_component, this.config3));
          }, this)
        }, this)
      }, this);
      var newHostComponents = [];
      result.forEach(function(hcJson){
        hcJson.id = hcJson.component_name + '_' + hcJson.host_id;
        var component = App.HostComponent.find(hcJson.id);
        if (component && component.get('isLoaded')) { // UPDATE
          if (hcJson.work_status) {
            component.set('workStatus', hcJson.work_status);
          }
          if (hcJson.component_name) {
            component.set('componentName', hcJson.component_name);
          }
          if (hcJson.host_id) {
            component.set('host', App.Host.find(hcJson.host_id));
          }
          if (hcJson.service_id) {
            component.set('service', App.Service.find(hcJson.service_id));
          }
        } else {
          newHostComponents.push(hcJson);
        }
      });
      if (newHostComponents.length > 0) {
        App.store.loadMany(this.get('model3'), newHostComponents);
      }
    }
  },
  update: function (service) {
    var finalJson;
    switch (service.ServiceInfo.service_name) {
      case 'HDFS':
        finalJson = this.hdfsMapper(service);
        App.HDFSService.find().map(function (e) {
          e.set("version", finalJson.version);
          e.set("nameNodeStartTime", finalJson.name_node_start_time);
          e.set("jvmMemoryHeapCommitted", finalJson.jvm_memory_heap_committed);
          e.set("jvmMemoryHeapUsed", finalJson.jvm_memory_heap_used);
          e.set("capacityUsed", finalJson.capacity_used);
          e.set("capacityTotal", finalJson.capacity_total);
          e.set("dfsTotalBlocks", finalJson.dfs_total_blocks);
          e.set("dfsCorruptBlocks", finalJson.dfs_corrupt_blocks);
          e.set("dfsMissingBlocks", finalJson.dfs_missing_blocks);
          e.set("dfsUnderReplicatedBlocks", finalJson.dfs_under_replicated_blocks);
          e.set("dfsTotalFiles", finalJson.dfs_total_files);
          e.set("upgradeStatus", finalJson.upgrade_status);
          e.set("safeModeStatus", finalJson.safe_mode_status);
          // Live data nodes
          var dataNodes = e.get('liveDataNodes');
          if (dataNodes) {
            dataNodes.clear();
            finalJson.live_data_nodes.forEach(function (ldn) {
              dataNodes.pushObject(App.Host.find(ldn));
            });
          }
          // Decommission data nodes
          dataNodes = e.get('decommissionDataNodes');
          if (dataNodes) {
            dataNodes.clear();
            finalJson.decommission_data_nodes.forEach(function (ldn) {
              dataNodes.pushObject(App.Host.find(ldn));
            });
          }
          // Dead data nodes
          dataNodes = e.get('deadDataNodes');
          if (dataNodes) {
            dataNodes.clear();
            finalJson.dead_data_nodes.forEach(function (ldn) {
              dataNodes.pushObject(App.Host.find(ldn));
            });
          }
        });
        break;
      case 'MAPREDUCE':
        finalJson = this.mapreduceMapper(service);
        App.MapReduceService.find().map(function (e) {
          e.set("version", finalJson.version);
          e.set("jobTrackerStartTime", finalJson.job_tracker_start_time);
          e.set("jobTrackerHeapUsed", finalJson.job_tracker_heap_used);
          e.set("jobTrackerHeapMax", finalJson.job_tracker_heap_max);
          e.set("mapSlots", finalJson.map_slots);
          e.set("reduceSlots", finalJson.reduce_slots);
          e.set("jobsSubmitted", finalJson.jobs_submitted);
          e.set("jobsCompleted", finalJson.jobs_completed);
          e.set("mapSlotsOccupied", finalJson.map_slots_occupied);
          e.set("mapSlotsReserved", finalJson.map_slots_reserved);
          e.set("reduceSlotsOccupied", finalJson.reduce_slots_occupied);
          e.set("reduceSlotsReserved", finalJson.reduce_slots_reserved);
          e.set("mapsRunning", finalJson.maps_running);
          e.set("mapsWaiting", finalJson.maps_waiting);
          e.set("reducesRunning", finalJson.reduces_running);
          e.set("reducesWaiting", finalJson.reduces_waiting);
          e.set("trackersDecommissioned", finalJson.trackers_decommissioned);
        })
        break;
      case 'HBASE':
        finalJson = this.hbaseMapper(service);
        App.HBaseService.find().map(function (e) {
          e.set("version", finalJson.version);
          e.set("masterStartTime", finalJson.master_start_time);
          e.set("masterActiveTime", finalJson.master_active_time);
          e.set("averageLoad", finalJson.average_load);
          e.set("regionsInTransition", finalJson.regions_in_transition);
          e.set("revision", finalJson.revision);
          e.set("heapMemoryUsed", finalJson.heap_memory_used);
          e.set("heapMemoryMax", finalJson.heap_memory_max);
        })
        break;
      default:
        this.otherMapper(service);
    }
  },
  otherMapper: function (item) {
    var result = [];
    if (App.Service.find(item.ServiceInfo.service_name).get("serviceName") == item.ServiceInfo.service_name) {
      //update other service
    }
  },
  hdfsMapper: function (item) {
    var result = [];
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
    var result = [];
    var finalConfig = jQuery.extend({}, this.config);
    var hbaseConfig = this.hbaseConfig;
    item.components.forEach(function (component) {
      if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "HBASE_MASTER") {
        item.masterComponent = component;
        finalConfig = jQuery.extend(finalConfig, hbaseConfig);
        var regionsArray = App.parseJSON(component.ServiceComponentInfo.RegionsInTransition);
        item.regions_in_transition = regionsArray == null ? 0 : regionsArray.length;
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
    finalJson.quick_links = [13, 14, 15, 16, 17, 18];
    return finalJson;
  },
  updateRecord: function (record, json, fieldsToUpdate) {
    for (var field in fieldsToUpdate) {
      if (json[field] !== undefined) {
        if(json[field] instanceof Array){
          this.updateHasMany(record, stringUtils.underScoreToCamelCase(field), json[field]);
        } else {
          record.set(stringUtils.underScoreToCamelCase(field), json[field]);
        }
      }
    }
  },
  updateHasMany: function(record, field, items ){
    record.get(field).clear();
    items.forEach(function (item) {
      record.get(field).pushObject(record.get(field).type.find().findProperty('id', item));
    });
  }
});