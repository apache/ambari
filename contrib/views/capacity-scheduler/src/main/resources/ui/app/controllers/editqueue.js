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

App.CapschedQueuesconfEditqueueController = Ember.Controller.extend({
  needs: ['capsched'],
  isOperator: Ember.computed.alias('controllers.capsched.isOperator'),
  isNotOperator: Ember.computed.not('isOperator'),
  scheduler: Ember.computed.alias('controllers.capsched.content'),
  allQueues: Ember.computed.alias('controllers.capsched.queues'),

  isRangerEnabledForYarn: function() {
    var isRanger = this.get('controllers.capsched.isRangerEnabledForYarn');
    if (isRanger == null || typeof isRanger == 'undefined') {
      return false;
    }
    isRanger = isRanger.toLowerCase();
    if (isRanger == 'yes' || isRanger == 'true') {
      return true;
    }
    return false;
  }.property('controllers.capsched.isRangerEnabledForYarn'),

  actions: {
    toggleProperty: function (property, target) {
      target = target || this;
      target.toggleProperty(property);
    },
    mouseUp: function(){
      return false;
    },
    editQueueName: function() {
      this.set('enableEditQName', true);
      this.set('updatedQName', this.get('content.name'));
    },
    cancelQNameEdit: function() {
      this.set('enableEditQName', false);
      this.set('isInvalidQName', false);
      this.set('invalidQNameMessage', '');
    },
    renameQueue: function() {
      if (this.validateQName()) {
        return;
      }
      this.set('content.name', this.get('updatedQName'));
      this.set('enableEditQName', false);
    }
  },

  updatedQName: '',
  enableEditQName: false,
  isInvalidQName: false,
  invalidQNameMessage: '',

  validateQName: function() {
    var qName = this.get('updatedQName'),
    originalQName = this.get('content.name'),
    qParentPath = this.get('content.parentPath'),
    qPath = [qParentPath, qName].join('.'),
    qAlreadyExists = this.store.hasRecordForId('queue', qPath.toLowerCase());
    if (Ember.isBlank(qName)) {
      this.set('isInvalidQName', true);
      this.set('invalidQNameMessage', 'Enter queue name');
    } else if (qAlreadyExists && qName !== originalQName) {
      this.set('isInvalidQName', true);
      this.set('invalidQNameMessage', 'Queue already exists');
    } else {
      this.set('isInvalidQName', false);
      this.set('invalidQNameMessage', '');
    }
    return this.get('isInvalidQName');
  },

  qNameDidChage: function() {
    this.validateQName();
  }.observes('updatedQName', 'updatedQName.length'),

  /**
   * Collection of modified fields in queue.
   * @type {Object} - { [fileldName] : {Boolean} }
   */
  queueDirtyFields: {},

  isQueueDirty: Ember.computed.bool('content.isDirty'),

  /**
   * Possible values for ordering policy
   * @type {Array}
   */
  orderingPolicyValues: [
    {label: 'FIFO', value: 'fifo'},
    {label: 'Fair', value: 'fair'}
  ],

  /**
   * Returns true if queue is root.
   * @type {Boolean}
   */
   isRoot: Ember.computed.match('content.id', /^(root)$/),

   /**
    * Returns true if queue is default.
    * @type {Boolean}
    */
   isDefaultQ: Ember.computed.match('content.id', /^(root.default)$/),

   /**
    * Represents queue run state. Returns true if state is null.
    * @return {Boolean}
    */
   isRunning: function() {
     return this.get('content.state') == _runState || this.get('content.state') == null;
   }.property('content.state'),

   /**
    * Current ordering policy value of queue.
    * @param  {String} key
    * @param  {String} value
    * @return {String}
    */
   currentOP: function (key, val) {
     if (arguments.length > 1) {
       if (!this.get('isFairOP')) {
         this.send('rollbackProp', 'enable_size_based_weight', this.get('content'));
       }
       this.set('content.ordering_policy', val || null);
     }
     return this.get('content.ordering_policy') || 'fifo';
   }.property('content.ordering_policy'),

   /**
    * Does ordering policy is equal to 'fair'
    * @type {Boolean}
    */
   isFairOP: Ember.computed.equal('content.ordering_policy', 'fair'),

   /**
    * Returns maximum applications for a queue if defined,
    * else the inherited value (for all queues)
    */
   maximumApplications: function(key, val) {
     if (arguments.length > 1) {
       if (val !== this.get('scheduler.maximum_applications')) {
         this.set('content.maximum_applications', val);
       } else {
         this.set('content.maximum_applications', null);
       }
     }
     var schedulerMaxApps = this.get('scheduler.maximum_applications'),
     absoluteCapacity = this.get('content.absolute_capacity');
     if (this.get('content.maximum_applications')) {
       return this.get('content.maximum_applications');
     } else {
       return Math.round(schedulerMaxApps * (absoluteCapacity / 100));
     }
   }.property('content.maximum_applications', 'content.absolute_capacity', 'scheduler.maximum_applications'),

   /**
    * Returns maximum AM resource percent for a queue if defined,
    * else the inherited value (for all queues)
    */
   maximumAMResourcePercent: function(key, val) {
     if (arguments.length > 1) {
       if (val !== this.get('scheduler.maximum_am_resource_percent')) {
         this.set('content.maximum_am_resource_percent', val);
       } else {
         this.set('content.maximum_am_resource_percent', null);
       }
     }
     var schedulerResoucePercent = this.get('scheduler.maximum_am_resource_percent'),
     absoluteCapacity = this.get('content.absolute_capacity');
     if (this.get('content.maximum_am_resource_percent')) {
        return this.get('content.maximum_am_resource_percent')
     } else {
       return (schedulerResoucePercent * (absoluteCapacity / 100));
     }
   }.property('content.maximum_am_resource_percent', 'content.absolute_capacity', 'scheduler.maximum_am_resource_percent'),

   /**
    * Sets ACL value to '*' or ' ' and returns '*' and 'custom' respectively.
    * @param  {String} key   - ACL attribute
    * @param  {String} value - ACL value
    * @return {String}
    */
   handleAcl: function (key, value) {
     if (value) {
       this.set(key, (value === '*')? '*' : ' ');
     }
     return (this.get(key) === '*' || this.get(key) == null) ? '*' : 'custom';
   },

   /**
    * Queue's acl_administer_queue property can be set to '*' (everyone) or ' ' (nobody) thru this property.
    *
    * @param  {String} key
    * @param  {String} value
    * @return {String} - '*' if equal to '*' or 'custom' in other case.
    */
   acl_administer_queue: function (key, value) {
     return this.handleAcl('content.acl_administer_queue', value);
   }.property('content.acl_administer_queue'),

   /**
    * Returns true if acl_administer_queue is set to '*'
    * @type {Boolean}
    */
   aaq_anyone: Ember.computed.equal('acl_administer_queue', '*'),

   /**
    * Returns effective permission of the current queue to perform administrative functions on this queue.
    */
    aaq_effective_permission: function(key, value){
      return this.getEffectivePermission('acl_administer_queue');
    }.property('content.acl_administer_queue'),

    /**
     * Queue's acl_submit_applications property can be set to '*' (everyone) or ' ' (nobody) thru this property.
     *
     * @param  {String} key
     * @param  {String} value
     * @return {String} - '*' if equal to '*' or 'custom' in other case.
     */
    acl_submit_applications: function (key, value) {
      return this.handleAcl('content.acl_submit_applications', value);
    }.property('content.acl_submit_applications'),

    /**
     * Returns true if acl_submit_applications is set to '*'
     * @type {Boolean}
     */
    asa_anyone:Ember.computed.equal('acl_submit_applications', '*'),

    /**
     * Returns effective permission of the current queue to submit application.
     */
    asa_effective_permission: function(key, value){
      return this.getEffectivePermission('acl_submit_applications');
    }.property('content.acl_submit_applications'),

    /**
     * Returns effective permission of the current queue.
     */
    getEffectivePermission: function(permissionType){
      var effectivePermission,
      users = [],
      groups = [],
      currentPermissions = this.getPermissions(permissionType);
      for(var i = 0; i < currentPermissions.length; i++){
        var permission = currentPermissions[i];
        if (permission === '*') {
          return '*';
        } else if (permission.trim() === '') {
          effectivePermission = '';
        } else {
          var usersAndGroups = permission.split(' ');
          this.fillUsersAndGroups(users, usersAndGroups[0]);
          if (usersAndGroups.length === 2) {
            this.fillUsersAndGroups(groups, usersAndGroups[1]);
          }
        }
      }
      if(users.length > 0 || groups.length > 0){
        effectivePermission = users.join(',') + ' ' + groups.join(',');
      }
      return effectivePermission;
    },

    /**
     * Removes duplicate users or groups.
     */
    fillUsersAndGroups: function(usersOrGroups, list){
      var splitted = list.split(',');
      splitted.forEach(function(item){
        if(usersOrGroups.indexOf(item) === -1){
          usersOrGroups.push(item);
        }
      });
    },

    /**
     * Returns array of permissions from root to leaf.
     */
    getPermissions: function(permissionType){
      var currentQ = this.get('content'),
      permissions = [];
      while (currentQ !== null) {
        if (currentQ.get(permissionType) !== null) {
          permissions.push(currentQ.get(permissionType));
        } else {
          permissions.push('*');
        }
        currentQ = this.store.getById('queue', currentQ.get('parentPath').toLowerCase());
      }
      permissions.reverse();//root permission at the 0th position.
      return permissions;
    },

    /**
     * Array of leaf queues.
     * @return {Array}
     */
    childrenQueues: function () {
      return this.get('allQueues')
        .filterBy('depth', this.get('content.depth') + 1)
        .filterBy('parentPath', this.get('content.path'));
    }.property('allQueues.length', 'content.path', 'content.parentPath'),

    /**
     * Parent of current queue.
     * @return {App.Queue}
     */
    parentQueue: function () {
      return this.store.getById('queue', this.get('content.parentPath').toLowerCase());
    }.property('content.parentPath'),

    /*
     * Returns true if the current queue is a leaf queue
     */
    isLeafQ: function() {
      return this.get('content.queues') == null;
    }.property('allQueues.length', 'content.queues'),

    childrenQueuesTotalCapacity: function() {
      var childrenQs = this.get('childrenQueues'),
      totalCapacity = 0;
      childrenQs.forEach(function(currentQ){
        totalCapacity += currentQ.get('capacity');
      });
      return totalCapacity;
    }.property('childrenQueues.length', 'childrenQueues.@each.capacity'),

    widthPattern: 'width: %@%',

    warnInvalidCapacity: function() {
      var totalCap = this.get('childrenQueuesTotalCapacity');
      if (totalCap > 100 || totalCap < 100) {
        return true;
      }
      return false;
    }.property('childrenQueuesTotalCapacity'),

    totalCapacityBarWidth: function() {
      var totalCap = this.get('childrenQueuesTotalCapacity');
      if (totalCap > 100) {
        totalCap = 100;
      }
      return this.get('widthPattern').fmt(totalCap);
    }.property('childrenQueuesTotalCapacity'),

   /**
    * Adds observers for each queue attribute.
    * @method dirtyObserver
    */
   dirtyObserver: function () {
     this.get('content.constructor.transformedAttributes.keys.list').forEach(function(item) {
       this.addObserver('content.' + item, this, 'propertyBecomeDirty');
     }.bind(this));
   }.observes('content'),

   /**
    * Adds modified queue fileds to q queueDirtyFields collection.
    * @param  {String} controller
    * @param  {String} property
    * @method propertyBecomeDirty
    */
   propertyBecomeDirty: function (controller, property) {
     var queueProp = property.split('.').objectAt(1);
     this.set('queueDirtyFields.' + queueProp, this.get('content').changedAttributes().hasOwnProperty(queueProp));
   }
});
