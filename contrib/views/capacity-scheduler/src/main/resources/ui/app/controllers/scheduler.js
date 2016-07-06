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

App.CapschedSchedulerController = Ember.Controller.extend({
  needs: ['capsched'],
  scheduler: cmp.alias('controllers.capsched.content'),
  schedulerProps: ['maximum_am_resource_percent', 'maximum_applications', 'node_locality_delay', 'resource_calculator'],

  actions: {
    rollbackSchedulerProps: function() {
      var sched = this.get('scheduler'),
      attributes = sched.changedAttributes(),
      props = this.schedulerProps;
      props.forEach(function(prop) {
        if (attributes.hasOwnProperty(prop)) {
          sched.set(prop, attributes[prop][0]);
        }
      });
    },
    showConfirmDialog: function() {
      this.set('isConfirmDialogOpen', true);
    },
    showSaveConfigDialog: function(mode) {
      if (mode) {
        this.set('saveMode', mode);
      } else {
        this.set('saveMode', '');
      }
      this.set('isSaveConfigDialogOpen', true);
    }
  },

  isOperator: cmp.alias('controllers.capsched.isOperator'),

  saveMode: '',

  isConfirmDialogOpen: false,
  isSaveConfigDialogOpen: false,

  configNote: cmp.alias('store.configNote'),

  isSchedulerDirty: false,

  schedulerBecomeDirty: function() {
    var sched = this.get('scheduler'),
    attributes = sched.changedAttributes(),
    props = this.schedulerProps;
    var isDirty = props.any(function(prop){
      return attributes.hasOwnProperty(prop);
    });
    this.set('isSchedulerDirty', isDirty);
  }.observes('scheduler.maximum_am_resource_percent', 'scheduler.maximum_applications', 'scheduler.node_locality_delay', 'scheduler.resource_calculator'),

  /**
   * Collection of modified fields in Scheduler.
   * @type {Object} - { [fileldName] : {Boolean} }
   */
  schedulerDirtyFilelds: {},

  dirtyObserver:function () {
    this.get('scheduler.constructor.transformedAttributes.keys.list').forEach(function(item) {
      this.addObserver('scheduler.' + item, this, 'propertyBecomeDirty');
    }.bind(this));
  }.observes('scheduler').on('init'),

  propertyBecomeDirty:function (controller, property) {
    var schedProp = property.split('.').objectAt(1);
    this.set('schedulerDirtyFilelds.' + schedProp, this.get('scheduler').changedAttributes().hasOwnProperty(schedProp));
  },

  resourceCalculatorValues: [{
    label: 'Default Resource Calculator',
    value: 'org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator'
  }, {
    label: 'Dominant Resource Calculator',
    value: 'org.apache.hadoop.yarn.util.resource.DominantResourceCalculator'
  }]
});
