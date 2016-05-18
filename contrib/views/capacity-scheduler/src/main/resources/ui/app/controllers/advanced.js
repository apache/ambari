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

var cmp = Ember.computed;

App.CapschedAdvancedController = Ember.Controller.extend({
  needs: ['capsched'],

  actions: {
    rollbackQueueMappingProps: function() {
      var sched = this.get('scheduler'),
      attributes = sched.changedAttributes(),
      props = this.queueMappingProps;
      props.forEach(function(prop) {
        if (attributes.hasOwnProperty(prop)) {
          sched.set(prop, attributes[prop][0]);
        }
      });
    }
  },

  isOperator: cmp.alias('controllers.capsched.isOperator'),
  scheduler: cmp.alias('controllers.capsched.content'),
  queues: cmp.alias('controllers.capsched.queues'),
  isQueueMappingsDirty: false,
  queueMappingProps: ['queue_mappings', 'queue_mappings_override_enable'],

  queueMappingsDidChange: function() {
    var sched = this.get('scheduler'),
    attributes = sched.changedAttributes(),
    props = this.queueMappingProps;
    var isDirty = props.any(function(prop){
      return attributes.hasOwnProperty(prop);
    });
    this.set('isQueueMappingsDirty', isDirty);
  }.observes('scheduler.queue_mappings', 'scheduler.queue_mappings_override_enable')

});
