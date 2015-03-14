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
  actions:{
    setState:function (state) {
      this.set('content.state', (state === "running") ? (this.get('content.state') == null) ? null : _runState : _stopState );
    },
    createQ:function () {
      this.get('controllers.queues').send('createQ',this.get('content'));
    },
    addQ:function (path) {
      this.get('controllers.queues').send('addQ',path);
    },
    delQ:function (record) {
      this.get('controllers.queues').send('delQ',record);
    },
    renameQ:function (opt) {
      if (opt == 'ask') {
        this.set('tmpName',{name:this.get('content.name'),path:this.get('content.path')});
        this.get('content').addObserver('name',this,this.setQueuePath);
        this.toggleProperty('isRenaming');
        return;
      }
      if (opt == 'cancel') {
        this.get('content').removeObserver('name',this,this.setQueuePath);
        this.get('content').setProperties({
          name:this.get('tmpName.name'),
          id:this.get('tmpName.path'),
          path:this.get('tmpName.path')
        });
        this.toggleProperty('isRenaming');
        return;
      }
      if (opt && !this.get('content').get('errors.path')) {

        this.store.filter('label',function (label){
          return label.get('forQueue') == this.get('tmpName.path');
        }.bind(this)).then(function (labels) {
          labels.forEach(function (label) {
            label.materializeId([this.get('id'),label.get('name')].join('.'));
            label.store.updateId(label,label);
          }.bind(this))
        }.bind(this));
        this.toggleProperty('isRenaming');
        this.get('content').removeObserver('name',this,this.setQueuePath);
        this.store.updateId(this.get('content'),this.get('content'));
        this.transitionToRoute('queue',this.get('content.id'));

      }

    },
    // TODO bubble to route
    rollbackProp:function(prop, queue){
      queue = queue || this.get('content');
      attributes = queue.changedAttributes();
      if (attributes.hasOwnProperty(prop)) {
        queue.set(prop,attributes[prop][0]);
      }
    }
  },

  /**
   * Collection of modified fields in queue.
   * @type {Object} - { [fileldName] : {Boolean} }
   */
  queueDirtyFilelds:{},

  /**
   * Represents renaming status.
   * @type {Boolean}
   */
  isRenaming:false,

  /**
   * Object contains temporary name and path while renaming the queue.
   * @type {Object} - { name : {String}, path : {String} }
   */
  tmpName:{},



  // COMPUTED PROPERTIES

  /**
   * Alias for user admin status.
   * @type {Boolean}
   */
  isOperator:Em.computed.alias('controllers.queues.isOperator'),

  /**
   * Inverted user admin status.
   * @type {Boolean}
   */
  isNotOperator:Em.computed.alias('controllers.queues.isNotOperator'),

  /**
   * All queues in store.
   * @type {DS.RecordArray}
   */
  allQueues:Em.computed.alias('controllers.queues.content'),

  /**
   * Array of leaf queues.
   * @return {Array}
   */
  leafQueues:function () {
    return this.get('allQueues').filterBy('parentPath',this.get('content.parentPath'));
  }.property('allQueues.length','content.parentPath'),

  /**
   * Parent of current queue.
   * @return {App.Queue}
   */
  parentQueue: function () {
    return this.store.getById('queue',this.get('content.parentPath'));
  }.property('content.parentPath'),

  /**
   * Returns true if queue is root.
   * @type {Boolean}
   */
  isRoot:Ember.computed.match('content.id', /^(root|root.default)$/),

  /**
   * Represents queue run state. Returns true if state is null.
   * @return {Boolean}
   */
  isRunning: function() {
    return this.get('content.state') == _runState || this.get('content.state') == null;
  }.property('content.state'),

  /**
   * Queue's acl_administer_queue property can be set to '*' (everyone) or ' ' (nobody) thru this property.
   *
   * @param  {String} key
   * @param  {String} value
   * @return {String} - '*' if equal to '*' or 'custom' in other case.
   */
  acl_administer_queue: function (key, value) {
    return this.handleAcl('content.acl_administer_queue',value);
  }.property('content.acl_administer_queue'),

  /**
   * Returns true if acl_administer_queue is set to '*'
   * @type {Boolean}
   */
  aaq_anyone:Ember.computed.equal('acl_administer_queue', '*'),

  /**
   * Queue's acl_submit_applications property can be set to '*' (everyone) or ' ' (nobody) thru this property.
   *
   * @param  {String} key
   * @param  {String} value
   * @return {String} - '*' if equal to '*' or 'custom' in other case.
   */
  acl_submit_applications: function (key, value) {
    return this.handleAcl('content.acl_submit_applications',value);
  }.property('content.acl_submit_applications'),

  /**
   * Returns true if acl_submit_applications is set to '*'
   * @type {Boolean}
   */
  asa_anyone:Ember.computed.equal('acl_submit_applications', '*'),

  /**
   * Error messages for queue path.
   * @type {[type]}
   */
  pathErrors:Ember.computed.mapBy('content.errors.path','message'),



  // OBSERVABLES

  /**
   * Marks each queue in leaf with 'overCapacity' if sum if their capacity values is greater then 100.
   * @method capacityControl
   */
  capacityControl:function () {
    var leafQueues = this.get('leafQueues'),
        total = leafQueues.reduce(function (prev, queue) {
          return +queue.get('capacity') + prev;
        },0);

    leafQueues.setEach('overCapacity',total>100);
  }.observes('content.capacity','leafQueues.@each.capacity'),

  /**
   * Keeps track of leaf queues and sets 'queues' value of parent to list of their names.
   * @method queueNamesControl
   */
  queueNamesControl:function () {
    if (this.get('parentQueue')) this.set('parentQueue.queuesArray',this.get('leafQueues').mapBy('name'));
  }.observes('allQueues.length','allQueues.@each.name','content'),

  /**
   * Adds observers for each queue attribute.
   * @method dirtyObserver
   */
  dirtyObserver:function () {
    this.get('content.constructor.transformedAttributes.keys.list').forEach(function(item) {
      this.addObserver('content.' + item,this,'propertyBecomeDirty');
    }.bind(this));
  }.observes('content'),



  // METHODS

  /**
   * Sets ACL value to '*' or ' ' and returns '*' and 'custom' respectively.
   * @param  {String} key   - ACL attribute
   * @param  {String} value - ACL value
   * @return {String}
   */
  handleAcl:function (key,value) {
    if (value) {
      this.set(key,(value == '*')?'*':' ');
    }
    return (this.get(key) == '*' || this.get(key) == null) ? '*' : 'custom';
  },

  /**
   * Sets queue path and id in accordance with the name.
   * Also adds errors to queue if name is incorrect.
   *
   * @method setQueuePath
   */
  setQueuePath:function (queue) {
    var name = queue.get('name').replace(/\s|\./g, ''),
        parentPath = queue.get('parentPath');

    queue.setProperties({
      name:name,
      path:parentPath+'.'+name,
      id:(parentPath+'.'+name).dasherize()
    });

    if (name == '') {
      queue.get('errors').add('path', 'This field is required');
    }

    this.store.filter('queue',function (q) {
      return q.get('id') === queue.get('id');
    }.bind(this)).then(function (queues){
      if (queues.get('length') > 1) {
        return this.get('content').get('errors').add('path', 'Queue already exists');
      }
    }.bind(this));
  },

  /**
   * Adds modified queue fileds to q queueDirtyFilelds collection.
   * @param  {String} controller
   * @param  {String} property
   * @method propertyBecomeDirty
   */
  propertyBecomeDirty:function (controller, property) {
    var queueProp = property.split('.').objectAt(1);
    this.set('queueDirtyFilelds.' + queueProp, this.get('content').changedAttributes().hasOwnProperty(queueProp));
  }
});

App.ErrorController = Ember.ObjectController.extend();
