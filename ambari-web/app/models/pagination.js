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

/**
 @extends Ember.Mixin

 Implements common pagination management properties for controllers.
 */
App.Pagination = Em.Mixin.create({

  total: 0,
  rangeStart: 0,
  pageSize: 0,

  rangeStop: function() {
    var rangeStop = this.get('rangeStart') + this.get('pageSize'),
      total = this.get('total');
    if (rangeStop < total) {
      return rangeStop;
    }
    return total;
  }.property('total', 'rangeStart', 'pageSize').cacheable(),

  hasPrevious: function() {
    return this.get('rangeStart') > 0;
  }.property('rangeStart').cacheable(),

  hasNext: function() {
    return this.get('rangeStop') < this.get('total');
  }.property('rangeStop', 'total').cacheable(),

  nextPage: function() {
    if (this.get('hasNext')) {
      this.incrementProperty('rangeStart', this.get('pageSize'));
    }
  },

  previousPage: function() {
    if (this.get('hasPrevious')) {
      this.decrementProperty('rangeStart', this.get('pageSize'));
    }
  },

  currentPage: function () {
    return this.get('rangeStop') / this.get('pageSize');
  }.property('rangeStop', 'pageSize').cacheable(),

  startPosition: function() {
    if (this.get('total') == 0)
      return 0;
    return this.get('rangeStart')  + 1;
  }.property('rangeStart', 'total').cacheable(),

  totalPages: function() {
    return Math.ceil(this.get('total') / this.get('pageSize'));
  }.property('total', 'pageSize').cacheable(),

//  changeContent: function() {
////    this.didRequestRange(this.get('rangeStart'), this.get('rangeStop'));
//  }.observes('total', 'rangeStart', 'rangeStop'),

  pageSizeChange: function() {
    this.set('rangeStart', 0);
  }.observes('pageSize')

});