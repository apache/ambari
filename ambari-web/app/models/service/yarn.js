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

App.YARNService = App.Service.extend({
  version: DS.attr('string'),
  resourceManagerNode: DS.belongsTo('App.Host'),
  nodeManagerNodes: DS.hasMany('App.Host'),
  nodeManagerLiveNodes: DS.hasMany('App.Host'),
  nodeManagersCountActive: DS.attr('number'),
  nodeManagersCountLost: DS.attr('number'),
  nodeManagersCountUnhealthy: DS.attr('number'),
  nodeManagersCountRebooted: DS.attr('number'),
  nodeManagersCountDecommissioned: DS.attr('number'),
  containersAllocated: DS.attr('number'),
  containersPending: DS.attr('number'),
  containersReserved: DS.attr('number'),
  appsSubmitted: DS.attr('number'),
  appsRunning: DS.attr('number'),
  appsPending: DS.attr('number'),
  appsCompleted: DS.attr('number'),
  appsKilled: DS.attr('number'),
  appsFailed: DS.attr('number'),
  yarnClientNodes: DS.hasMany('App.Host'),
  resourceManagerStartTime: DS.attr('number'),
  jvmMemoryHeapUsed: DS.attr('number'),
  jvmMemoryHeapCommitted: DS.attr('number')
});

App.YARNService.FIXTURES = [];
