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

var App = require('app');

App.HDFSService = App.Service.extend({
  version: DS.attr('string'),
  nameNode: function () {
    return this.get('hostComponents').findProperty('componentName', 'NAMENODE');
  }.property('hostComponents'),
  snameNode: function () {
    return this.get('hostComponents').findProperty('componentName', 'SECONDARY_NAMENODE');
  }.property('hostComponents'),
  activeNameNode: DS.belongsTo('App.HostComponent'),
  standbyNameNode: DS.belongsTo('App.HostComponent'),
  standbyNameNode2: DS.belongsTo('App.HostComponent'),
  dataNodesStarted: DS.attr('number'),
  dataNodesInstalled: DS.attr('number'),
  dataNodesTotal: DS.attr('number'),
  journalNodes: function () {
    return this.get('hostComponents').filterProperty('componentName', 'JOURNALNODE');
  }.property('hostComponents.@each'),
  nameNodeStartTime: DS.attr('number'),
  jvmMemoryHeapUsed: DS.attr('number'),
  jvmMemoryHeapMax: DS.attr('number'),
  decommissionDataNodes: DS.hasMany('App.HostComponent'),
  capacityUsed: DS.attr('number'),
  capacityTotal: DS.attr('number'),
  capacityRemaining: DS.attr('number'),
  dfsTotalBlocks: DS.attr('number'),
  dfsCorruptBlocks: DS.attr('number'),
  dfsMissingBlocks: DS.attr('number'),
  dfsUnderReplicatedBlocks: DS.attr('number'),
  dfsTotalFiles: DS.attr('number'),
  upgradeStatus: DS.attr('boolean'),
  safeModeStatus: DS.attr('string'),
  nameNodeRpc: DS.attr('number')
});

App.HDFSService.FIXTURES = [];
