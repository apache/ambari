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

App.Scheduler = DS.Model.extend({
  maximum_am_resource_percent: DS.attr('number', { defaultValue: 0 }),
  maximum_applications: DS.attr('number', { defaultValue: 0 }),
  node_locality_delay: DS.attr('number', { defaultValue: 0 })
});

/**
 * Represents the queue.
 *
 */
App.Queue = DS.Model.extend({
  name: DS.attr('string'),
  parentPath: DS.attr('string'),
  depth: DS.attr('number'),
  path: DS.attr('string'),

  // cs props
  
  // queue props
  state: DS.attr('string', { defaultValue: 'RUNNING' }),

  capacity: DS.attr('number', { defaultValue: 0 }),
  maximum_capacity: DS.attr('number', { defaultValue: 0 }),
  unfunded_capacity: DS.attr('number', { defaultValue: 0 }),
  
  acl_administer_queue: DS.attr('string', { defaultValue: '*' }),
  acl_administer_jobs: DS.attr('string', { defaultValue: '*' }),
  acl_submit_applications: DS.attr('string', { defaultValue: '*' }),
  
  minimum_user_limit_percent: DS.attr('number', { defaultValue: 0 }),
  user_limit_factor: DS.attr('number', { defaultValue: 0 }),
  
  queueNames: DS.attr('string'),
  queueNamesArray:function () {
    return (this.get('queueNames.length')>0)?this.get('queueNames').split(','):[];
  }.property('queueNames'),

  

  overCapacity:false,

  //new queue flag
  isNewQueue:DS.attr('boolean', {defaultValue: false})
});
