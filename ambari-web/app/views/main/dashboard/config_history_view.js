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

App.MainConfigHistoryView = App.TableView.extend(App.TableServerViewMixin, {
  templateName: require('templates/main/dashboard/config_history'),

  controllerBinding: 'App.router.mainConfigHistoryController',
  filteringComplete: false,

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: function () {
    return this.t('tableView.filters.filteredConfigVersionInfo').format(this.get('filteredCount'), this.get('totalCount'));
  }.property('filteredCount', 'totalCount'),

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
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(App.Service.find().map(function (service) {
        return {
          value: service.get('serviceName'),
          label: service.get('displayName')
        }
      }));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  configGroupFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    content: function () {
      var groupName = App.ServiceConfigVersion.find().mapProperty('groupName').uniq();
      if (groupName.indexOf(null) > -1) {
        groupName.splice(groupName.indexOf(null), 1);
      }
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(groupName.map(function (item) {
        return {
          value: item,
          label: item
        }
      }));
    }.property('App.router.mainConfigHistoryController.content'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  modifiedFilterView: filters.createSelectView({
    column: 3,
    appliedEmptyValue: ["", ""],
    fieldType: 'filter-input-width,modified-filter',
    content: [
      {
        value: 'Any',
        label: Em.I18n.t('any')
      },
      {
        value: 'Past 1 hour',
        label: 'Past 1 hour'
      },
      {
        value: 'Past 1 Day',
        label: 'Past 1 Day'
      },
      {
        value: 'Past 2 Days',
        label: 'Past 2 Days'
      },
      {
        value: 'Past 7 Days',
        label: 'Past 7 Days'
      },
      {
        value: 'Past 14 Days',
        label: 'Past 14 Days'
      },
      {
        value: 'Past 30 Days',
        label: 'Past 30 Days'
      }
    ],
    emptyValue: 'Any',
    valueBinding: "controller.modifiedFilter.optionValue",
    selectedBinding: "controller.modifiedFilter.optionValue",
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
   * refresh table content
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
  }.property('controller.colPropAssoc')
});
