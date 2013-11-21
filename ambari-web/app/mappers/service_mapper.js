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

App.servicesMapper = App.QuickDataMapper.create({

  model: App.Service,
  config: {
    id: 'ServiceInfo.service_name',
    service_name: 'ServiceInfo.service_name',
    work_status: 'ServiceInfo.state',
    $rand: Math.random(),
    $alerts: [ 1, 2, 3 ],
    host_components: 'host_components',
    running_host_components: 'running_host_components',
    unknown_host_components: 'unknown_host_components'
  },
  hdfsConfig: {
    version: 'nameNodeComponent.host_components[0].metrics.dfs.namenode.Version',
    name_node_id: 'nameNodeComponent.host_components[0].HostRoles.host_name',
    sname_node_id: 'snameNodeComponent.host_components[0].HostRoles.host_name',
    active_name_node_id: 'active_name_node_id',
    standby_name_node_id: 'standby_name_node_id',
    standby_name_node2_id: 'standby_name_node2_id',
    journal_nodes: 'journal_nodes',
    name_node_start_time: 'nameNodeComponent.host_components[0].metrics.runtime.StartTime',
    jvm_memory_heap_used: 'nameNodeComponent.host_components[0].metrics.jvm.memHeapUsedM',
    jvm_memory_heap_committed: 'nameNodeComponent.host_components[0].metrics.jvm.memHeapCommittedM',
    live_data_nodes: 'live_data_nodes',
    dead_data_nodes: 'dead_data_nodes',
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
    name_node_rpc: 'nameNodeComponent.host_components[0].metrics.rpc.RpcQueueTime_avg_time'
  },
  yarnConfig: {
    version: 'resourceManagerComponent.ServiceComponentInfo.Version',
    resource_manager_node_id: 'resourceManagerComponent.host_components[0].HostRoles.host_name',
    node_manager_live_nodes: 'node_manager_live_nodes',
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
    //node_managers_count_lost: 'resourceManagerComponent.ServiceComponentInfo.rm_metrics.cluster.lostNMcount',
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
    job_history_server_id: 'jobHistoryServerComponent.host_components[0].HostRoles.host_name'
  },
  mapReduceConfig: {
    version: 'jobTrackerComponent.ServiceComponentInfo.Version',
    job_tracker_id: 'jobTrackerComponent.host_components[0].HostRoles.host_name',
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
    job_tracker_rpc: 'jobTrackerComponent.host_components[0].metrics.rpc.RpcQueueTime_avg_time'
  },
  hbaseConfig: {
    version: 'masterComponent.ServiceComponentInfo.Version',
    master_id: 'masterComponent.host_components[0].HostRoles.host_name',
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
    host_id: 'HostRoles.host_name',
    stale_configs: 'HostRoles.stale_configs',
    $service_id: 'none' /* will be set outside of parse function */
  },

  map: function (json) {
    if (!this.get('model')) {
      return;
    }
    console.time('App.servicesMapper execution time');
    if (json.items) {

      // Host components
      var hostComponents = [];
      var services = [];
      var previousComponentStatuses = App.cache['previousComponentStatuses'];
      /**
       * services contains constructed service-components structure from components array
       */
      json.items.forEach(function (component) {
        var service = services.findProperty('ServiceInfo.service_name', component.ServiceComponentInfo.service_name);
        var serviceName = component.ServiceComponentInfo.service_name;
        if (!service) {
          service = {
            ServiceInfo: {
              service_name: component.ServiceComponentInfo.service_name
            },
            host_components: [],
            components: []
          };
          services.push(service);
        }
        service.components.push(component);
        component.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
          previousComponentStatuses[host_component.id] = host_component.HostRoles.state;
          if (host_component.HostRoles.component_name == "HBASE_MASTER") {
            this.config3.ha_status = 'metrics.hbase.master.IsActiveMaster';
          }
          var comp = this.parseIt(host_component, this.config3);
          comp.service_id = serviceName;
          hostComponents.push(comp);
        }, this);
      }, this);

      //load master components to model
      App.store.loadMany(this.get('model3'), hostComponents);

      var result = [];

      //parse service metrics from components
      services.forEach(function (item) {
        var finalJson = [];
        var serviceData = App.cache['hostComponentsOnService'][item.ServiceInfo.service_name];
        item.host_components = serviceData.host_components;
        item.running_host_components = serviceData.running_host_components;
        item.unknown_host_components = serviceData.unknown_host_components;
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
          if (finalJson.nodeObjs) {
            finalJson.nodeObjs.forEach(function (no) {
              App.store.load(App.FlumeNode, no);
            });
          }
          App.store.load(App.FlumeService, finalJson);
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
        } else {
          finalJson = this.parseIt(item, this.config);
          finalJson.rand = Math.random();
          this.mapQuickLinks(finalJson, item);
          result.push(finalJson);
        }
      }, this);

      result = misc.sortByOrder(App.Service.servicesSortOrder, result);

      //load services to model
      App.store.loadMany(this.get('model'), result);

      var servicesMap = {};
      //calculate service statuses according to their host-components
      App.HostComponent.find().filterProperty('isMaster').forEach(function (item) {
        if (item) {
          this.countServiceComponents(item, servicesMap, servicesMap[item.get('service.id')]);
        }
      }, this);
      this.updateServicesStatus(App.Service.find(), servicesMap);
    }
    console.timeEnd('App.servicesMapper execution time');
  },

  /**
   * fill serviceMap with aggregated data of hostComponents for each service
   * @param hostComponent
   * @param servicesMap
   * @param service
   */
  countServiceComponents: function (hostComponent, servicesMap, service) {
    if (!service) {
      service = {
        everyStarted: true,
        everyStartedOrMaintenance: true,
        masterComponents: [],
        isStarted: false,
        isUnknown: false,
        isStarting: false,
        isStopped: false,
        isHbaseActive: false,
        serviceName: hostComponent.get('service.id'),
        isRunning: true,
        hdfsHealthStatus: '',
        toolTipContent: ''
      };
      servicesMap[hostComponent.get('service.id')] = service;
    }
    if (hostComponent.get('isMaster')) {
      if (service.everyStartedOrMaintenance) {
        service.everyStartedOrMaintenance = (((hostComponent.get('componentName') === 'NAMENODE' && !App.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE')) || hostComponent.get('componentName') === 'JOURNALNODE') && App.HDFSService.find().filterProperty('activeNameNode.hostName').length > 0)
          ? true : service.everyStartedOrMaintenance = ([App.HostComponentStatus.started, App.HostComponentStatus.maintenance].contains(hostComponent.get('workStatus')));
      } else {
        service.everyStartedOrMaintenance = false;
      }
      service.everyStarted = (service.everyStarted)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.started)
        : false;
      service.isStarted = (!service.isStarted)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.started)
        : true;
      service.isUnknown = (!service.isUnknown)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.unknown)
        : true;
      service.isStarting = (!service.isStarting)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.starting)
        : true;
      service.isStopped = (!service.isStopped)
        ? (hostComponent.get('workStatus') === App.HostComponentStatus.stopped)
        : true;
      service.isHbaseActive = (!service.isHbaseActive)
        ? (hostComponent.get('haStatus') === 'true')
        : true;

      service.masterComponents.push(hostComponent);

      // set advanced nameNode display name for HA, active or standby NameNode
      // this is useful on three places: hdfs health status hover tooltip, hdfs service summary and NN component on host detail page
      if (hostComponent.get('componentName') === 'NAMENODE' && !App.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE')) {
        var hostName = hostComponent.get('host.hostName');
        var services = App.Service.find();
        var hdfs;
        services.forEach(function (item) {
          if (item.get("serviceName") == "HDFS") {
            hdfs = App.HDFSService.find(item.get('id'));
          }
        }, this);
        var activeNNText = Em.I18n.t('services.service.summary.nameNode.active');
        var standbyNNText = Em.I18n.t('services.service.summary.nameNode.standby');
        if (hdfs) {
          if (hdfs.get('activeNameNode') && hdfs.get('activeNameNode').get('hostName')) {
            var activeHostname = hdfs.get('activeNameNode').get('hostName');
          }
          if (hdfs.get('standbyNameNode') && hdfs.get('standbyNameNode').get('hostName')) {
            var standbyHostname1 = hdfs.get('standbyNameNode').get('hostName');
          }
          if (hdfs.get('standbyNameNode2') && hdfs.get('standbyNameNode2').get('hostName')) {
            var standbyHostname2 = hdfs.get('standbyNameNode2').get('hostName');
          }
          if (hostName == activeHostname) {
            hostComponent.set('displayNameAdvanced', activeNNText);
          } else if (hostName == standbyHostname1 || hostName == standbyHostname2) {
            hostComponent.set('displayNameAdvanced', standbyNNText);
          } else {
            hostComponent.set('displayNameAdvanced', null);
          }
        }
      } else if (hostComponent.get('componentName') === 'HBASE_MASTER') {
        if (hostComponent.get('workStatus') === 'STARTED') {
          hostComponent.get('haStatus') == 'true' ? hostComponent.set('displayNameAdvanced', this.t('dashboard.services.hbase.masterServer.active')) : hostComponent.set('displayNameAdvanced', this.t('dashboard.services.hbase.masterServer.standby'));
        } else {
          hostComponent.set('displayNameAdvanced', null);
        }
      }

      if (hostComponent.get("displayNameAdvanced")) {
        service.toolTipContent += hostComponent.get("displayNameAdvanced") + " " + hostComponent.get("componentTextStatus") + "<br/>";
      } else {
        service.toolTipContent += hostComponent.get("displayName") + " " + hostComponent.get("componentTextStatus") + "<br/>";
      }

    }

    if (hostComponent.get('workStatus') !== App.HostComponentStatus.stopped &&
      hostComponent.get('workStatus') !== App.HostComponentStatus.install_failed &&
      hostComponent.get('workStatus') !== App.HostComponentStatus.unknown &&
      hostComponent.get('workStatus') !== App.HostComponentStatus.maintenance) {
      service.isRunning = false;
    }
  },

  /**
   * compute service status and properties by servicesMap of hostComponents
   * @param services
   * @param servicesMap
   */
  updateServicesStatus: function(services, servicesMap){
    services.forEach(function(_service){
      var service = servicesMap[_service.get('id')];
      if (service) {
        var serviceName = _service.get('serviceName');
        var serviceSpecificObj = null;
        switch (serviceName) {
          case "HDFS":
            serviceSpecificObj = App.HDFSService.find(_service.get('id'));
            break;
          case "YARN":
            serviceSpecificObj = App.YARNService.find(_service.get('id'));
            break;
          case "MAPREDUCE":
            serviceSpecificObj = App.MapReduceService.find(_service.get('id'));
            break;
          case "HBASE":
            serviceSpecificObj = App.HBaseService.find(_service.get('id'));
            break;
        }
        //computation of service health status
        var isGreen = serviceName === 'HBASE' && App.supports.multipleHBaseMasters ? service.isStarted : service.everyStartedOrMaintenance;
        if (isGreen) {
          _service.set('healthStatus', 'green');
          if (serviceSpecificObj != null) {
            serviceSpecificObj.set('healthStatus', 'green');
          }
        } else if (service.isUnknown) {
          _service.set('healthStatus', 'yellow');
          if (serviceSpecificObj != null) {
            serviceSpecificObj.set('healthStatus', 'yellow');
          }
        } else if (service.isStarting) {
          _service.set('healthStatus', 'green-blinking');
          if (serviceSpecificObj != null) {
            serviceSpecificObj.set('healthStatus', 'green-blinking');
          }
        } else if (service.isStopped) {
          _service.set('healthStatus', 'red');
          if (serviceSpecificObj != null) {
            serviceSpecificObj.set('healthStatus', 'red');
          }
        } else {
          _service.set('healthStatus', 'red-blinking');
          if (serviceSpecificObj != null) {
            serviceSpecificObj.set('healthStatus', 'red-blinking');
          }
        }

        if (serviceName === 'HBASE' && App.supports.multipleHBaseMasters) {
          if (!service.isHbaseActive) {
            _service.set('healthStatus', 'red');
            if (serviceSpecificObj != null) {
              serviceSpecificObj.set('healthStatus', 'red');
            }
          }
        }

        _service.set('isStarted', service.everyStarted);
        _service.set('isStopped', service.isRunning);
        _service.set('toolTipContent', service.toolTipContent);
        if (serviceSpecificObj != null) {
          serviceSpecificObj.set('isStarted', service.everyStarted);
          serviceSpecificObj.set('isStopped', service.isRunning);
          serviceSpecificObj.set('toolTipContent', service.toolTipContent);
        }
      }
    }, this);
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
              item.active_name_node_id = active_name_node[0];
              break;
          }
          switch (standby_name_nodes.length) {
            case 1:
              item.standby_name_node_id = standby_name_nodes[0];
              break;
            case 2:
              item.standby_name_node_id = standby_name_nodes[0];
              item.standby_name_node2_id = standby_name_nodes[1];
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
          var liveNodesJson = App.parseJSON(component.host_components[0].metrics.dfs.namenode.LiveNodes);
          var deadNodesJson = App.parseJSON(component.host_components[0].metrics.dfs.namenode.DeadNodes);
          var decommissionNodesJson = App.parseJSON(component.host_components[0].metrics.dfs.namenode.DecomNodes);
        }
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
