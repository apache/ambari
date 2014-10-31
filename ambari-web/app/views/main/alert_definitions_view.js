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
var filters = require('views/common/filter_view'),
  sort = require('views/common/sort_view'),
  date = require('utils/date'),
  dataUtils = require('utils/data_manipulation');

App.MainAlertDefinitionsView = App.TableView.extend({

  templateName: require('templates/main/alerts'),

  content: function() {
    return this.get('controller.content');
  }.property('controller.content.@each'),

  /**
   * @type {number}
   */
  totalCount: function () {
    return this.get('content.length');
  }.property('content.length'),

  colPropAssoc: ['', 'label', 'state', 'service.serviceName', 'lastTriggered'],

  /**
   * List of css-classes for alert types
   * @type {object}
   */
  typeIcons: {
    'OK': 'icon-ok-sign',
    'WARNING': 'icon-warning-sign',
    'CRITICAL': 'icon-remove',
    'DISABLED': 'icon-off',
    'UNKNOWN': 'icon-question-sign'
  },

  sortView: sort.wrapperView,

  /**
   * Sorting header for <label>alertDefinition.label</label>
   * @type {Em.View}
   */
  nameSort: sort.fieldView.extend({
    column: 1,
    name: 'label',
    displayName: Em.I18n.t('common.name')
  }),

  /**
   * Sorting header for <label>alertDefinition.status</label>
   * @type {Em.View}
   */
  statusSort: sort.fieldView.extend({
    column: 2,
    name: 'status',
    displayName: Em.I18n.t('common.status'),
    type: 'string'
  }),

  /**
   * Sorting header for <label>alertDefinition.service.serviceName</label>
   * @type {Em.View}
   */
  serviceSort: sort.fieldView.extend({
    column: 3,
    name: 'service.serviceName',
    displayName: Em.I18n.t('common.service'),
    type: 'string'
  }),

  /**
   * Sorting header for <label>alertDefinition.lastTriggeredSort</label>
   * @type {Em.View}
   */
  lastTriggeredSort: sort.fieldView.extend({
    column: 4,
    name: 'memory',
    displayName: Em.I18n.t('alerts.table.header.lastTriggered'),
    type: 'date'
  }),

  /**
   * Filtering header for <label>alertDefinition.label</label>
   * @type {Em.View}
   */
  nameFilterView: filters.createTextView({
    column: 1,
    fieldType: 'filter-input-width',
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  /**
   * Filtering header for <label>alertDefinition.status</label>
   * @type {Em.View}
   */
  stateFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    content: ['All', 'OK', 'WARNING', 'CRITICAL', 'DISABLED', 'UNKNOWN'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('actualValue'), 'select');
    },
    emptyValue: Em.I18n.t('common.all')
  }),

  /**
   * Filtering header for <label>alertDefinition.service.serviceName</label>
   * @type {Em.View}
   */
  serviceFilterView: filters.createSelectView({
    column: 3,
    fieldType: 'filter-input-width',
    content: function () {
      return ['All'].concat(App.Service.find().mapProperty('serviceName'));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('actualValue'), 'select');
    },
    emptyValue: Em.I18n.t('common.all')
  }),

  /**
   * Filtering header for <label>alertDefinition.lastTriggered</label>
   * @type {Em.View}
   */
  triggeredFilterView: filters.createSelectView({
    column: 4,
    triggeredOnSameValue: [
      {
        values: ['Custom', 'Custom2'],
        displayAs: 'Custom'
      }
    ],
    appliedEmptyValue: ["", ""],
    fieldType: 'filter-input-width,modified-filter',
    content: ['Any', 'Past 1 hour',  'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days', 'Custom', 'Custom2'],
    valueBinding: "controller.modifiedFilter.optionValue",
    startTimeBinding: "controller.modifiedFilter.actualValues.startTime",
    endTimeBinding: "controller.modifiedFilter.actualValues.endTime",
    onTimeChange: function () {
      this.get('parentView').updateFilter(this.get('column'), [this.get('controller.modifiedFilter.actualValues.startTime'), this.get('controller.modifiedFilter.actualValues.endTime')], 'range');
    }.observes('controller.modifiedFilter.actualValues.startTime', 'controller.modifiedFilter.actualValues.endTime')
  }),

  /**
   * Filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: function () {
    return this.t('alerts.filters.filteredAlertsInfo').format(this.get('filteredCount'), this.get('totalCount'));
  }.property('filteredCount', 'totalCount'),

  /**
   * Determines how display "back"-link - as link or text
   * @type {string}
   */
  paginationLeftClass: function () {
    if (this.get("startIndex") > 1) {
      return "paginate_previous";
    }
    return "paginate_disabled_previous";
  }.property("startIndex", 'filteredCount'),

  /**
   * Determines how display "next"-link - as link or text
   * @type {string}
   */
  paginationRightClass: function () {
    if ((this.get("endIndex")) < this.get("filteredCount")) {
      return "paginate_next";
    }
    return "paginate_disabled_next";
  }.property("endIndex", 'filteredCount'),

  /**
   * Show previous-page if user not in the first page
   * @method previousPage
   */
  previousPage: function () {
    if (this.get('paginationLeftClass') === 'paginate_previous') {
      this._super();
    }
  },

  /**
   * Show next-page if user not in the last page
   * @method nextPage
   */
  nextPage: function () {
    if (this.get('paginationRightClass') === 'paginate_next') {
      this._super();
    }
  },

  /**
   * View for each table row with <code>alertDefinition</code>
   * @type {Em.View}
   */
  AlertDefinitionView: Em.View.extend({

    tagName: 'tr',

    /**
     * Status generates from child-alerts
     * Format: 1 OK / 2 WARN / 1 CRIT / 1 DISABLED / 1 UNKNOWN
     * If some there are no alerts with some state, this state isn't shown
     * Order is equal to example
     * @type {string}
     */
    status: function () {
      var typeIcons = this.get('parentView.typeIcons'),
        ordered = ['OK', 'WARNING', 'CRITICAL', 'DISABLED', 'UNKNOWN'],
        grouped = dataUtils.groupPropertyValues(this.get('content.alerts'), 'state');
      return ordered.map(function (state) {
        if (grouped[state]) {
          return grouped[state].length + ' <span class="' + typeIcons[state] + ' alert-state-' + state + '"></span>';
        }
        return null;
      }).compact().join(' / ');
    }.property('content.alerts.@each.status')

  })


});
