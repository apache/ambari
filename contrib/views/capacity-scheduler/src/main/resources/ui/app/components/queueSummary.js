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

 var _runState = 'RUNNING';
 var _stopState = 'STOPPED';

 var _notStartedState = 'NOT STARTED';

 App.QueueSummaryComponent = Ember.Component.extend({
   layoutName: 'components/queueSummary',
   queue: null,
   allQueues: null,

   isRunningState: function() {
     return this.get('queue.state') === _runState || this.get('queue.state') === null;
   }.property('queue.state'),

   queueState: function() {
     if (this.get('queue.isNewQueue')) {
       return _notStartedState;
     } else if (this.get('isRunningState')) {
       return _runState;
     } else {
       return _stopState;
     }
   }.property('queue.state'),

   qStateColor: function() {
     if (this.get('queue.isNewQueue')) {
       return 'text-info';
     } else if (this.get('isRunningState')) {
       return 'text-success';
     } else {
       return 'text-danger';
     }
   }.property('queue.state'),

   isDirtyState: function() {
     return this.get('queue').changedAttributes().hasOwnProperty('state');
   }.property('queue.state'),

   effectiveCapacity: function() {
     var currentQ = this.get('queue'),
     allQueues = this.get('allQueues'),
     effectiveCapacityRatio = 1;
     while (currentQ !== null) {
       effectiveCapacityRatio *= (currentQ.get('capacity') / 100);
       currentQ = allQueues.findBy('id', currentQ.get('parentPath').toLowerCase()) || null;
     }
     var effectiveCapacityPercent = effectiveCapacityRatio * 100;
     this.get('queue').set('absolute_capacity', effectiveCapacityPercent || null);
     return effectiveCapacityPercent;
   }.property('queue.capacity', 'allQueues.@each.capacity', 'allQueues.length')
 });
