/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


var App = require('app');

App.Host = DS.Model.extend({
  hostName: DS.attr('string'),
  cluster: DS.belongsTo('App.Cluster'),
  components: DS.hasMany('App.Component'),
  cpu: DS.attr('string'),
  memory: DS.attr('string'),
  diskUsage: DS.attr('string'),
  loadAvg: DS.attr('string'),
  os: DS.attr('string'),
  ip: DS.attr('string'),
  healthStatus: DS.attr('string'),
  cpuUsage: DS.attr('number'),
  memoryUsage: DS.attr('number'),
  networkUsage: DS.attr('number'),
  ioUsage: DS.attr('number'),
  lastHeartBeatTime: DS.attr('number'),


  /**
   * Return true if host not heartbeating last 180 seconds
   */
  isNotHeartBeating : function(){
    return ((new Date()).getTime() - this.get('lastHeartBeatTime')) > 180 * 1000;
  }.property('lastHeartBeatTime'),

  updateHostStatus: function(){

    /**
     * Do nothing until load
     */
    if(!this.get('isLoaded') || !this.get('components').everyProperty('isLoaded', true)){
      return;
    }

    var components = this.get('components');
    var status;

    var masterComponents = components.filterProperty('isMaster', true);
    if(components.everyProperty('workStatus', "STARTED")){
      status = 'LIVE';
    } else if(false && this.get('isNotHeartBeating')){ //todo uncomment on real data
      status = 'DEAD-YELLOW';
    } else if(masterComponents.length > 0 && !masterComponents.everyProperty('workStatus', "STARTED")){
      status = 'DEAD';
    } else{
      status = 'DEAD-ORANGE';
    }

    if(status){
      this.set('healthStatus', status);
     // console.log('set ' + status + ' for ' + this.get('hostName'));
    }
  }.observes('components.@each.workStatus'),

  healthClass: function(){
    return 'health-status-' + this.get('healthStatus');
  }.property('healthStatus')
});

App.Host.FIXTURES = [
  {
    id: 1,
    host_name: 'z_host1',
    cluster_id: 1,
    components:[1, 3],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '40',
    load_avg: '0.2, 1.2, 2.4',
    ip: '123.123.123.123',
    health_status: 'LIVE',
    cpu_usage: 33,
    memory_usage: 26,
    network_usage: 36,
    io_usage: 39,
    last_heart_beat_time : 1351536732366
  },
  {
    id: 2,
    host_name: 'host2',
    cluster_id: 1,
    components:[4, 5, 8],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'DEAD-ORANGE',
    cpu_usage: 36,
    memory_usage: 29,
    network_usage: 56,
    io_usage: 69,
    lastHeartBeatTime : 1351536732366
  },
  {
    id: 3,
    host_name: 'n_host3',
    cluster_id: 2,
    components:[7],
    health_status: 'DEAD-YELLOW',
    cpu_usage: 23,
    memory_usage: 16,
    network_usage: 16,
    io_usage: 49
  },
  {
    id: 4,
    host_name: 'b_host4',
    cluster_id: 2,
    components:[],
    health_status: 'DEAD',
    cpu_usage: 23,
    memory_usage: 36,
    network_usage: 46,
    io_usage: 39
  },
  {
    id: 5,
    host_name: 'host5',
    cluster_id: 1,
    components:[],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'DEAD',
    cpu_usage: 53,
    memory_usage: 16,
    network_usage: 16,
    io_usage: 29
  },
  {
    id: 6,
    host_name: 'a_host6',
    cluster_id: 1,
    components:[],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'LIVE',
    cpu_usage: 33,
    memory_usage: 26,
    network_usage: 36,
    io_usage: 39
  },
  {
    id: 7,
    host_name: 'host7',
    cluster_id: 1,
    components:[],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'LIVE',
    cpu_usage: 53,
    memory_usage: 16,
    network_usage: 16,
    io_usage: 29
  },
  {
    id: 8,
    host_name: 'host8',
    cluster_id: 1,
    components:[],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'LIVE',
    cpu_usage: 53,
    memory_usage: 11,
    network_usage: 14,
    io_usage: 27
  },
  {
    id: 9,
    host_name: 'host9',
    cluster_id: 1,
    components:[],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'LIVE',
    cpu_usage: 53,
    memory_usage: 26,
    network_usage: 56,
    io_usage: 19
  },
  {
    id: 10,
    host_name: 'host10',
    cluster_id: 1,
    components:[],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'LIVE',
    cpu_usage: 53,
    memory_usage: 16,
    network_usage: 16,
    io_usage: 29
  }
];