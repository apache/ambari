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
require('utils/config');

App.Service = DS.Model.extend({
  serviceName: DS.attr('string'),
  displayName: function() {
    return App.format.role(this.get('serviceName'), true);
  }.property('serviceName'),
  passiveState: DS.attr('string'),
  workStatus: DS.attr('string'),
  rand: DS.attr('string'),
  toolTipContent: DS.attr('string'),
  quickLinks: DS.hasMany('App.QuickLinks'),  // mapped in app/mappers/service_metrics_mapper.js method - mapQuickLinks
  hostComponents: DS.hasMany('App.HostComponent'),
  serviceConfigsTemplate: App.config.get('preDefinedServiceConfigs'),
  /**
   * used by services("OOZIE", "ZOOKEEPER", "HIVE", "MAPREDUCE2", "TEZ", "SQOOP", "PIG","FALCON")
   * that have only client components
   */
  installedClients: DS.attr('number'),

  clientComponents: DS.hasMany('App.ClientComponent'),
  slaveComponents: DS.hasMany('App.SlaveComponent'),
  masterComponents: DS.hasMany('App.MasterComponent'),

  /**
   * @type {bool}
   */
  isInPassive: function() {
    return this.get('passiveState') === "ON";
  }.property('passiveState'),

  serviceComponents: function() {
    var clientComponents = this.get('clientComponents').mapProperty('componentName');
    var slaveComponents = this.get('slaveComponents').mapProperty('componentName');
    var masterComponents = this.get('masterComponents').mapProperty('componentName');
    return clientComponents.concat(slaveComponents).concat(masterComponents);
  }.property('clientComponents.@each', 'slaveComponents.@each','masterComponents.@each'),

  // Instead of making healthStatus a computed property that listens on hostComponents.@each.workStatus,
  // we are creating a separate observer _updateHealthStatus.  This is so that healthStatus is updated
  // only once after the run loop.  This is because Ember invokes the computed property every time
  // a property that it depends on changes.  For example, App.statusMapper's map function would invoke
  // the computed property too many times and freezes the UI without this hack.
  // See http://stackoverflow.com/questions/12467345/ember-js-collapsing-deferring-expensive-observers-or-computed-properties
  healthStatus: function(){
    switch(this.get('workStatus')){
      case 'STARTED':
        return 'green';
      case 'STARTING':
        return 'green-blinking';
      case 'INSTALLED':
        return 'red';
      case 'STOPPING':
        return 'red-blinking';
      case 'UNKNOWN':
      default:
        return 'yellow';
    }
  }.property('workStatus'),
  isStopped: function () {
    return this.get('workStatus') === 'INSTALLED';
  }.property('workStatus'),
  isStarted: function () {
    return this.get('workStatus') === 'STARTED';
  }.property('workStatus'),

  /**
   * Service Tagging by their type.
   * @type {String[]}
   **/
  serviceTypes: function() {
    var typeServiceMap = {
      GANGLIA: ['MONITORING'],
      HDFS: ['HA_MODE'],
      YARN: ['HA_MODE'],
      RANGER: ['HA_MODE'],
      HAWQ: ['HA_MODE']
    };
    return typeServiceMap[this.get('serviceName')] || [];
  }.property('serviceName'),

  /**
   * For each host-component, if the desired_configs dont match the
   * actual_configs, then a restart is required.
   */
  isRestartRequired: function () {
    var rhc = this.get('hostComponents').filterProperty('staleConfigs', true);
    var hc = {};

    rhc.forEach(function(_rhc) {
      var hostName = _rhc.get('hostName');
      if (!hc[hostName]) {
        hc[hostName] = [];
      }
      hc[hostName].push(_rhc.get('displayName'));
    });
    this.set('restartRequiredHostsAndComponents', hc);
    return (rhc.length > 0);
  }.property('serviceName'),
  
  /**
   * Contains a map of which hosts and host_components
   * need a restart. This is populated when calculating
   * #isRestartRequired()
   * Example:
   * {
   *  'publicHostName1': ['TaskTracker'],
   *  'publicHostName2': ['JobTracker', 'TaskTracker']
   * }
   */
  restartRequiredHostsAndComponents: {},
  
  /**
   * Based on the information in #restartRequiredHostsAndComponents
   */
  restartRequiredMessage: function () {
    var restartHC = this.get('restartRequiredHostsAndComponents');
    var hostCount = 0;
    var hcCount = 0;
    var hostsMsg = "<ul>";
    for(var host in restartHC){
      hostCount++;
      hostsMsg += "<li>"+host+"</li><ul>";
      restartHC[host].forEach(function(c){
        hcCount++;
        hostsMsg += "<li>"+c+"</li>";       
      });
      hostsMsg += "</ul>";
    }
    hostsMsg += "</ul>";
    return this.t('services.service.config.restartService.TooltipMessage').format(hcCount, hostCount, hostsMsg);
  }.property('restartRequiredHostsAndComponents'),

  /**
   * Does service have Critical Alerts
   * @type {boolean}
   */
  hasCriticalAlerts: false,

  /**
   * Number of the Critical and Warning alerts for current service
   * @type {number}
   */
  alertsCount: 0

});

App.Service.Health = {
  live: "LIVE",
  dead: "DEAD-RED",
  starting: "STARTING",
  stopping: "STOPPING",
  unknown: "DEAD-YELLOW",

  getKeyName: function (value) {
    switch (value) {
      case this.live:
        return 'live';
      case this.dead:
        return 'dead';
      case this.starting:
        return 'starting';
      case this.stopping:
        return 'stopping';
      case this.unknown:
        return 'unknown';
    }
    return 'none';
  }
};

/**
 * association between service and extended model name
 * @type {Object}
 */
  App.Service.extendedModel = {
  'HDFS': 'HDFSService',
  'HBASE': 'HBaseService',
  'YARN': 'YARNService',
  'MAPREDUCE2': 'MapReduce2Service',
  'STORM': 'StormService',
  'FLUME': 'FlumeService'
};

App.Service.FIXTURES = [];
