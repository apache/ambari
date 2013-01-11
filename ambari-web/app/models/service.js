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

App.Service = DS.Model.extend({

  serviceName: DS.attr('string'),
  serviceAudit: DS.hasMany('App.ServiceAudit'),

  workStatus: DS.attr('string'),
  alerts: DS.hasMany('App.Alert'),
  quickLinks: DS.hasMany('App.QuickLinks'),
  components: DS.hasMany('App.Component'),
  hostComponents: DS.hasMany('App.HostComponent'),
  isStartDisabled: function () {
    return !(this.get('healthStatus') == 'red');
  }.property('healthStatus'),

  isStopDisabled: function () {
    return !(this.get('healthStatus') == 'green');
  }.property('healthStatus'),

  healthStatus: function () {
    var components = this.get('components').filterProperty('isMaster', true);
    if (components.everyProperty('workStatus', App.Component.Status.started)) {
      return 'green';
    } else if (components.someProperty('workStatus', App.Component.Status.starting)) {
      return 'green-blinking';
    } else if (components.someProperty('workStatus', App.Component.Status.stopped)) {
      return 'red';
    } else {
      return 'red-blinking';
    }
  }.property('components.@each.workStatus'),

  isStopped: function () {
    var components = this.get('components');
    return components.everyProperty('workStatus', App.Component.Status.stopped);
  }.property('components.@each.workStatus'),

  isStarted: function () {
    var components = this.get('components').filterProperty('isMaster', true);
    return components.everyProperty('workStatus', App.Component.Status.started);
  }.property('components.@each.workStatus'),
  isMaintained: function(){
    var maintainedServices = [
      "HDFS",
      "MAPREDUCE",
      "HBASE",
      "OOZIE",
      "HIVE",
      "WEBHCAT",
      "ZOOKEEPER",
      "PIG",
      "SQOOP"
    ];
    for(var i in maintainedServices){
      if(this.get('serviceName') == maintainedServices[i]) return true;
    }
  }.property('serviceName'),
  isConfigurable:function(){
    var configurableServices = [
      "HDFS",
      "MAPREDUCE",
      "HBASE",
      "OOZIE",
      "HIVE",
      "WEBHCAT",
      "ZOOKEEPER",
      "PIG",
      "SQOOP",
      "NAGIOS"
    ];
    for(var i in configurableServices){
      if(this.get('serviceName') == configurableServices[i]) return true;
    }
  }.property('serviceName'),
  displayName: function () {
    switch (this.get('serviceName').toLowerCase()) {
      case 'hdfs':
        return 'HDFS';
      case 'mapreduce':
        return 'MapReduce';
      case 'hbase':
        return 'HBase';
      case 'oozie':
        return 'Oozie';
      case 'hive':
        return 'Hive/HCat';
      case 'hcatalog':
        return 'HCat';
      case 'zookeeper':
        return 'ZooKeeper';
      case 'pig':
        return 'Pig';
      case 'sqoop':
        return 'Sqoop';
      case 'webhcat':
        return 'WebHCat';
      case 'ganglia':
        return 'Ganglia';
      case 'nagios':
        return 'Nagios';
    }
    return this.get('serviceName');
  }.property('serviceName')
});

App.Service.Health = {
  live: "LIVE",
  dead: "DEAD",
  starting: "STARTING",
  stopping: "STOPPING",

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
    }
    return 'none';
  }
};

App.Service.FIXTURES = [];
