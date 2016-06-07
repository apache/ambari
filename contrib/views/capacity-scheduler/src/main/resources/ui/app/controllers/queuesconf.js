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
      leafQueueNames = store.getById('queue', parentPath.toLowerCase()).get('queuesArray'),
      newInLeaf = Ember.isEmpty(leafQueueNames),
      totalLeafCapacity,
      freeLeafCapacity,
      newQueue;

      this.send('clearCreateQueue');
      if (!newInLeaf) {
        totalLeafCapacity = leafQueueNames.reduce(function (capacity, qName) {
          return store.getById('queue', [parentPath, qName].join('.').toLowerCase()).get('capacity') + capacity;
        }, 0);
        freeLeafCapacity = (totalLeafCapacity < 100) ? 100 - totalLeafCapacity : 0;
      }
      var qCapacity = (newInLeaf) ? 100 : freeLeafCapacity;
      newQueue = store.createRecord('queue', {
        id: queuePath.toLowerCase(),
        name: queueName,
        path: queuePath,
        parentPath: parentPath,
        depth: depth,
        isNewQueue: true,
        capacity: qCapacity,
        maximum_capacity: qCapacity
      });
      this.set('newQueue', newQueue);
      store.saveAndUpdateQueue(newQueue)
      .then(Em.run.bind(this, 'saveAndUpdateQueueSuccess', newQueue))
      .catch(Em.run.bind(this, 'saveQueuesConfigError', 'createQueue'));
    },
    saveQueuesConfig: function(saveMode) {
      var that = this;
      var collectedLabels = this.get('queues').reduce(function (prev, q) {
        return prev.pushObjects(q.get('labels.content'));
      },[]);

      var store = this.get('store'),
      optn = '',
      saveQs = this.get('queues').save(),
      labels = DS.ManyArray.create({content:collectedLabels}).save();

      if (saveMode === 'refresh') {
        optn = 'saveAndRefresh';
      } else if (saveMode === 'restart') {
        optn = 'saveAndRestart';
      }

      Ember.RSVP.Promise.all([labels, saveQs])
      .then(Em.run.bind(that, 'saveQueuesConfigSuccess'))
      .then(function() {
        if (optn) {
          return store.relaunchCapSched(optn);
        }
      })
      .then(Em.run.bind(that, 'relaunchCapSchedSuccess', optn))
      .catch(Em.run.bind(that, 'saveQueuesConfigError', optn));
    },
    clearCreateQueue: function() {
      this.set('newQueueName', '');
      this.set('showQueueNameInput', false);
      this.set('isInvalidQueueName', false);
      this.set('invalidQueueNameMessage', '');
    },
    clearAlert: function () {
      this.set('alertMessage', null);
    },
    cancelQueuesChanges: function() {

    },
    stopQueue: function() {
      this.set('selectedQueue.state', _stopState);
    },
    startQueue: function() {
      this.set('selectedQueue.state', _runState);
    },
    deleteQueue: function() {
      var that = this;
      var delQ = this.get('selectedQueue');
      if (delQ.get('isNew')) {
        this.set('newQueue', null);
      }
      this.transitionToRoute('capsched.queuesconf.editqueue', delQ.get('parentPath').toLowerCase())
      .then(Em.run.schedule('afterRender', function () {
        that.get('store').recurceRemoveQueue(delQ);
      }));
    }
  },

  isAnyQueueDirty: function() {
    return this.get('queues').isAny('isAnyDirty');
  }.property('queues.@each.isAnyDirty'),

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
   * Marks each queue in leaf with 'overCapacity' if sum if their capacity values is greater than 100.
   * @method capacityControl
   */
  capacityControl: function() {
    var paths = this.get('queues').getEach('parentPath').uniq();
    paths.forEach(function (path) {
      var leaf = this.get('queues').filterBy('parentPath', path),
      total = leaf.reduce(function (prev, queue) {
          return +queue.get('capacity') + prev;
        }, 0);
      leaf.setEach('overCapacity', total != 100);
    }.bind(this));
  }.observes('queues.length','queues.@each.capacity'),

  /**
   * True if newQueue is not empty.
   * @type {Boolean}
   */
  hasNewQueue: Ember.computed.bool('newQueue'),

  /**
   * True if some queue of desired configs was removed.
   * @type {Boolean}
   */
  hasDeletedQueues: Ember.computed.alias('store.hasDeletedQueues'),

  /**
   * Check properties for refresh requirement
   * @type {Boolean}
   */
  needRefreshProps: Ember.computed.any('hasChanges', 'hasNewQueues'),

  /**
   * List of modified queues.
   * @type {Array}
   */
  dirtyQueues:function () {
    return this.get('queues').filter(function (q) {
      return q.get('isAnyDirty');
    });
  }.property('queues.@each.isAnyDirty'),

  /**
   * True if dirtyQueues is not empty.
   * @type {Boolean}
   */
  hasChanges: Ember.computed.notEmpty('dirtyQueues.[]'),

  /**
   * List of new queues.
   * @type {Array}
   */
  newQueues: Ember.computed.filterBy('queues', 'isNewQueue', true),

  /**
   * True if newQueues is not empty.
   * @type {Boolean}
   */
  hasNewQueues: Ember.computed.notEmpty('newQueues.[]'),

  /**
   * check if RM needs restart
   * @type {bool}
   */
  needRestart: Em.computed.and('hasDeletedQueues', 'isOperator'),

  /**
   * check there is some changes for save
   * @type {bool}
   */
  needSave: Em.computed.any('needRestart', 'needRefresh'),

  /**
   * check if RM needs refresh
   * @type {bool}
   */
  needRefresh: Em.computed.and('needRefreshProps', 'noNeedRestart', 'isOperator'),

  /**
   * Inverted needRestart value.
   * @type {Boolean}
   */
  noNeedRestart: Em.computed.not('needRestart'),

  /**
   * check if can save configs
   * @type {bool}
   */
  canNotSave: Em.computed.any('hasOverCapacity', 'hasInvalidMaxCapacity', 'hasUncompetedAddings', 'hasNotValid', 'hasNotValidLabels'),

  /**
   * List of not valid queues.
   * @type {Array}
   */
  notValid: Em.computed.filterBy('queues', 'isValid', false),

  /**
   * True if notValid is not empty.
   * @type {Boolean}
   */
  hasNotValid: Em.computed.notEmpty('notValid.[]'),

  /**
   * True if queues have not valid labels.
   * @type {Boolean}
   */
  hasNotValidLabels: function() {
    return this.get('queues').anyBy('hasNotValidLabels',true);
  }.property('queues.@each.hasNotValidLabels'),

  /**
   * List of queues with excess of capacity
   * @type {Array}
   */
  overCapacityQ: function () {
    return this.get('queues').filter(function (q) {
      return q.get('overCapacity');
    });
  }.property('queues.@each.overCapacity'),

  /**
   * True if overCapacityQ is not empty.
   * @type {Boolean}
   */
  hasOverCapacity: Em.computed.notEmpty('overCapacityQ.[]'),

  /**
   * True if any queue has invalid max capacity (if max_cappacity < capacity).
   * @type {Boolean}
   */
  hasInvalidMaxCapacity: Em.computed.filterBy('queues', 'isInvalidMaxCapacity', true),

  /**
   * List of queues with incomplete adding process
   * @type {[type]}
   */
  uncompetedAddings: Em.computed.filterBy('queues', 'isNew', true),

  /**
   * True if uncompetedAddings is not empty.
   * @type {Boolean}
   */
  hasUncompetedAddings: Em.computed.notEmpty('uncompetedAddings.[]'),

  /**
   * Represents queue run state. Returns true if state is null.
   * @return {Boolean}
   */
  isSelectedQRunning: function() {
    if (!this.get('selectedQueue.isNewQueue')) {
      return this.get('selectedQueue.state') === _runState || this.get('selectedQueue.state') === null;
    } else {
      return false;
    }
  }.property('selectedQueue.state', 'selectedQueue.isNewQueue'),

  isSelectedQStopped: function() {
    return this.get('selectedQueue.state') === _stopState;
  }.property('selectedQueue.state'),


  isSelectedQLeaf: function() {
    return this.get('selectedQueue.queues') === null;
  }.property('selectedQueue.queues'),

  isSelectedQLeafAndRunning: function() {
    return this.get('isSelectedQLeaf') && this.get('isSelectedQRunning');
  }.property('isSelectedQRunning', 'isSelectedQLeaf'),

  /**
   * Returns true if queue is root.
   * @type {Boolean}
   */
   isRootQSelected: Ember.computed.match('selectedQueue.id', /^(root)$/),

   isSelectedQDeletable: function() {
     return !this.get('isRootQSelected') && !this.get('isSelectedQRunning');
   }.property('isRootQSelected', 'isSelectedQRunning'),

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
   },
   relaunchCapSchedSuccess: function(opt) {

   }
});
