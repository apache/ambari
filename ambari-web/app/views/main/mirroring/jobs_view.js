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

App.MainDatasetJobsView = App.TableView.extend({
  templateName: require('templates/main/mirroring/jobs'),
  content: function () {
    return this.get('controller.jobs');
  }.property('controller.jobs', 'controller.jobs.length'),

  dataset: function () {
    return this.get('controller.content');
  }.property('controller.content'),

  sortView: sort.wrapperView,
  idSort: sort.fieldView.extend({
    column: 1,
    name: 'name',
    displayName: Em.I18n.t('mirroring.table.jobId'),
    type: 'string'
  }),
  startSort: sort.fieldView.extend({
    column: 2,
    name: 'startDate',
    displayName: Em.I18n.t('mirroring.table.start'),
    type: 'number'
  }),
  endSort: sort.fieldView.extend({
    column: 3,
    name: 'endDate',
    displayName: Em.I18n.t('mirroring.table.end'),
    type: 'number'
  }),
  statusSort: sort.fieldView.extend({
    column: 4,
    name: 'statusFormatted',
    displayName: Em.I18n.t('mirroring.table.status'),
    type: 'string'
  }),

  /**
   * Filter view for name column
   * Based on <code>filters</code> library
   */
  idFilterView: filters.createTextView({
    fieldType: 'input-small',
    column: 1,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'number');
    }
  }),

  startFilterView: filters.createSelectView({
    fieldType: 'input-small',
    column: 2,
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  endFilterView: filters.createSelectView({
    fieldType: 'input-small',
    column: 3,
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  statusFilterView: filters.createSelectView({
    fieldType: 'input-small',
    column: 4,
    content: ['Any', 'Waiting', 'Running', 'Suspended', 'Killed', 'Failed', 'Succeeded', 'Error'],
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

  JobView: Em.View.extend({
    content: null,
    tagName: 'tr',
    canActionBeTaken: function () {
      var job_status = this.get('content.status');

      return job_status == "RUNNING" || job_status == "SUSPENDED";
    }.property('content.status'),

    isKilled: function () {
      var job_status = this.get('content.status');
      return job_status == 'KILLED';
    }.property(),

    statusClass: function () {
      var job_status = this.get('content.status');
      switch (job_status) {
        case 'RUNNING' :
          return "btn btn-success dropdown-toggle";
          break;
        case 'SUSPENDED' :
          return "btn btn-warning dropdown-toggle";
          break;
        case 'SUCCEEDED' :
          return "label label-success";
          break;
        case 'KILLED' :
          return "label label-important";
          break;
        case 'WAITING' :
          return "label";
          break;
        case 'FAILED' :
        case 'ERROR' :
          return "label label-important";
          break;
        default :
          return "label";
          break;
      }
    }.property('content.status'),

    listOfOptions: function () {
      var listOfActions = [];
      var status = this.get('content.status');
      switch (status) {
        case 'RUNNING' :
          listOfActions.push({title: 'Suspend', value: 'Suspend'});
          listOfActions.push({title: 'Abort', value: 'Abort'});
          break;
        case 'SUSPENDED' :
          listOfActions.push({title: 'Resume', value: 'Resume'});
          listOfActions.push({title: 'Abort', value: 'Abort'});
          break;
      }
      return listOfActions;
    }.property('content.status'),

    changeStatus: function (event) {
      var selected = event.context;
      var self = this;
      App.showConfirmationPopup(function () {
        switch (selected.title) {
          case 'Suspend' :
            self.set('content.status', 'SUSPENDED');
            break;
          case 'Resume' :
            self.set('content.status', 'RUNNING');
            break;
          case 'Abort' :
            self.set('content.status', 'KILLED');
            break;
        }
      });
    }
  }),

  /**
   * associations between dataset property and column index
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'name';
    associations[2] = 'startDate';
    associations[3] = 'endDate';
    associations[4] = 'statusFormatted';
    return associations;
  }.property()



});
