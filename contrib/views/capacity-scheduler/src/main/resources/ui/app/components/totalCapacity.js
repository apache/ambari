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

App.TotalCapacityComponent = Ember.Component.extend({
  layoutName:'components/totalCapacity',

  actions:{
    toggleEdit:function () {
      this.toggleProperty('isEdit');
    },
    addQueue:function (path) {
      this.sendAction('addQueue',path);
    },
    createQueue:function (queue) {
      this.sendAction('createQueue',queue);
    },
    deleteQueue:function (queue) {
      this.sendAction('deleteQueue',queue);
    },
  },

  /**
   * passed params
   */
  currentQueue:null,
  allQueues:[],
  allQueuesArranged:[],

  isEdit:false,

  disableEdit:function () {
    this.set('isEdit',false);
  }.observes('allQueues'),

  currentPrPath:Em.computed.alias('currentQueue.parentPath'),

  leafQueuesCapacity: Ember.computed.map('leafQueues.@each.capacity', function (queue) {
    return Number(queue.get('capacity')); 
  }),

  totalCapacity: Ember.computed.sum('leafQueuesCapacity'),

  leafQueues:function () {
    return this.allQueuesArranged.filterBy('parentPath',this.get('currentPrPath')).filterBy('isNew',false);
  }.property('allQueuesArranged.length','currentPrPath'),

  newLeafQueues:function () {
    return this.allQueues.filterBy('parentPath',this.get('currentPrPath')).filterBy('isNew',true);
  }.property('allQueues.length','currentPrPath'),
  
  parentQueue:function () {
    return this.allQueues.findBy('path',this.get('currentPrPath'));
  }.property('allQueues','currentPrPath'),

  currentInLeaf:function (argument) {
    var leaf = this.get('leafQueues');
    leaf.setEach('isCurrent',false);
    if(!this.get('currentQueue.currentState.stateName').match(/root.deleted/)) {
      this.get('currentQueue').set('isCurrent',true);
    }
  }.observes('leafQueues','currentQueue').on('init'),

  newQueueNameField: Em.TextField.extend({
    queue:null,
    classNames:['form-control'],
    classNameBindings:['isValid::input-error'],
    isValid:Em.computed.bool('queue.isValid')
  })
});

App.CapacityEditFormView = Em.View.extend({
  mark:function () {
    this.addObserver('controller.target.isEdit',this,'slide');
    if (!this.get('controller.target.isEdit')) {
      this.$('.capacity-edit-form').hide();
    };
  }.on('didInsertElement'),
  slide:function () {
    this.$('.capacity-edit-form').slideToggle(100);
  }
});
