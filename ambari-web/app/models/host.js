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

DS.attr.transforms.object = {
  from: function(serialized) {
    return Ember.none(serialized) ? null : Object(serialized);
  },

  to: function(deserialized) {
    return Ember.none(deserialized) ? null : Object(deserialized);
  }
};

App.Host = DS.Model.extend({
  hostName: DS.attr('string'),
  publicHostName: DS.attr('string'),
  cluster: DS.belongsTo('App.Cluster'),
  components: DS.hasMany('App.Component'),
  hostComponents: DS.hasMany('App.HostComponent'),
  cpu: DS.attr('string'),
  memory: DS.attr('string'),
  diskTotal: DS.attr('number'),
  diskFree: DS.attr('number'),
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
  diskInfo: DS.attr('object'),
  loadOne:DS.attr('number'),
  loadFive:DS.attr('number'),
  loadFifteen:DS.attr('number'),

  publicHostNameFormatted: function() {
    return this.get('publicHostName').substr(0, 25) + ' ...';
  }.property('publicHostName'),
  /**
   * API return diskTotal and diskFree. Need to save their different
   */
  diskUsed: function(){
    return this.get('diskTotal') - this.get('diskFree');
  }.property('diskFree', 'diskTotal'),
  /**
   * Format diskUsed value to float with 2 digits (also convert to GB)
   */
  diskUsedFormatted: function() {
    return Math.round(this.get('diskUsed') / (1024*1024*1024) * Math.pow(10, 2)) / Math.pow(10, 2) + 'GB';
  }.property('diskUsed'),
  /**
   * Format diskTotal value to float with 2 digits (also convert to GB)
   */
  diskTotalFormatted: function() {
    return Math.round(this.get('diskTotal') / (1024*1024*1024) * Math.pow(10, 2)) / Math.pow(10, 2) + 'GB';
  }.property('diskTotal'),
  /**
   * Percent value of used disk space
   */
  diskUsage: function() {
    return (this.get('diskUsed')) / this.get('diskTotal') * 100;
  }.property('diskUsed', 'diskTotal'),
  /**
   * Format diskUsage to float with 2 digits
   */
  diskUsageFormatted: function() {
    if (isNaN(this.get('diskUsage')) || this.get('diskUsage') < 0) {
      return 'Data Unavailable';
    }
    var s = Math.round(this.get('diskUsage') * Math.pow(10, 2)) / Math.pow(10, 2);
    if (isNaN(s)) {
      s = 0;
    }
    return s + '%';
  }.property('diskUsage'),

  diskInfoBar: function() {
    if (isNaN(this.get('diskUsage')) || this.get('diskUsage') < 0) {
      return this.get('diskUsageFormatted');
    }
    return this.get('diskUsedFormatted') + '/' + this.get('diskTotalFormatted') + ' (' + this.get('diskUsageFormatted') + ' used)';
  }.property('diskUsedFormatted', 'diskTotalFormatted'),
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
    if (this.get('loadOne') != null) return this.get('loadOne').toFixed(2);
    if (this.get('loadFive') != null) return this.get('loadFive').toFixed(2);
    if (this.get('loadFifteen') != null) return this.get('loadFifteen').toFixed(2);
  }.property('loadOne', 'loadFive', 'loadFifteen'),

  healthClass: function(){
    var healthStatus = this.get('healthStatus');
    /**
     * Do nothing until load
     */
    if (!this.get('isLoaded') || this.get('isSaving')) {
    } else {
      var status;
      var masterComponents = this.get('components').filterProperty('isMaster', true);
      var masterComponentsRunning = masterComponents.everyProperty('workStatus', App.Component.Status.started);
      if (this.get('isNotHeartBeating')) {
        status = 'DEAD-YELLOW';
      } else if (masterComponentsRunning) {
        status = 'LIVE';
      } else if (masterComponents.length > 0 && !masterComponentsRunning) {
        status = 'DEAD';
      } else {
        status = 'DEAD-ORANGE';
      }
      if (status) {
        healthStatus = status;
      }
    }
    return 'health-status-' + healthStatus;
  }.property('healthStatus', 'components.@each.workStatus')
});

App.Host.FIXTURES = [];