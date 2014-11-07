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
  filteringComplete: false,
  timeOut: null,

  content: function () {
    return this.get('controller.content');
  }.property('controller.content'),

  pageContent: function () {
    var content = this.get('filteredContent');
    if (content.length > ((this.get('endIndex') - this.get('startIndex')) + 1)) {
      content = content.slice(0, (this.get('endIndex') - this.get('startIndex')) + 1);
    }
    return content.sort(function (a, b) {
      return a.get('index') - b.get('index');
    });
  }.property('filteredContent'),

  filteredCount: function () {
    return this.get('controller.filteredCount');
  }.property('controller.filteredCount'),

  totalCount: function () {
    return this.get('controller.totalCount');
  }.property('controller.totalCount'),
  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: function () {
    return this.t('tableView.filters.filteredConfigVersionInfo').format(this.get('filteredCount'), this.get('totalCount'));
  }.property('filteredCount', 'totalCount'),

  /**
   * synchronize properties of view with controller to generate query parameters
   */
  updatePagination: function (key) {
    if (!Em.isNone(this.get('displayLength'))) {
      App.db.setDisplayLength(this.get('controller.name'), this.get('displayLength'));
      this.get('controller.paginationProps').findProperty('name', 'displayLength').value = this.get('displayLength');
    }
    if (!Em.isNone(this.get('startIndex'))) {
      App.db.setStartIndex(this.get('controller.name'), this.get('startIndex'));
      this.get('controller.paginationProps').findProperty('name', 'startIndex').value = this.get('startIndex');
    }

    if (key !== 'SKIP_REFRESH') {
      this.refresh();
    }
  },

  didInsertElement: function () {
    this.addObserver('startIndex', this, 'updatePagination');
    this.addObserver('displayLength', this, 'updatePagination');
    this.refresh();
    this.set('controller.isPolling', true);
    this.get('controller').doPolling();
  },

  /**
   * stop polling after leaving config history page
   */
  willDestroyElement: function () {
    this.set('controller.isPolling', false);
    clearTimeout(this.get('controller.timeoutRef'));
  },

  sortView: sort.serverWrapperView,
  versionSort: sort.fieldView.extend({
    column: 1,
    name: 'serviceVersion',
    displayName: Em.I18n.t('dashboard.configHistory.table.version.title'),
    classNames: ['first']
  }),
  configGroupSort: sort.fieldView.extend({
    column: 2,
    name: 'configGroup',
    displayName: Em.I18n.t('dashboard.configHistory.table.configGroup.title')
  }),
  modifiedSort: sort.fieldView.extend({
    column: 3,
    name: 'createTime',
    status: 'sorting_desc',
    displayName: Em.I18n.t('dashboard.configHistory.table.created.title')
  }),
  authorSort: sort.fieldView.extend({
    column: 4,
    name: 'author',
    displayName: Em.I18n.t('common.author')
  }),
  notesSort: sort.fieldView.extend({
    column: 5,
    name: 'notes',
    displayName: Em.I18n.t('common.notes')
  }),

  serviceFilterView: filters.createSelectView({
    column: 1,
    fieldType: 'filter-input-width',
    content: function () {
      return ['All'].concat(App.Service.find().mapProperty('serviceName'));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('actualValue'), 'select');
    },
    emptyValue: Em.I18n.t('common.all')
  }),

  configGroupFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    content: function () {
      var groupName = App.ServiceConfigVersion.find().mapProperty('groupName').uniq();
      if (groupName.indexOf(null) > -1 ){
        groupName.splice(groupName.indexOf(null), 1);
      }
      return ['All'].concat(groupName);
    }.property('App.router.mainConfigHistoryController.content'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('actualValue'), 'select');
    },
    emptyValue: Em.I18n.t('common.all')
  }),

  modifiedFilterView: filters.createSelectView({
    column: 3,
    appliedEmptyValue: ["", ""],
    fieldType: 'filter-input-width,modified-filter',
    content: ['Any', 'Past 1 hour',  'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    valueBinding: "controller.modifiedFilter.optionValue",
    startTimeBinding: "controller.modifiedFilter.actualValues.startTime",
    endTimeBinding: "controller.modifiedFilter.actualValues.endTime",
    onTimeChange: function () {
      this.get('parentView').updateFilter(this.get('column'), [this.get('controller.modifiedFilter.actualValues.startTime'), this.get('controller.modifiedFilter.actualValues.endTime')], 'range');
    }.observes('controller.modifiedFilter.actualValues.startTime', 'controller.modifiedFilter.actualValues.endTime')
  }),

  authorFilterView: filters.createTextView({
    column: 4,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  notesFilterView: filters.createTextView({
    column: 5,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  updateFilter: function (iColumn, value, type) {
    var self = this;
    this.set('controller.resetStartIndex', false);
    this.saveFilterConditions(iColumn, value, type, false);
    if (!this.get('filteringComplete')) {
      clearTimeout(this.get('timeOut'));
      this.set('timeOut', setTimeout(function () {
        self.updateFilter(iColumn, value, type);
      }, this.get('filterWaitingTime')));
    } else {
      clearTimeout(this.get('timeOut'));
      this.set('controller.resetStartIndex', true);
      this.refresh();
    }
  },

  ConfigVersionView: Em.View.extend({
    tagName: 'tr',
    showLessNotes: true,
    toggleShowLessStatus: function () {
      this.set('showLessNotes', !this.get('showLessNotes'));
    },
    didInsertElement: function () {
      App.tooltip(this.$("[rel='Tooltip']"));
    }
  }),

  /**
   * sort content
   */
  refresh: function () {
    var self = this;
    this.set('filteringComplete', false);
    this.get('controller').load().done(function () {
      self.set('filteringComplete', true);
      self.propertyDidChange('pageContent');
      self.set('controller.resetStartIndex', false);
    });
  },

  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    return this.get('controller.colPropAssoc');
  }.property('controller.colPropAssoc'),

  resetStartIndex: function () {
    if (this.get('controller.resetStartIndex') && this.get('filteredCount') > 0) {
      this.set('startIndex', 1);
      this.updatePagination('SKIP_REFRESH');
    }
  }.observes('controller.resetStartIndex')

});
