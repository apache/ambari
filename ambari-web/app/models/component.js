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

App.Component = DS.Model.extend({

  componentName: DS.attr('string'),

  displayName: function () {
    return App.format.role(this.get('componentName'));
  }.property('componentName'),

  service: DS.belongsTo('App.Service'),
  //service_id: DS.attr('string'),

  host: DS.belongsTo('App.Host'),
  //host_id: DS.attr('string'),

  workStatus: DS.attr('string'),

  isMaster: function () {
    switch (this.get('componentName')) {
      case 'NAMENODE':
      case 'SECONDARY_NAMENODE':
      case 'SNAMENODE':
      case 'JOBTRACKER':
      case 'ZOOKEEPER_SERVER':
      case 'HIVE_SERVER':
      case 'HIVE_METASTORE':
      case 'MYSQL_SERVER':
      case 'HBASE_MASTER':
      case 'NAGIOS_SERVER':
      case 'GANGLIA_SERVER':
      case 'OOZIE_SERVER':
      case 'WEBHCAT_SERVER':
        return true;
      default:
        return false;
    }
    return this.get('componentName');
  }.property('componentName'),
  isSlave: function(){
    switch (this.get('componentName')) {
      case 'DATANODE':
      case 'TASKTRACKER':
      case 'HBASE_REGIONSERVER':
      case 'GANGLIA_MONITOR':
        return true;
      default:
        return false;
    }
    return this.get('componentName');
  }.property('componentName')
});

App.Component.Status = {
  started: "STARTED",
  starting: "STARTING",
  stopped: "INSTALLED",
  stopping: "STOPPING",
  stop_failed: "STOP_FAILED",
  start_failed: "START_FAILED",

  getKeyName:function(value){
    switch(value){
      case this.started:
        return 'started';
      case this.starting:
        return 'starting';
      case this.stopped:
        return 'installed';
      case this.stopping:
        return 'stopping';
      case this.stop_failed:
        return 'stop_failed';
      case this.start_failed:
        return 'start_failed';
    }
    return 'none';
  }
}

App.Component.FIXTURES = [];

