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
/**
 * This model loads all serviceComponents supported by the stack
 * @type {*}
 */
App.StackServiceComponent = DS.Model.extend({
  componentName: DS.attr('string'),
  serviceName: DS.attr('string'),
  componentCategory: DS.attr('string'),
  isMaster: DS.attr('boolean'),
  isClient: DS.attr('boolean'),
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),

  displayName: function() {
    return App.format.components[this.get('componentName')];
  }.property('componentName'),

  isSlave: function() {
   return this.get('componentCategory') === 'SLAVE';
  }.property('componentCategory'),

  isRestartable: function() {
    return !this.get('isClient');
  }.property('isClient'),

  isReassignable: function() {
    return ['NAMENODE', 'SECONDARY_NAMENODE', 'JOBTRACKER', 'RESOURCEMANAGER'].contains(this.get('componentName'));
  }.property('componentName'),

  isDeletable: function() {
    return ['SUPERVISOR', 'HBASE_MASTER', 'DATANODE', 'TASKTRACKER', 'NODEMANAGER', 'HBASE_REGIONSERVER', 'GANGLIA_MONITOR'].contains(this.get('componentName'));
  }.property('componentName'),

  isRollinRestartAllowed: function() {
    return ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER", "SUPERVISOR"].contains(this.get('componentName'));
  }.property('componentName'),

  isDecommissionAllowed: function() {
    return ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER"].contains(this.get('componentName'));
  }.property('componentName'),

  isRefreshConfigsAllowed: function() {
    return ["FLUME_HANDLER"].contains(this.get('componentName'));
  }.property('componentName'),

  isAddableToHost: function() {
    return ["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER", "HBASE_MASTER", "ZOOKEEPER_SERVER", "SUPERVISOR", "GANGLIA_MONITOR"].contains(this.get('componentName'));
  }.property('componentName'),

  isShownOnInstallerAssignMasterPage: function() {
    var component = this.get('componentName');
    var mastersNotShown = ['MYSQL_SERVER','JOURNALNODE'];
    return ((this.get('isMaster') && !mastersNotShown.contains(component)) || component === 'APP_TIMELINE_SERVER');
  }.property('isMaster','componentName')
});

App.StackServiceComponent.FIXTURES = [];