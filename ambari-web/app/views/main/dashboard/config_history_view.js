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

  /**
   * @type {boolean}
   * @default false
   */
  filteringComplete: false,
  isInitialRendering: true,

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
    this.set('isInitialRendering', true);
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

  /**
   * clear filters on initial loading
   */
  willInsertElement: function () {
    if (!this.get('controller.showFilterConditionsFirstLoad')) {
      this.clearFilterConditionsFromLocalStorage();
    }
    this._super();
  },

  updateFilter: function (iColumn, value, type) {
    if (!this.get('isInitialRendering')) {
      this._super(iColumn, value, type);
    }
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
    }.property('parentView.isInitialRendering'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  modifiedFilterView: filters.createSelectView({
    column: 3,
    appliedEmptyValue: ["", ""],
    fieldType: 'filter-input-width,modified-filter',
    emptyValue: 'Any',
    contentBinding: "controller.modifiedFilter.content",
    onChangeValue: function () {
      this.set("controller.modifiedFilter.optionValue", this.get('selected'));
      this.get('parentView').updateFilter(this.get('column'), [this.get('controller.modifiedFilter.actualValues.startTime'), this.get('controller.modifiedFilter.actualValues.endTime')], 'range');
    }
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
      this.toggleProperty('showLessNotes');
    },
    didInsertElement: function () {
      App.tooltip(this.$("[rel='Tooltip']"), {html: false});
    }
  }),

  /**
   * refresh table content
   */
  refresh: function () {
    var self = this;
    this.set('filteringComplete', false);
    this.get('controller').load().done(function () {
      self.refreshDone.apply(self);
    });
  },

  /**
   * callback executed after refresh call done
   * @method refreshDone
   */
  refreshDone: function () {
    this.set('isInitialRendering', false);
    this.set('filteringComplete', true);
    this.propertyDidChange('pageContent');
    this.set('controller.resetStartIndex', false);
    App.loadTimer.finish('Config History Page');
  },

  /**
   * associations between host property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    return this.get('controller.colPropAssoc');
  }.property('controller.colPropAssoc'),

  filter: Em.K
});
