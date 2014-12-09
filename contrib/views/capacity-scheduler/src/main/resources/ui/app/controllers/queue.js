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

App.QueueController = Ember.ObjectController.extend({
  needs:['queues'],
  isOperator:Em.computed.alias('controllers.queues.isOperator'),
  isNotOperator:Em.computed.alias('controllers.queues.isNotOperator'),
  actions:{
    setState:function (state) {
      this.content.set('state', (state === "running") ? _runState : _stopState );
    },
    createQ:function (record) {
      this.get('controllers.queues').send('createQ',this.get('content'));
    },
    addQ:function (path) {
      this.get('controllers.queues').send('addQ',path);
    },
    delQ:function (record) {
      this.get('controllers.queues').send('delQ',this.get('content'));
    },
    renameQ:function (opt) {
      if (opt == 'ask') {
        this.content.addObserver('name',this,this.setQueuePath);
        this.toggleProperty('isRenaming');
        return;
      }
      if (opt == 'cancel') {
        this.send('rollbackProp','name');
        this.send('rollbackProp','id');
        this.send('rollbackProp','path');
        this.content.removeObserver('name',this,this.setQueuePath);
        this.toggleProperty('isRenaming');
        return;
      }
      if (opt) {
        var self = this;
        this.store.filter('queue',function (q) {
          return q.id === self.content.id;
        }).then(function(queues){
          if (queues.get('length') > 1) {
            return self.content.get('errors').add('path', 'Queue already exists');
          }
          self.toggleProperty('isRenaming');
          self.content.removeObserver('name',self,self.setQueuePath);
          self.transitionToRoute('queue',self.content.id);
        })
      }

    },
    toggleEditRA:function () {
      this.toggleProperty('isEditRA');
    },
    toggleEditACL:function () {
      this.toggleProperty('isEditACL');
    },
    rollbackProp:function(prop){
      attributes = this.content.changedAttributes();
      if (attributes.hasOwnProperty(prop)) {
        this.content.set(prop,attributes[prop][0]);
      }
    }
  },
  setQueuePath:function (queue,o) {
    var name = queue.get(o);
    var parentPath = queue.get('parentPath');

    queue.setProperties({
      name:name.replace(/\s|\./g, ''),
      path:parentPath+'.'+name,
      id:(parentPath+'.'+name).dasherize()
    });

    if (name == '') {
      queue.get('errors').add('path', 'This field is required');
    }
  },

  isRenaming:false,

  unsetRenaming:function () {
    this.set('isRenaming',false);
  }.observes('content'),

  isRoot:Ember.computed.equal('content.id', 'root'),
  isRunning:Ember.computed.equal('content.state', _runState),
  allQueues:Em.computed.alias('controllers.queues.content'),
  allQueuesArranged:Em.computed.alias('controllers.queues.arrangedContent'),

  isEditRA:false,
  isEditACL:false,

  handleAcl:function (key,value) {
    if (value) {
      this.set(key,(value == '*')?'*':' ');
    }
    return (this.get(key) == '*')? '*':'custom';
  },

  acl_administer_queue: function (key, value, previousValue) {
    return this.handleAcl('content.acl_administer_queue',value);
  }.property('content.acl_administer_queue'),
  aaq_anyone:Ember.computed.equal('acl_administer_queue', '*'),
  aaq_dirty:function () {
    var attributes = this.content.changedAttributes();
    return attributes.hasOwnProperty('acl_administer_queue');
  }.property('content.acl_administer_queue'),

  acl_administer_jobs: function (key, value, previousValue) {
    return this.handleAcl('content.acl_administer_jobs',value);
  }.property('content.acl_administer_jobs'),
  aaj_anyone:Ember.computed.equal('acl_administer_jobs', '*'),
  aaj_dirty:function () {
    var attributes = this.content.changedAttributes();
    return attributes.hasOwnProperty('acl_administer_jobs');
  }.property('content.acl_administer_jobs'),

  acl_submit_applications: function (key, value, previousValue) {
    return this.handleAcl('content.acl_submit_applications',value);
  }.property('content.acl_submit_applications'),
  asa_anyone:Ember.computed.equal('acl_submit_applications', '*'),
  asa_dirty:function () {
    var attributes = this.content.changedAttributes();
    return attributes.hasOwnProperty('acl_submit_applications');
  }.property('content.acl_submit_applications'),

  capacityControl:function () {
    var leafQueues = this.get('leafQueues');
    var total = 0;

    leafQueues.forEach(function (queue) {
      total+=Number(queue.get('capacity'));
    });
    leafQueues.setEach('overCapacity',total>100);
  }.observes('content.capacity','leafQueues.@each.capacity'),
  leafQueues:function () {
    return this.get('allQueues').filterBy('parentPath',this.get('content.parentPath'));
  }.property('allQueues.length','content.parentPath'),

  queueNamesControl:function (c,o) {
    var leaf = c.get('leafQueues');
    var parent = c.get('allQueues').filterBy('path',c.get('content.parentPath')).get('firstObject');
    if (parent) parent.set('queueNames',leaf.mapBy('name').join() || null);
  }.observes('allQueues.length','allQueues.@each.name','content'),

  pathErrors:Ember.computed.mapBy('content.errors.path','message')
  
});

App.ErrorController = Ember.ObjectController.extend();
