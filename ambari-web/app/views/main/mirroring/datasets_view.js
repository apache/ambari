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

App.MainDatasetsView = App.TableView.extend({
  name: 'mainDatasetsView',
  templateName: require('templates/main/mirroring/datasets'),
  pagination: false,
  content: function () {
    return this.get('controller.datasets');
  }.property('controller.datasets.length'),

  didInsertElement: function () {
    this.filter();
  },

  sortView: sort.wrapperView,
  nameSort: sort.fieldView.extend({
    column: '1',
    name: 'name',
    displayName: Em.I18n.t('common.name')
  }),

  statusSort: sort.fieldView.extend({
    column: '2',
    name: 'status',
    displayName: Em.I18n.t('mirroring.table.datasetStatus')
  }),

  /**
   * Filter view for name column
   * Based on <code>filters</code> library
   */
  nameFilterView: filters.createTextView({
    fieldType: 'input-small',
    column: 1,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  statusFilterView: filters.createSelectView({
    fieldType: 'input-small',
    column: 2,
    content: ['Any', 'Scheduled', 'Suspended', 'Running'],
    onClearValue: function () {
      if (this.get('value') === '') {
        this.set('value', 'Any');
      }
    }.observes('value'),
    onChangeValue: function () {
      var value = this.get('value');
      if (value === 'Any') {
        value = '';
      }
      this.get('parentView').updateFilter(this.get('column'), value, 'string');
    }
  }),

  DatasetView: Em.View.extend({
    content: null,
    tagName: 'tr',

    click: function () {
      App.router.send('gotoShowJobs', this.get('content'));
    },

    classNameBindings: ['selectedClass', ':dataset-item'],

    selectedClass: function () {
      return this.get('controller.selectedDataset.id') === this.get('content.id') ? 'dataset-selected' : '';
    }.property('controller.selectedDataset'),

    lastDurationFormatted: function () {
      var milliseconds = this.get('content.lastDuration');
      var h = Math.floor(milliseconds / 3600000);
      var m = Math.floor((milliseconds % 3600000) / 60000);
      var s = Math.floor(((milliseconds % 360000) % 60000) / 1000);
      return (h == 0 ? '' : h + 'hr ') + (m == 0 ? '' : m + 'mins ') + (s == 0 ? '' : s + 'secs ');
    }.property('content.lastDuration'),

    lastSucceededDateFormatted: function () {
      if (this.get('content.lastSucceededDate')) {
        return $.timeago(this.get('content.lastSucceededDate'));
      }
    }.property('content.lastSucceededDate'),

    lastFailedDateFormatted: function () {
      if (this.get('content.lastFailedDate')) {
        return $.timeago(this.get('content.lastFailedDate'));
      }
    }.property('content.lastFailedDate')
  }),

  /**
   * associations between dataset property and column index
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'name';
    associations[2] = 'status';
    return associations;
  }.property()

});
