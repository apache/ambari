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
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');

App.MainJobsView = App.TableView.extend({
  templateName: require('templates/main/mirroring/jobs'),
  content: function () {
    return this.get('controller.jobs');
  }.property('controller.jobs'),

  didInsertElement: function() {
    this.set('content', this.get('controller.jobs'));
  },

  dataset: function() {
    return this.get('controller.content');
  }.property('controller.content'),

  sortView: sort.wrapperView,
  idSort: sort.fieldView.extend({
    name: 'id',
    displayName: Em.I18n.t('mirroring.table.jobId'),
    type: 'number'
  }),
  startSort: sort.fieldView.extend({
    name: 'startDate',
    displayName: Em.I18n.t('mirroring.table.start'),
    type: 'number'
  }),
  endSort: sort.fieldView.extend({
    name: 'endDate',
    displayName: Em.I18n.t('mirroring.table.end'),
    type: 'number'
  }),
  durationSort: sort.fieldView.extend({
    name: 'duration',
    displayName: Em.I18n.t('mirroring.table.duration'),
    type: 'number'
  }),
  dataSort: sort.fieldView.extend({
    name: 'data',
    displayName: Em.I18n.t('mirroring.table.data'),
    type: 'number'
  }),

  /**
   * Filter view for name column
   * Based on <code>filters</code> library
   */
  idFilterView: filters.createTextView({
    fieldType: 'input-small',
    onChangeValue: function () {
      this.get('parentView').updateFilter(1, this.get('value'), 'number');
    }
  }),

  startFilterView: filters.createSelectView({
    fieldType: 'input-small',
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(2, this.get('value'), 'date');
    }
  }),

  endFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(3, this.get('value'), 'date');
    }
  }),

  durationFilterView: filters.createTextView({
    fieldType: 'input-medium',
    onChangeValue: function () {
      this.get('parentView').updateFilter(4, this.get('value'), 'duration');
    }
  }),

  dataFilterView: filters.createTextView({
    fieldType: 'input-small',
    onChangeValue: function () {
      this.get('parentView').updateFilter(5, this.get('value'), 'ambari-bandwidth');
    }
  }),

  JobView: Em.View.extend({
    content: null,
    tagName: 'tr',

    durationFormatted: function () {
      var milliseconds = this.get('content.duration');
      var h = Math.floor(milliseconds / 3600000);
      var m = Math.floor((milliseconds % 3600000) / 60000);
      var s = Math.floor(((milliseconds % 360000) % 60000) / 1000);
      return (h == 0 ? '' : h + 'hr ') + (m == 0 ? '' : m + 'mins ') + (s == 0 ? '' : s + 'secs ');
    }.property('content.duration'),

    startFormatted: function () {
      if (this.get('content.startDate')) {
        return $.timeago(this.get('content.startDate'));
      }
    }.property('content.startDate'),

    endFormatted: function () {
      if (this.get('content.endDate')) {
        return $.timeago(this.get('content.endDate'));
      }
    }.property('content.endDate')
  }),

  /**
   * associations between dataset property and column index
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'id';
    associations[2] = 'startDate';
    associations[3] = 'endDate';
    associations[4] = 'duration';
    associations[5] = 'data';
    return associations;
  }.property()

});
