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
  model: App.Service,
  config: {
    id: 'ServiceInfo.service_name',
    service_name: 'ServiceInfo.service_name',
    $work_status: 'DEAD',
    $service_audit: [ 1, 2, 3 ],
    $alerts: [ 1, 2, 3 ],
    components_key: 'components',
    components_type: 'array',
    components: {
      item: 'ServiceComponentInfo.component_name'
    }
  },
  
  hdfsConfig : {
    version : 'nameNodeComponent.ServiceComponentInfo.Version',
    name_node_id: 'nameNodeComponent.host_components[0].HostRoles.host_name',
    sname_node_id: 'snameNodeComponent.host_components[0].HostRoles.host_name',
    data_nodes: 'data_nodes',
    name_node_start_time: 'nameNodeComponent.ServiceComponentInfo.StartTime',
    jvm_memory_heap_used: 'nameNodeComponent.host_components[0].metrics.jvm.memHeapUsedM',
    jvm_memory_heap_committed: 'nameNodeComponent.host_components[0].metrics.jvm.memHeapCommittedM',
    live_data_nodes: 'live_data_nodes',
    dead_data_nodes: 'dead_data_nodes',
    decommision_data_nodes: 'decommission_data_nodes',
    capacity_used: 'nameNodeComponent.ServiceComponentInfo.CapacityUsed',
    capacity_total: 'nameNodeComponent.ServiceComponentInfo.CapacityTotal',
    ///// dfsTotalBlocks: 'nameNodeComponent.host_components[0].metrics.dfs.namenode.????',
    ///// dfsCorruptBlocks: DS.attr('number'),
    ///// dfsMissingBlocks: DS.attr('number'),
    ///// dfsUnderReplicatedBlocks: DS.attr('number'),
    ///// dfsTotalFiles: DS.attr('number'),
    upgradeStatus: 'nameNodeComponent.ServiceComponentInfo.UpgradeFinalized',
    safeModeStatus: 'nameNodeComponent.ServiceComponentInfo.Safemode'
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
    reduce_slots: 'reduce_slots'
//    jobsSubmitted: DS.attr('number'),
//    jobsCompleted: DS.attr('number'),
//    mapSlotsOccupied: DS.attr('number'),
//    mapSlotsReserved: DS.attr('number'),
//    reduceSlotsOccupied: DS.attr('number'),
//    reduceSlotsReserved: DS.attr('number'),
//    mapsRunning: DS.attr('number'),
//    mapsWaiting: DS.attr('number'),
//    reducesRunning: DS.attr('number'),
//    reducesWaiting: DS.attr('number')
  },

  model2: App.Component,
  config2: {
    id: 'ServiceComponentInfo.component_name',
    component_name: 'ServiceComponentInfo.component_name',
    service_id: 'ServiceComponentInfo.service_name',
    work_status: 'host_components[0].HostRoles.state',
    host_id: 'host_components[0].HostRoles.host_name',
    $decommissioned: false
  },

  map: function (json) {
    if (!this.get('model')) {
      return;
    }

    if (json.items) {
      var result = [];
      json.items.forEach(function (item) {
        var finalConfig = jQuery.extend({}, this.config);
        if (item && item.ServiceInfo && item.ServiceInfo.service_name == "HDFS") {
          // Change the JSON so that it is easy to map
          var hdfsConfig = this.hdfsConfig;
          item.components.forEach(function (component) {
            if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "NAMENODE") {
              item.nameNodeComponent = component;
              finalConfig = jQuery.extend(finalConfig, hdfsConfig);
              // Get the live, dead & decommision nodes from string json
              var liveNodesJson = jQuery.parseJSON(component.ServiceComponentInfo.LiveNodes);
              var deadNodesJson = jQuery.parseJSON(component.ServiceComponentInfo.DeadNodes);
              var decommisionNodesJson = jQuery.parseJSON(component.ServiceComponentInfo.DecomNodes);
              item.live_data_nodes = [];
              item.dead_data_nodes = [];
              item.decommision_data_nodes = [];
              for(var ln in liveNodesJson){
                item.live_data_nodes.push(ln);
              }
              for(var dn in deadNodesJson){
                item.dead_data_nodes.push(dn);
              }
              for(var dcn in decommisionNodesJson){
                item.decommision_data_nodes.push(dcn);
              }
            }
            if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "SECONDARY_NAMENODE") {
              item.snameNodeComponent = component;
            }
            if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "DATANODE") {
              if(!item.data_nodes){
                item.data_nodes = [];
              }
              item.data_nodes.push(component.host_components[0].HostRoles.host_name);
            }
          });
          // Map
          var finalJson = this.parseIt(item, finalConfig);
          result.push(finalJson);
          App.store.load(App.HDFSService, finalJson);
        } else if (item && item.ServiceInfo && item.ServiceInfo.service_name == "MAPREDUCE") {
            // Change the JSON so that it is easy to map
            var mapReduceConfig = this.mapReduceConfig;
            item.components.forEach(function (component) {
              if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "JOBTRACKER") {
                item.jobTrackerComponent = component;
                finalConfig = jQuery.extend(finalConfig, mapReduceConfig);
                // Get the live, gray & black nodes from string json
                item.map_slots = 0;
                item.reduce_slots = 0;
                var liveNodesJson = jQuery.parseJSON(component.ServiceComponentInfo.AliveNodes);
                var grayNodesJson = jQuery.parseJSON(component.ServiceComponentInfo.GrayListedNodes);
                var blackNodesJson = jQuery.parseJSON(component.ServiceComponentInfo.BlackListedNodes);
                item.alive_trackers = [];
                item.gray_list_trackers = [];
                item.black_list_trackers = [];
                liveNodesJson.forEach(function(nj){
                  item.alive_trackers.push(nj.hostname);
                  if(nj.slots && nj.slots.map_slots)
                    item.map_slots += nj.slots.map_slots;
                  if(nj.slots && nj.slots.map_slots_used)
                    item.map_slots_used += nj.slots.map_slots_used;
                  if(nj.slots && nj.slots.reduce_slots)
                    item.reduce_slots += nj.slots.reduce_slots;
                  if(nj.slots && nj.slots.reduce_slots_used)
                    item.reduce_slots_used += nj.slots.reduce_slots_used;
                });
                grayNodesJson.forEach(function(nj){
                  item.gray_list_trackers.push(nj.hostname);
                });
                blackNodesJson.forEach(function(nj){
                  item.black_list_trackers.push(nj.hostname);
                });
              }
              if (component.ServiceComponentInfo && component.ServiceComponentInfo.component_name == "TASKTRACKER") {
                if(!item.task_trackers){
                  item.task_trackers = [];
                }
                item.task_trackers.push(component.host_components[0].HostRoles.host_name);
              }
            });
            // Map
            finalJson = this.parseIt(item, finalConfig);
            result.push(finalJson);
            App.store.load(App.MapReduceService, finalJson);
        } else {
          result.push(this.parseIt(item, this.config));
        }
      }, this);

      App.store.loadMany(this.get('model'), result);

      result = [];
      json.items.forEach(function (item) {
        item.components.forEach(function (component) {
          result.push(this.parseIt(component, this.config2));
        }, this)
      }, this);

      App.store.loadMany(this.get('model2'), result);
    }
  }
});
