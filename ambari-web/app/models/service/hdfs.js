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
  nameNode: DS.belongsTo('App.HostComponent'),
  snameNode: DS.belongsTo('App.HostComponent'),
  activeNameNode: DS.belongsTo('App.HostComponent'),
  standbyNameNode: DS.belongsTo('App.HostComponent'),
  standbyNameNode2: DS.belongsTo('App.HostComponent'),
  isNnHaEnabled: function() {
    return !this.get('snameNode') && this.get('hostComponents').filterProperty('componentName', 'NAMENODE').length > 1;
  }.property('snameNode','hostComponents'),
  dataNodesStarted: DS.attr('number'),
  dataNodesInstalled: DS.attr('number'),
  dataNodesTotal: DS.attr('number'),
  nfsGatewaysStarted: DS.attr('number', {defaultValue: 0}),
  nfsGatewaysInstalled: DS.attr('number', {defaultValue: 0}),
  nfsGatewaysTotal: DS.attr('number', {defaultValue: 0}),
  journalNodes: DS.hasMany('App.HostComponent'),
  nameNodeStartTime: DS.attr('number'),
  jvmMemoryHeapUsed: DS.attr('number'),
  jvmMemoryHeapMax: DS.attr('number'),
  decommissionDataNodes: DS.hasMany('App.HostComponent'),
  liveDataNodes: DS.hasMany('App.HostComponent'),
  deadDataNodes: DS.hasMany('App.HostComponent'),
  capacityUsed: DS.attr('number'),
  capacityTotal: DS.attr('number'),
  capacityRemaining: DS.attr('number'),
  capacityNonDfsUsed: DS.attr('number'),
  dfsTotalBlocks: DS.attr('number'),
  dfsCorruptBlocks: DS.attr('number'),
  dfsMissingBlocks: DS.attr('number'),
  dfsUnderReplicatedBlocks: DS.attr('number'),
  dfsTotalFiles: DS.attr('number'),
  upgradeStatus: DS.attr('string'),
  safeModeStatus: DS.attr('string'),
  nameNodeRpc: DS.attr('number'),
  metricsNotAvailable: DS.attr('boolean')
});

App.HDFSService.FIXTURES = [];
