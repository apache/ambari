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
  tmp_result:[],
  config: {
    id: 'Hosts.host_name',
    host_name: 'Hosts.host_name',
    public_host_name: 'Hosts.public_host_name',
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
    disk_total: 'metrics.disk.disk_total',
    disk_free: 'metrics.disk.disk_free',
    health_status: 'Hosts.host_status',
    load_one: 'metrics.load.load_one',
    load_five: 'metrics.load.load_five',
    load_fifteen: 'metrics.load.load_fifteen',
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
    var self = this;
    if (!this.get('model')) {
      return;
    }
    if (json.items) {

      json.items.forEach(function (item) {
        var result = [];
        self.tmp_result=[];

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

        if(App.Host.find(item.Hosts.host_name).get("hostName") == item.Hosts.host_name){ // UPDATE

          App.Host.find(item.Hosts.host_name).set("cpuUsage", item.cpu_usage);
          App.Host.find(item.Hosts.host_name).set("diskUsage", item.disk_usage);
          App.Host.find(item.Hosts.host_name).set("memoryUsage", item.memory_usage);

          self.tmp_result.push(this.parseIt(item, this.config));

          $.map(self.tmp_result[0], function (e,a){
            //console.log(a, "------------------", self.tmp_result[0][a])
            if(typeof(e) === "string" || typeof(e) === "number")
            {
              var modelName=self.parseName(a);
              if(typeof(App.Host.find(self.tmp_result[0].host_name).get(modelName)) !== "undefined"){
                App.Host.find(self.tmp_result[0].host_name).set(modelName, self.tmp_result[0][a]);
               // console.log(modelName, "------------------", self.tmp_result[0][a])
              }
            }
          })

        }else{ // ADD

          item.host_components.forEach(function (host_component) {
            host_component.id = host_component.HostRoles.component_name + "_" + host_component.HostRoles.host_name;
          }, this);
          result.push(this.parseIt(item, this.config));
          App.store.loadMany(this.get('model'), result);

        }
      }, this);
       //console.log(this.get('model'), result);

    }
  },

  parseName:function(name){
    var new_name = name.replace(/_\w/g,replacer);
    function replacer(str, p1, p2, offset, s)
    {
     return str[1].toUpperCase();
    }
    return new_name;
  }

});
