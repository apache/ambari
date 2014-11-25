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
  date = require('utils/date');

App.MainAlertDefinitionsView = App.TableView.extend({

  templateName: require('templates/main/alerts'),

  content: function() {
    return this.get('controller.content');
  }.property('controller.content.@each'),

  didInsertElement: function () {
    Em.run.later(this, function () {
      App.tooltip($(".enable-disable-button"));
    }, 50) ;
  },

  /**
   * @type {number}
   */
  totalCount: function () {
    return this.get('content.length');
  }.property('content.length'),

  colPropAssoc: ['', 'label', 'summary', 'service.serviceName', 'lastTriggered', 'groups', 'enabled'],

  sortView: sort.wrapperView,

  /**
   * Sorting header for <label>alertDefinition.label</label>
   * @type {Em.View}
   */
  nameSort: sort.fieldView.extend({
    column: 1,
    name: 'label',
    displayName: Em.I18n.t('alerts.table.header.definitionName')
  }),

  /**
   * Sorting header for <label>alertDefinition.status</label>
   * @type {Em.View}
   */
  statusSort: sort.fieldView.extend({
    column: 2,
    name: 'summary',
    displayName: Em.I18n.t('common.status'),
    type: 'alert_status',
    status: 'sorting_desc'
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
    name: 'lastTriggered',
    displayName: Em.I18n.t('alerts.table.header.lastTriggered'),
    type: 'number'
  }),

  /**
   * Sorting header for <label>alertDefinition.enabled</label>
   * @type {Em.View}
   */
  enabledSort: sort.fieldView.extend({
    template:Em.Handlebars.compile('<span {{bindAttr class="view.status :column-name"}}><span class="icon-off"></span></span>'),
    column: 6,
    name: 'enabled'
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
    content: [
      {
        value: '',
        label: Em.I18n.t('common.all')
      },
      {
        value: 'OK',
        label: 'OK'
      },
      {
        value: 'WARNING',
        label: 'WARNING'
      },
      {
        value: 'CRITICAL',
        label: 'CRITICAL'
      },
      {
        value: 'UNKNOWN',
        label: 'UNKNOWN'
      },
      {
        value: 'PENDING',
        label: 'PENDING'
      }
    ],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'alert_status');
    }
  }),

  /**
   * Filtering header for <label>alertDefinition.service.serviceName</label>
   * @type {Em.View}
   */
  serviceFilterView: filters.createSelectView({
    column: 3,
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

  /**
   * Filtering header for <label>alertDefinition.lastTriggered</label>
   * @type {Em.View}
   */
  triggeredFilterView: filters.createSelectView({
    column: 4,
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
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  /**
   * Filtering header for <label>alertDefinition.enabled</label>
   * @type {Em.View}
   */
  enabledFilterView:  filters.createSelectView({
    column: 6,
    fieldType: 'filter-input-width',
    content: [
      {
        value: '',
        label: Em.I18n.t('common.all')
      },
      {
        value: 'enabled',
        label: Em.I18n.t('alerts.table.state.enabled')
      },
      {
        value: 'disabled',
        label: Em.I18n.t('alerts.table.state.disabled')
      }
    ],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'enable_disable');
    }
  }),

  /**
   * Filtering header for <label>alertDefinition</label> groups
   * @type {Em.View}
   */
  alertGroupFilterView: filters.createSelectView({
    column: 5,
    fieldType: 'filter-input-width',
    template: Ember.Handlebars.compile(
      '<div class="btn-group display-inline-block">' +
        '<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">' +
          '<span class="filters-label">Groups:&nbsp;&nbsp;</span>' +
          '<span>{{view.selected.label}}&nbsp;<span class="caret"></span></span>' +
        '</a>' +
          '<ul class="dropdown-menu">' +
            '{{#each category in view.content}}' +
              '<li {{bindAttr class=":category-item category.selected:active"}}>' +
                '<a {{action selectCategory category target="view"}} href="#">' +
                   '<span {{bindAttr class="category.class"}}></span>{{category.label}}</a>' +
              '</li>'+
            '{{/each}}' +
          '</ul>'+
      '</div>'
    ),
    content: [],

    didInsertElement: function() {
      this._super();
      this.updateContent();
      this.set('value', '');
    },

    /**
     * Update list of <code>App.AlertGroup</code> used in the filter
     * @method updateContent
     */
    updateContent: function() {
      var alertGroups = App.AlertGroup.find().map(function (group) {
        return Em.Object.create({
          value: group.get('id'),
          label: group.get('displayNameDefinitions'),
          default: group.get('default')
        });
      });
      var defaultGroups = alertGroups.filterProperty('default');
      defaultGroups.forEach(function(defaultGroup) {
        alertGroups.removeObject(defaultGroup);
      });
      var sortedGroups = defaultGroups.sortProperty('label').concat(alertGroups.sortProperty('label'));

      this.set('content', [
        Em.Object.create({
          value: '',
          label: Em.I18n.t('common.all') + ' (' + this.get('parentView.controller.content.length') + ')'
        })
      ].concat(sortedGroups));
      this.onValueChange();
    }.observes('App.router.clusterController.isLoaded', 'controller.mapperTimestamp'),

    selectCategory: function (event) {
      var category = event.context;
      this.set('value', category.value);
      this.get('parentView').updateFilter(this.get('column'), category.value, 'alert_group');
    },

    onValueChange: function () {
      var value = this.get('value');
      if (value != undefined ) {
        this.get('content').setEach('selected', false);
        this.set('selected', this.get('content').findProperty('value', value));
        this.get('content').findProperty('value', value).set('selected', true);
      }
    }.observes('value')
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
  }
});
