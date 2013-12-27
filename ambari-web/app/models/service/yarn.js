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
var objectUtils = require('utils/object_utils');

App.YARNService = App.Service.extend({
  version: DS.attr('string'),
  resourceManagerNode: DS.belongsTo('App.Host'),
  nodeManagerNodes: function(){
    return this.get('hostComponents').filterProperty('componentName', 'NODEMANAGER').mapProperty('host');
  }.property('hostComponents.length'),
  nodeManagerLiveNodes: DS.hasMany('App.Host'),
  nodeManagersCountActive: DS.attr('number'),
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
  yarnClientNodes: function(){
    return this.get('hostComponents').filterProperty('componentName', 'YARN_CLIENT').mapProperty('host');
  }.property('hostComponents.length'),
  resourceManagerStartTime: DS.attr('number'),
  jvmMemoryHeapUsed: DS.attr('number'),
  jvmMemoryHeapMax: DS.attr('number'),
  allocatedMemory: DS.attr('number'),
  reservedMemory: DS.attr('number'),
  availableMemory: DS.attr('number'),
  queue: DS.attr('string'),
  queueFormatted: function() {
    var queue = JSON.parse(this.get('queue'));
    return objectUtils.recursiveTree(queue);
  }.property('queue'),
  queuesCount: function() {
    var queue = JSON.parse(this.get('queue'));
    return objectUtils.recursiveKeysCount(queue);
  }.property('queue'),
  allQueueNames: [],
  childQueueNames: [],
  /** 
   * Provides a flat array of queue names.
   * Example: root, root/default
   */
  maxMemory: function() {
    return this.get('allocatedMemory') + this.get('availableMemory');
  }.property('allocatedMemory','availableMemory'),

  queueNames: function () {
    var queueString = this.get('queue');
    var allQueueNames = [];
    var childQueueNames = [];
    if (queueString != null) {
      var queues = JSON.parse(queueString);
      var addQueues = function (queuesObj, path){
        var names = [];
        for ( var subQueue in queuesObj) {
          if (queuesObj[subQueue] instanceof Object) {
            var qFN = path=='' ? subQueue : path+'/'+subQueue;
            names.push(qFN);
            var subNames = addQueues(queuesObj[subQueue], qFN);
            names = names.concat(subNames);
            if (!subNames || subNames.length < 1) {
              childQueueNames.push(qFN);
            }
          }
        }
        return names;
      }
      allQueueNames = addQueues(queues, '');
    }
    this.set('allQueueNames', allQueueNames);
    this.set('childQueueNames', childQueueNames);
  }.observes('queue'),
  /**
   * ResourceManager's lost count is not accurate once RM is rebooted. Since
   * Ambari knows the total number of nodes and the counts of nodes in other
   * states, we calculate the lost count.
   */
  nodeManagersCountLost: function () {
    var allNMs = this.get('nodeManagerNodes');
    var totalCount = allNMs != null ? allNMs.get('length') : 0;
    var activeCount = this.get('nodeManagersCountActive');
    var rebootedCount = this.get('nodeManagersCountRebooted');
    var unhealthyCount = this.get('nodeManagersCountUnhealthy');
    var decomCount = this.get('nodeManagersCountDecommissioned');
    var nonLostHostsCount = activeCount + rebootedCount + decomCount + unhealthyCount;
    return totalCount >= nonLostHostsCount ? totalCount - nonLostHostsCount : 0;
  }.property('nodeManagerNodes', 'nodeManagersCountActive', 'nodeManagersCountRebooted', 'nodeManagersCountUnhealthy', 'nodeManagersCountDecommissioned')
});

App.YARNService.FIXTURES = [];
