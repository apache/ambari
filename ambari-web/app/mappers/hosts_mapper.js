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

App.hostsMapper = App.QuickDataMapper.create({
  model: App.Host,
  config: {
    id: 'Hosts.host_name',
    host_name: 'Hosts.host_name',
    cluster_id: 'Hosts.cluster_name',// 1
    components_key: 'host_components',
    components_type: 'array',
    components: {
      item: 'HostRoles.component_name'
    },
    rack: 'Hosts.rack_info',
    host_components_key: 'host_components',
    host_components_type: 'array',
    host_components: {
      item: 'id'
    },
    cpu: 'Hosts.cpu_count',
    memory: 'Hosts.total_mem',
    disk_info: 'Hosts.disk_info',
    disk_usage: 'disk_usage',
    health_status: 'Hosts.host_status',
    load_one: 'Hosts.load.load_one',
    load_five: 'Hosts.load.load_five',
    load_fifteen: 'Hosts.load.load_fifteen',
    cpu_usage: 'cpu_usage',
    memory_usage: 'memory_usage',
    $network_usage: 36,
    $io_usage: 39,
    last_heart_beat_time: "Hosts.last_heartbeat_time",
    os_arch: 'Hosts.os_arch',
    os_type: 'Hosts.os_type',
    ip: 'Hosts.ip'
  },
  map: function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json.items) {
      var result = [];
      json.items.forEach(function (item) {
        // Disk Usage
        if (item.metrics && item.metrics.disk && item.metrics.disk.disk_total && item.metrics.disk.disk_free) {
          var diskUsed = item.metrics.disk.disk_total - item.metrics.disk.disk_free;
          var diskUsedPercent = (100 * diskUsed) / item.metrics.disk.disk_total;
          item.disk_usage = diskUsedPercent.toFixed(1);
        }
        // CPU Usage
        if (item.metrics && item.metrics.cpu && item.metrics.cpu.cpu_system && item.metrics.cpu.cpu_user) {
          var cpuUsedPercent = item.metrics.cpu.cpu_system + item.metrics.cpu.cpu_user;
          item.cpu_usage = cpuUsedPercent.toFixed(1);
        }
        // Memory Usage
        if (item.metrics && item.metrics.memory && item.metrics.memory.mem_free && item.metrics.memory.mem_total) {
          var memUsed = item.metrics.memory.mem_total - item.metrics.memory.mem_free;
          var memUsedPercent = (100 * memUsed) / item.metrics.memory.mem_total;
          item.memory_usage = memUsedPercent.toFixed(1);
        }
        item.host_components.forEach(function (host_component) {
          host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
        }, this);
        result.push(this.parseIt(item, this.config));
      }, this);

      // console.log(this.get('model'), result);
      App.store.loadMany(this.get('model'), result);
    }
  }

});
