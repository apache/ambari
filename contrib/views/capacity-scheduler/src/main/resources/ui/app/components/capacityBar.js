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

App.CapacityBarComponent = Em.Component.extend({
  layoutName:'components/capacityBar',
  classNames:['capacity-bar'],
  mark:function () {
    if (this.$().parent().hasClass('active')) {
      Ember.run.next(this, 'addInClass');
    };
  }.on('didInsertElement'),
  addInClass:function () {
    this.$().children().addClass('in');
  },
  capacityWidth: function() {
    return  (Number(this.get('capacityValue'))<=100)?('width: '+this.get('capacityValue')+'%'):'width: 100%';
  }.property('capacityValue'),
  maxCapacityWidth: function() {
    var val = Number(this.get('maxCapacityValue')) - Number(this.get('capacityValue'));
    return  (val<=100)?('width: '+val+'%'):'width: 100%';
  }.property('maxCapacityValue','capacityValue'),
  showMaxCapacity:function () {
    return this.get('maxCapacityValue') > this.get('capacityValue');
  }.property('maxCapacityValue','capacityValue')
});
