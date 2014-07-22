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

App.MainConfigHistoryView = App.TableView.extend({
  templateName: require('templates/main/dashboard/config_history'),

  controllerBinding: 'App.router.mainConfigHistoryController',

  content: function () {
    return this.get('controller.content');
  }.property('controller.content'),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: function () {
    return this.t('hosts.filters.filteredHostsInfo').format(this.get('filteredCount'), this.get('content.length'));
  }.property('filteredCount', 'totalCount'),


  didInsertElement: function () {
    this.set('controller.isPolling', true);
    this.get('controller').load();
  },

  /**
   * stop polling after leaving config history page
   */
  willDestroyElement: function () {
    this.set('controller.isPolling', false);
  },

  sortView: sort.serverWrapperView,
  versionSort: sort.fieldView.extend({
    column: 1,
    name: 'serviceVersion',
    displayName: Em.I18n.t('dashboard.configHistory.table.version.title'),
    classNames: ['first']
  }),
  modifiedSort: sort.fieldView.extend({
    column: 2,
    name: 'createTime',
    displayName: Em.I18n.t('dashboard.configHistory.table.modified.title')
  }),
  authorSort: sort.fieldView.extend({
    column: 3,
    name: 'author',
    displayName: Em.I18n.t('common.author')
  }),
  notesSort: sort.fieldView.extend({
    column: 4,
    name: 'notes',
    displayName: Em.I18n.t('common.notes')
  }),

  versionFilterView: filters.createSelectView({
    column: 1,
    fieldType: 'filter-input-width',
    content: ['All'],
    valueBinding: "controller.filterObject.version",
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    },
    emptyValue: Em.I18n.t('common.all')
  }),

  modifiedFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    content: ['Any'],
    valueBinding: "controller.filterObject.modified",
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  authorFilterView: filters.createTextView({
    column: 3,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  notesFilterView: filters.createTextView({
    column: 4,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  /**
   * sort content
   */
  refresh: function () {
    this.sortContent();
  },

  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'serviceVersion';
    associations[2] = 'createTime';
    associations[3] = 'author';
    associations[4] = 'notes';
    return associations;
  }.property()

});
