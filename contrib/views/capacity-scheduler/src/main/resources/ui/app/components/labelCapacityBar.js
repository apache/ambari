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

App.LabelCapacityBarComponent = Ember.Component.extend({
  layoutName: 'components/labelCapacityBar',
  queueLabels: null,
  labels: null,
  queues: null,

  extractLabels: function() {
    var qLabels = this.get('queueLabels'),
    labels = [], queues = [];
    qLabels.forEach(function(labelObj) {
      labels.pushObject(labelObj.label);
      queues.pushObject(labelObj.queue);
    });
    this.set('labels', labels);
    this.set('queues', queues);
  }.observes('queueLabels.length').on('init'),

  childrenQueueLabelsTotalCapacity: function() {
    var labels = this.get('labels'),
    totalCapacity = 0;
    labels.forEach(function(label){
      totalCapacity += label.get('capacity');
    });
    return totalCapacity;
  }.property('labels.length', 'labels.@each.capacity'),

  widthPattern: 'width: %@%',

  warnInvalidLabelCapacity: function() {
    var totalCap = this.get('childrenQueueLabelsTotalCapacity');
    var isInvalid = false;
    if (totalCap > 100 || totalCap < 100) {
      isInvalid = true;
    }
    this.get('labels').setEach('overCapacity', isInvalid);
    return isInvalid;
  }.property('childrenQueueLabelsTotalCapacity'),

  totalLabelCapacityBarWidth: function() {
    var totalCap = this.get('childrenQueueLabelsTotalCapacity');
    if (totalCap > 100) {
      totalCap = 100;
    }
    return this.get('widthPattern').fmt(totalCap);
  }.property('childrenQueueLabelsTotalCapacity'),

  isAnyQueueLabelsEnabled: function() {
    var qlabels = [],
    isAnyEnabled = false;
    this.get('queues').forEach(function(qq){
      qq.get('labels').forEach(function(lab){
        qlabels.addObject(lab);
      });
    });
    this.get('labels').forEach(function(label){
      if (qlabels.contains(label)) {
        isAnyEnabled = true;
        return false;
      }
    });
    return isAnyEnabled;
  }.property('queues.@each.labels.[]')
});
