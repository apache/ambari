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

  workStatus: DS.attr('string'),
  alerts: DS.hasMany('App.Alert'),
  quickLinks: DS.hasMany('App.QuickLinks'),
  hostComponents: DS.hasMany('App.HostComponent'),
  isStartDisabled: function () {
    return !(this.get('healthStatus') == 'red');
  }.property('healthStatus'),

  isStopDisabled: function () {
    return !(this.get('healthStatus') == 'green');
  }.property('healthStatus'),

  // Instead of making healthStatus a computed property that listens on hostComponents.@each.workStatus,
  // we are creating a separate observer _updateHealthStatus.  This is so that healthStatus is updated
  // only once after the run loop.  This is because Ember invokes the computed property every time
  // a property that it depends on changes.  For example, App.statusMapper's map function would invoke
  // the computed property too many times and freezes the UI without this hack.
  // See http://stackoverflow.com/questions/12467345/ember-js-collapsing-deferring-expensive-observers-or-computed-properties
  healthStatus: '',

  updateHealthStatus: function () {
    // console.log('model:service.healthStatus ' + this.get('serviceName'));
    var components = this.get('hostComponents').filterProperty('isMaster', true);
    var isGreen = (this.get('serviceName') === 'HBASE' ?
      components.someProperty('workStatus', App.HostComponentStatus.started) :
      components.everyProperty('workStatus', App.HostComponentStatus.started)) ;

    if (isGreen) {
      this.set('healthStatus', 'green');
    } else if (components.someProperty('workStatus', App.HostComponentStatus.starting)) {
      this.set('healthStatus', 'green-blinking');
    } else if (components.someProperty('workStatus', App.HostComponentStatus.stopped)) {
      this.set('healthStatus', 'red');
    } else {
      this.set('healthStatus', 'red-blinking');
    }
  },

  /**
   * Every time when changes workStatus of any component we schedule recalculating values related from them
   */
  _updateHealthStatus: (function() {
    Ember.run.once(this, 'updateHealthStatus');
    Ember.run.once(this, 'updateIsStopped');
    Ember.run.once(this, 'updateIsStarted');
  }).observes('hostComponents.@each.workStatus'),

  isStopped: false,
  isStarted: false,

  updateIsStopped: function () {
    var components = this.get('hostComponents');
    var flag = true;
    components.forEach(function (_component) {
      if (_component.get('workStatus') !== App.HostComponentStatus.stopped && _component.get('workStatus') !== App.HostComponentStatus.install_failed) {
        flag = false;
      }
    }, this);
    this.set('isStopped', flag);
  },

  updateIsStarted: function () {
    var components = this.get('hostComponents').filterProperty('isMaster', true);
    this.set('isStarted',
      components.everyProperty('workStatus', App.HostComponentStatus.started)
    );
  },

  isMaintained: function () {
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
    return maintainedServices.contains(this.get('serviceName'));
  }.property('serviceName'),

  isConfigurable: function () {
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
    return configurableServices.contains(this.get('serviceName'));
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
  dead: "DEAD-RED",
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
