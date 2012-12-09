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
var misc = require('utils/misc');

App.Host = DS.Model.extend({
  hostName: DS.attr('string'),
  publicHostName: DS.attr('string'),
  cluster: DS.belongsTo('App.Cluster'),
  components: DS.hasMany('App.Component'),
  hostComponents: DS.hasMany('App.HostComponent'),
  cpu: DS.attr('string'),
  memory: DS.attr('string'),
  diskUsage: DS.attr('string'),
  osArch: DS.attr('string'),
  ip: DS.attr('string'),
  rack: DS.attr('string'),
  healthStatus: DS.attr('string'),
  cpuUsage: DS.attr('number'),
  memoryUsage: DS.attr('number'),
  networkUsage: DS.attr('number'),
  ioUsage: DS.attr('number'),
  lastHeartBeatTime: DS.attr('number'),
  osType: DS.attr("string"),
  diskInfo: DS.attr('string'),
  loadOne:DS.attr('number'),
  loadFive:DS.attr('number'),
  loadFifteen:DS.attr('number'),


  /**
   * formatted bytes to appropriate value
   */
  memoryFormatted: function () {
    return misc.formatBandwidth(this.get('memory') * 1000);
  }.property('memory'),
  /**
   * Return true if host not heartbeating last 180 seconds
   */
  isNotHeartBeating : function(){
    return ((new Date()).getTime() - this.get('lastHeartBeatTime')) > 180 * 1000;
  }.property('lastHeartBeatTime'),

  loadAvg: function() {
    if (this.get('loadOne') != null) return this.get('loadOne');
    if (this.get('loadFive') != null) return this.get('loadFive');
    if (this.get('loadFifteen') != null) return this.get('loadFifteen');
  }.property('loadOne', 'loadFive', 'loadFifteen'),

  updateHostStatus: function(){

    /**
     * Do nothing until load
     */
    if(!this.get('isLoaded')){
      return;
    }

    var components = this.get('components');
    var status;

    var masterComponents = components.filterProperty('isMaster', true);
    if(components.everyProperty('workStatus', App.Component.Status.started)){
      status = 'LIVE';
    } else if(this.get('isNotHeartBeating')){
      status = 'DEAD-YELLOW';
    } else if(masterComponents.length > 0 && !masterComponents.everyProperty('workStatus', App.Component.Status.started)){
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

App.Host.FIXTURES = [];