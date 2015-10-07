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

require('views/common/time_range_list');

App.TimeRangeMixin = Em.Mixin.create({

  timeRangeClassName: 'pull-right',

  /**
   * time range options for service metrics, a dropdown will list all options
   * value set in hours
   */
  timeRangeOptions: [
    {index: 0, name: Em.I18n.t('graphs.timeRange.hour'), value: '1'},
    {index: 1, name: Em.I18n.t('graphs.timeRange.twoHours'), value: '2'},
    {index: 2, name: Em.I18n.t('graphs.timeRange.fourHours'), value: '4'},
    {index: 3, name: Em.I18n.t('graphs.timeRange.twelveHours'), value: '12'},
    {index: 4, name: Em.I18n.t('graphs.timeRange.day'), value: '24'},
    {index: 5, name: Em.I18n.t('graphs.timeRange.week'), value: '168'},
    {index: 6, name: Em.I18n.t('graphs.timeRange.month'), value: '720'},
    {index: 7, name: Em.I18n.t('graphs.timeRange.year'), value: '8760'}
  ],

  currentTimeRangeIndex: 0,

  currentTimeRange: function() {
    return this.get('timeRangeOptions').objectAt(this.get('currentTimeRangeIndex'));
  }.property('currentTimeRangeIndex'),

  /**
   * onclick handler for a time range option
   * @param {object} event
   */
  setTimeRange: function (event) {
    this.set('currentTimeRangeIndex', event.context.index);
  },

  timeRangeListView: App.TimeRangeListView.extend()

});
