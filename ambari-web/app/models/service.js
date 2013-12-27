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

  workStatus: DS.attr('string'),
  rand: DS.attr('string'),
  toolTipContent: DS.attr('string'),
  alerts: DS.hasMany('App.Alert'),
  quickLinks: DS.hasMany('App.QuickLinks'),
  hostComponents: DS.hasMany('App.HostComponent'),
  serviceConfigsTemplate: App.config.get('preDefinedServiceConfigs'),

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

  isConfigurable: function () {
    var configurableServices = [
      "HDFS",
      "YARN",
      "MAPREDUCE",
      "MAPREDUCE2",
      "HBASE",
      "OOZIE",
      "HIVE",
      "WEBHCAT",
      "ZOOKEEPER",
      "PIG",
      "SQOOP",
      "NAGIOS",
      "GANGLIA",
      "HUE"
    ];
    return configurableServices.contains(this.get('serviceName'));
  }.property('serviceName'),

  displayName: function () {
    return App.Service.DisplayNames[this.get('serviceName')];
  }.property('serviceName'),

  /**
   * For each host-component, if the desired_configs dont match the
   * actual_configs, then a restart is required. Except for Global site
   * properties, which need to be checked with map.
   */
  isRestartRequired: function () {
    var rhc = this.get('hostComponents').filterProperty('staleConfigs', true);
    var hc = {};
    rhc.forEach(function(_rhc) {
      var hostName = _rhc.get('host.publicHostName');
      if (!hc[hostName]) {
        hc[hostName] = [];
      }
      hc[hostName].push(_rhc.get('displayName'));
    });
    this.set('restartRequiredHostsAndComponents', hc);
    return (rhc.length>0);

  }.property('serviceName', 'hostComponents.@each.staleConfigs'),
  
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
      })
      hostsMsg += "</ul>";
    }
    hostsMsg += "</ul>"
    return this.t('services.service.config.restartService.TooltipMessage').format(hcCount, hostCount, hostsMsg);
  }.property('restartRequiredHostsAndComponents')
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

App.Service.DisplayNames = {
  'HDFS': 'HDFS',
  'YARN': 'YARN',
  'MAPREDUCE': 'MapReduce',
  'MAPREDUCE2': 'MapReduce2',
  'TEZ': 'Tez',
  'HBASE': 'HBase',
  'OOZIE': 'Oozie',
  'HIVE': 'Hive',
  'HCATALOG': 'HCat',
  'ZOOKEEPER': 'ZooKeeper',
  'PIG': 'Pig',
  'SQOOP': 'Sqoop',
  'WEBHCAT': 'WebHCat',
  'GANGLIA': 'Ganglia',
  'NAGIOS': 'Nagios',
  'HUE': 'Hue',
  'FLUME': 'Flume'
};

App.Service.servicesSortOrder = [
  'HDFS',
  'YARN',
  'MAPREDUCE',
  'MAPREDUCE2',
  'TEZ',
  'HBASE',
  'HIVE',
  'HCATALOG',
  'WEBHCAT',
  'FLUME',
  'OOZIE',
  'GANGLIA',
  'NAGIOS',
  'ZOOKEEPER',
  'PIG',
  'SQOOP',
  'HUE'
];

App.Service.FIXTURES = [];
