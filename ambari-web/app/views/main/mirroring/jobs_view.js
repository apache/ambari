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

  didInsertElement: function () {
    this.set('content', this.get('controller.jobs'));
  },

  dataset: function () {
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
    fieldType: 'input-medium',
    column: 3,
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  durationFilterView: filters.createTextView({
    fieldType: 'input-medium',
    column: 4,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'duration');
    }
  }),

  dataFilterView: filters.createTextView({
    fieldType: 'input-small',
    column: 5,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'ambari-bandwidth');
    }
  }),

  JobView: Em.View.extend({
    content: null,
    tagName: 'tr',
    canActionBeTaken: function () {
      var job_status = this.get('content.status');

      if (job_status == "RUNNING" || job_status == "SUSPENDED") {
        return true;
      }

      return false;

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
        case 'KILLED' :
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
    },

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
