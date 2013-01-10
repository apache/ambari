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

App.servicesMapper = App.QuickDataMapper.create({
  servicesSortOrder: [
    'HDFS',
    'MAPREDUCE',
    'HBASE',
    'HIVE',
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
      var addArray = [];
      json.items.forEach(function (item) {
        var finalConfig = jQuery.extend({}, this.config);
        var finalJson, commonJson;
        item.host_components = [];
        item.components.forEach(function (component) {
          component.host_components.forEach(function (host_component) {
            host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
            item.host_components.push(host_component.id);
          }, this)
        }, this);
        commonJson = this.parseIt(item, this.config);
        if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HDFS") {
          // Change the JSON so that it is easy to map
          finalJson = this.hdfsMapper(item);
          this.updateServices(commonJson, finalJson, addArray, App.HDFSService);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE") {
          finalJson = this.mapreduceMapper(item);
          this.updateServices(commonJson, finalJson, addArray, App.MapReduceService);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HBASE") {
          finalJson = this.hbaseMapper(item);
          this.updateServices(commonJson, finalJson, addArray, App.HBaseService);
        } else {
          finalJson = commonJson;
          this.updateServices(commonJson, finalJson, addArray);
        }
      }, this);

      addArray = this.sortByOrder(this.get('servicesSortOrder'), addArray);
      App.store.loadMany(this.get('model'), addArray);

      addArray = [];
      json.items.forEach(function (item) {
        item.components.forEach(function (component) {
          var finalJson = this.parseIt(component, this.config2);
          this.updateHostsComponents(finalJson, addArray, this.get('model2'));
        }, this)
      }, this);

      App.store.loadMany(this.get('model2'), addArray);

      addArray = [];
      json.items.forEach(function (item) {
        item.components.forEach(function (component) {
          component.host_components.forEach(function (host_component) {
            var finalJson = this.parseIt(component, this.config2);
            this.updateHostsComponents(finalJson, addArray, this.get('model3'));
          }, this)
        }, this)
      }, this);
      App.store.loadMany(this.get('model3'), addArray);
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
  /**
   * update or add records for service model
   * if model defined updates Service model  and particular model that defined
   * @param json
   * @param addArray
   * @param model
   */
  updateServices: function (commonJson, json, addArray, model) {
    var commonOldItem = this.get('model').find().filterProperty('id', json.id);
    var oldItem = (model) ? model.find().filterProperty('id', json.id) : [];
    if(commonOldItem.length){
      for (var field in json) {
        if (field !== 'id') {
//          if(commonJson.hasOwnProperty(field)){
//            (json[field] instanceof Array)? this.setHasMany(field, json[field], commonOldItem[0]) : commonOldItem[0].set(field, json[field]);
//          }
          if(commonJson.hasOwnProperty(field)){
            commonOldItem[0].set(field, json[field]);
          }
          if(oldItem.length){
            oldItem[0].set(field, json[field]);
          }
        }
      }
    } else {
      addArray.push(commonJson);
      if(model){
        App.store.load(model, json)
      }
    }
  },
  /**
   * add or update records for host and component models
   * @param json
   * @param addArray
   * @param model
   */
  updateHostsComponents: function(json, addArray, model){
    var oldItem = model.find().filterProperty('id', json.id);
    if(oldItem.length){
      for (var field in json) {
        if (field !== 'id') {
          oldItem[0].set(field, json[field]);
        }
      }
    } else {
      addArray.push(json);
    }
  },
  /**
   * update hasMany relations in model
   * @param key
   * @param value
   * @param oldItem
   */
  setHasMany:function(key, value, oldItem){
    var model;
    value.clear();
    value.forEach(function (item) {
      oldItem.pushObject(model.find(item));
    }, this);
  }

});