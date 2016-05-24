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

App.CapschedQueuesconfController = Ember.Controller.extend({
  needs: ['capsched', 'loading'],
  queues: Ember.computed.alias('controllers.capsched.queues'),
  isOperator: Ember.computed.alias('controllers.capsched.isOperator'),

  actions: {
    addNewQueue: function() {
      this.set('newQueueName', '');
      this.set('showQueueNameInput', true);
    },
    createNewQueue: function() {
      if (this.validateQueueName()) {
        return;
      }
      var store = this.get('store'),
      queueName = this.get('newQueueName'),
      parentPath = this.get('selectedQueue.path'),
      queuePath = [parentPath, queueName].join('.'),
      depth = parentPath.split('.').length,
      queueAlreadyExists = store.hasRecordForId('queue', queuePath.toLowerCase()),
      leafQueueNames = store.getById('queue', parentPath.toLowerCase()).get('queuesArray'),
      newInLeaf = Ember.isEmpty(leafQueueNames),
      existed = store.get('deletedQueues').findBy('path', queuePath),
      totalLeafCapacity,
      freeLeafCapacity,
      newQueue;

      this.send('clearCreateQueue');

      if (existed) {
        newQueue = store.createFromDeleted(existed);
      } else {
        if (!newInLeaf) {
          totalLeafCapacity = leafQueueNames.reduce(function (capacity, qName) {
            return store.getById('queue', [parentPath, qName].join('.').toLowerCase()).get('capacity') + capacity;
          }, 0);
          freeLeafCapacity = (totalLeafCapacity < 100) ? 100 - totalLeafCapacity : 0;
        }

        var qCapacity = (newInLeaf) ? 100 : freeLeafCapacity;

        newQueue = store.createRecord('queue', {
          id: queuePath,
          name: queueName,
          path: queuePath,
          parentPath: parentPath,
          depth: depth,
          isNewQueue: true,
          capacity: qCapacity,
          maximum_capacity: qCapacity
        });

        this.set('newQueue', newQueue);
      }

      store.saveAndUpdateQueue(newQueue)
      .then(Em.run.bind(this, 'saveAndUpdateQueueSuccess', newQueue))
      .catch(Em.run.bind(this, 'saveQueuesConfigError', 'createQueue'));
    },
    saveQueuesConfig: function() {
      var store = this.get('store'),
      opt = 'saveAndRefresh',
      saveQs = this.get('queues').save();

      Ember.RSVP.Promise.all([saveQs]).then(
        Em.run.bind(this, 'saveQueuesConfigSuccess'),
        Em.run.bind(this, 'saveQueuesConfigError', opt)
      ).then(function() {
        return store.relaunchCapSched(opt);
      }).catch(Em.run.bind(this, 'saveQueuesConfigError', opt));

    },
    clearCreateQueue: function() {
      this.set('newQueueName', '');
      this.set('showQueueNameInput', false);
      this.set('isInvalidQueueName', false);
      this.set('invalidQueueNameMessage', '');
    },
    clearAlert: function () {
      this.set('alertMessage', null);
    }
  },

  selectedQueue: null,
  newQueue: null,
  newQueueName: '',
  showQueueNameInput: false,
  isInvalidQueueName: false,
  invalidQueueNameMessage: '',

  validateQueueName: function() {
    var parentPath = this.get('selectedQueue.path'),
    queueName = this.get('newQueueName'),
    queuePath = [parentPath, queueName].join('.'),
    qAlreadyExists = this.store.hasRecordForId('queue', queuePath.toLowerCase());
    if (Ember.isBlank(queueName)) {
      this.set('isInvalidQueueName', true);
      this.set('invalidQueueNameMessage', 'Enter queue name');
    } else if (qAlreadyExists) {
      this.set('isInvalidQueueName', true);
      this.set('invalidQueueNameMessage', 'Queue already exists');
    } else {
      this.set('isInvalidQueueName', false);
      this.set('invalidQueueNameMessage', '');
    }
    return this.get('isInvalidQueueName');
  },

  queueNameDidChange: function() {
    this.validateQueueName();
  }.observes('newQueueName', 'newQueueName.length'),

  /**
   * True if newQueue is not empty.
   * @type {Boolean}
   */
  hasNewQueue: Ember.computed.bool('newQueue'),

  /**
   * Represents queue run state. Returns true if state is null.
   * @return {Boolean}
   */
  isSelectedQRunning: function() {
    return this.get('selectedQueue.state') == _runState || this.get('selectedQueue.state') == null;
  }.property('selectedQueue.state'),

  /**
   * Returns true if queue is root.
   * @type {Boolean}
   */
   isRootQSelected: Ember.computed.match('selectedQueue.id', /^(root)$/),

   /**
    * Property for error message which may appear when saving queue.
    * @type {Object}
    */
   alertMessage: null,

   configNote: Ember.computed.alias('store.configNote'),

   saveQueuesConfigSuccess: function() {
     this.set('store.deletedQueues', []);
   },
   saveQueuesConfigError: function(operation, error) {
     var response = (error && error.responseJSON)? error.responseJSON : {};
     response.simpleMessage = operation.capitalize() + ' failed!';
     this.set('alertMessage', response);
   },
   saveAndUpdateQueueSuccess: function(newQ) {
     var parentPath = newQ.get('parentPath'),
     parentQ = this.store.getById('queue', parentPath.toLowerCase()),
     pQueues = parentQ.get('queues') ? parentQ.get('queues').split(",") : [];
     pQueues.addObject(newQ.get('name'));
     pQueues.sort();
     parentQ.set('queues', pQueues.join(","));
     this.set('newQueue', null);
   }
});
