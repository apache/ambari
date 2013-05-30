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
  content: function () {
    return this.get('controller.datasets');
  }.property('controller.datasets'),

  jobs: function () {
    return App.DataSetJob.find().slice(0, 2);
  }.property(),

  targetClusters: function () {
    return this.get('controller.targetClusters');
  }.property('controller.targetClusters'),

  showClusterPopup: function (event) {
    return App.ModalPopup.show({
      header: Em.I18n.t('mirroring.sidebar.popup.clusters.header'),
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile("{{t mirroring.sidebar.popup.clusters.body}}")
      }),
      onPrimary: function () {
        this.hide();
      }
    });
  },

  sortView: sort.wrapperView,
  nameSort: sort.fieldView.extend({
    name: 'name',
    displayName: Em.I18n.t('common.name')
  }),
  dataSetSourceSort: sort.fieldView.extend({
    name: 'sourceDir',
    displayName: Em.I18n.t('mirroring.table.datasetSource')
  }),
  lastSuccessSort: sort.fieldView.extend({
    name: 'lastSucceededDate',
    displayName: Em.I18n.t('mirroring.table.lastSuccess'),
    type: 'number'
  }),
  lastFailSort: sort.fieldView.extend({
    name: 'lastFailedDate',
    displayName: Em.I18n.t('mirroring.table.lastFail'),
    type: 'number'
  }),
  lastDurationSort: sort.fieldView.extend({
    name: 'lastDuration',
    displayName: Em.I18n.t('mirroring.table.lastDuration'),
    type: 'number'
  }),
  avgDataSort: sort.fieldView.extend({
    name: 'avgData',
    displayName: Em.I18n.t('mirroring.table.avgData'),
    type: 'number'
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

  datasetSourceFilterView: filters.createTextView({
    fieldType: 'input-small',
    column: 2,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  lastSuccessFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    column: 3,
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  lastFailFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    column: 4,
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  lastDurationFilterView: filters.createTextView({
    fieldType: 'input-small',
    column: 5,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'duration');
    }
  }),

  avgDataFilterView: filters.createTextView({
    fieldType: 'input-small',
    column: 6,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'ambari-bandwidth');
    }
  }),

  DatasetView: Em.View.extend({
    content: null,
    tagName: 'tr',

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
    associations[2] = 'sourceDir';
    associations[3] = 'lastSucceededDate';
    associations[4] = 'lastFailedDate';
    associations[5] = 'lastDuration';
    associations[6] = 'avgData';
    return associations;
  }.property()

});
