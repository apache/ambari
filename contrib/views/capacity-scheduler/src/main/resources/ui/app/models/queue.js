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

App.Label = DS.Model.extend({
  queue: DS.belongsTo('queue'),
  capacity: DS.attr('number', { defaultValue: 0 }),
  maximum_capacity: DS.attr('number', { defaultValue: 0 }),
  forQueue:function() {
    return this.get('id').substring(0,this.get('id').lastIndexOf('.'));
  }.property('id'),
  name:function() {
    return this.get('id').substr(this.get('id').lastIndexOf('.')+1);
  }.property('id'),

  overCapacity:false,
  isNotExist:function () {
    return this.get('store.nodeLabels.content').findBy('name',this.get('name')).notExist;
  }.property('store.nodeLabels.content.@each.notExist')
});

App.Scheduler = DS.Model.extend({
  maximum_am_resource_percent: DS.attr('number', { defaultValue: 0 }),
  maximum_applications: DS.attr('number', { defaultValue: 0 }),
  node_locality_delay: DS.attr('number', { defaultValue: 0 }),
  resource_calculator: DS.attr('string', { defaultValue: '' })
});


/**
 * Represents tagged of configuraion vresion.
 *
 */
App.Tag = DS.Model.extend({
  tag:DS.attr('string'),
  isCurrent:function () {
    return this.get('tag') === this.get('store.current_tag');
  }.property('store.current_tag'),
  changed:function () {
    return (this.get('tag').match(/version[1-9]+/))?moment(+this.get('tag').replace('version','')).fromNow():'';
  }.property('tag'),
  updateTime:function () {
    Em.run.later(this,function () {
      this.trigger('tick');
      this.notifyPropertyChange('tag');
    },5000);
  }.on('init','tick')
});

/**
 * Represents the queue.
 *
 */
App.Queue = DS.Model.extend({
  labels: DS.hasMany('label'),
  sortBy:['name'],
  sortedLabels:Em.computed.sort('labels','sortBy'),

  _accessAllLabels:DS.attr('boolean'),
  accessAllLabels:function (key,val) {
    var labels = this.get('store.nodeLabels').map(function(label) {
      return this.store.getById('label',[this.get('id'),label.name].join('.'));
    }.bind(this));

    if (arguments.length > 1) {
      this.set('_accessAllLabels',val);

      if (this.get('_accessAllLabels')) {
          labels.forEach(function(lb) {

            var containsByParent = (Em.isEmpty(this.get('parentPath')))?true:this.store.getById('queue',this.get('parentPath')).get('labels').findBy('name',lb.get('name'));
            if (!this.get('labels').contains(lb) && !!containsByParent) {
              this.get('labels').pushObject(lb);
              this.notifyPropertyChange('labels');
            }
          }.bind(this));
      }
    }

    if (this.get('labels.length') != labels.get('length')) {
      this.set('_accessAllLabels',false);
    }

    return this.get('_accessAllLabels');
  }.property('_accessAllLabels','labels.[]'),

  isAnyDirty: function () {
    return this.get('isDirty') || !Em.isEmpty(this.get('labels').findBy('isDirty',true)) || this.get('isLabelsDirty');
  }.property('isDirty','labels.@each.isDirty','initialLabels','isLabelsDirty'),

  initialLabels:[],
  labelsLoad:function() {
    this.set('initialLabels',this.get('labels').mapBy('id'));
  }.on('didLoad','didUpdate','didCreate'),

  isLabelsDirty:function () {
    var il = this.get('initialLabels').sort();
    var cl = this.get('labels').mapBy('id').sort();
    return !((il.length == cl.length) && il.every(function(element, index) {
      return element === cl[index];
    }));
  }.property('initialLabels', 'labels.[]', '_accessAllLabels'),

  name: DS.attr('string'),
  parentPath: DS.attr('string'),
  depth: DS.attr('number'),
  path: DS.attr('string'),

  // queue props
  state: DS.attr('string', { defaultValue: 'RUNNING' }),
  acl_administer_queue: DS.attr('string', { defaultValue: '*' }),
  acl_submit_applications: DS.attr('string', { defaultValue: '*' }),

  capacity: DS.attr('number', { defaultValue: 0 }),
  maximum_capacity: DS.attr('number', { defaultValue: 0 }),
  //unfunded_capacity: DS.attr('number', { defaultValue: 0 }),

  user_limit_factor: DS.attr('number', { defaultValue: 1 }),
  minimum_user_limit_percent: DS.attr('number', { defaultValue: 100 }),
  maximum_applications: DS.attr('number', { defaultValue: null }),
  maximum_am_resource_percent: DS.attr('number', { defaultValue: null }),

  queues: DS.attr('string'),
  queuesArray:function (key,val) {
    var qrray;
    if (arguments.length > 1) {
      if (typeof val === 'object' && val.hasOwnProperty('exclude')) {
        qrray = (this.get('queues'))?this.get('queues').split(','):[];
        this.set('queues',qrray.removeObject(val.exclude).join(',') || null);
      } else {
        this.set('queues',val.join(',') || null);
      }
    }
    return (this.get('queues'))?this.get('queues').split(','):[];
  }.property('queues'),

  _overCapacity:false,
  overCapacity:function(key,val) {
    if (arguments.length > 1) {
      this.set('_overCapacity',val);
    }

    return this.get('_overCapacity') || !Em.isEmpty(this.get('labels').filterBy('overCapacity'));
  }.property('_overCapacity','labels.@each.overCapacity'),

  //new queue flag
  isNewQueue:DS.attr('boolean', {defaultValue: false}),

  version:null,
  clearTag:function () {
    this.set('version', null);
  }.observes(
    'name',
    'parentPath',
    'depth',
    'path',
    'state',
    'acl_administer_queue',
    'acl_submit_applications',
    'capacity',
    'maximum_capacity',
    'unfunded_capacity',
    'user_limit_factor',
    'minimum_user_limit_percent',
    'maximum_applications',
    'maximum_am_resource_percent',
    'queues',
    'labels.@each.capacity',
    'labels.@each.maximum_capacity'
  )
});
