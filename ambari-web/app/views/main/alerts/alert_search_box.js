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

App.MainAlertDefinitionSearchBoxView = Em.View.extend({
  templateName: require('templates/main/host/combo_search_box'),
  errMsg: '',
  classNames: ['col-sm-12'],

  didInsertElement: function () {
    this.initVS();
    this.restoreComboFilterQuery();
    this.showHideClearButton();
    this.initOpenVSButton();
  },

  initOpenVSButton: function() {
    $('.VS-open-box button').click(function() {
      $('.VS-open-box .popup-arrow-up, .search-box-row').toggleClass('hide');
    });
  },

  initVS: function() {
    window.visualSearch = VS.init({
      container: $('#combo_search_box'),
      query: '',
      showFacets: true,
      delay: 500,
      placeholder: Em.I18n.t('common.search'),
      unquotable: [
        'text'
      ],
      callbacks: {
        search: this.search.bind(this),
        facetMatches: this.facetMatches.bind(this),
        valueMatches: this.valueMatches.bind(this)
      }
    });
  },

  /**
   * describe filter columns
   * @type {object}
   * @const
   */
  keyFilterMap: {
    'Status': {
      key: 'summary',
      type: 'alert_status',
      column: 2
    },
    'Alert Definition Name': {
      key: 'label',
      type: 'string',
      column: 1
    },
    'Service': {
      key: 'serviceName',
      type: 'select',
      column: 3
    },
    'Last Status Changed': {
      key: 'lastTriggered',
      type: 'date',
      column: 5
    },
    'State': {
      key: 'enabled',
      type: 'enable_disable',
      column: 6
    },
    'Group': {
      key: 'groups',
      type: 'alert_group',
      column: 7
    }
  },

  enabledDisabledMap: {
    'enabled': Em.I18n.t('alerts.table.state.enabled'),
    'disabled': Em.I18n.t('alerts.table.state.disabled')
  },

  lastTriggeredOptions: [
    'Past 1 hour',
    'Past 1 Day',
    'Past 2 Days',
    'Past 7 Days',
    'Past 14 Days',
    'Past 30 Days'
  ],

  /**
   * populated dynamically in <code>getGroupsAvailableValues<code>
   * @type {object}
   */
  groupsNameIdMap: {},

  /**
   * 'search' callback for visualsearch.js
   * @param query
   * @param searchCollection
   */
  search: function (query, searchCollection) {
    this.clearErrMsg();
    this.showHideClearButton();
    var invalidFacet = this.findInvalidFacet(searchCollection);
    if (invalidFacet) {
      this.showErrMsg(invalidFacet);
    }
    var tableView = this.get('parentView.parentView');
    App.db.setComboSearchQuery(tableView.get('controller.name'), query);
    var filterConditions = this.createFilterConditions(searchCollection);
    tableView.updateComboFilter(filterConditions);
  },

  /**
   * 'facetMatches' callback for visualsearch.js
   * @param callback
   */
  facetMatches: function (callback) {
    callback(Object.keys(this.get('keyFilterMap')), {preserveOrder: true});
  },

  /**
   * 'valueMatches' callback for visualsearch.js
   * @param facetValue
   * @param searchTerm
   * @param callback
   */
  valueMatches: function (facetValue, searchTerm, callback) {
    this.showHideClearButton();
    switch (this.get('keyFilterMap')[facetValue].key) {
      case 'summary':
        this.getSummaryAvailableValues(facetValue, callback);
        break;
      case 'label':
        this.getLabelAvailableValues(facetValue, callback);
        break;
      case 'serviceName':
        this.getServiceAvailableValues(facetValue, callback);
        break;
      case 'lastTriggered':
        this.getTriggeredAvailableValues(facetValue, callback);
        break;
      case 'enabled':
        this.getEnabledAvailableValues(facetValue, callback);
        break;
      case 'groups':
        this.getGroupsAvailableValues(facetValue, callback);
        break;
    }
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getSummaryAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(Object.keys(App.AlertDefinition.shortState), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getLabelAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(App.AlertDefinition.find().mapProperty('label').uniq(), facetValue));
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getServiceAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(App.AlertDefinition.find().mapProperty('serviceDisplayName').uniq(), facetValue));
  },

  /**
   *
   * @param {string} facetValue
   * @param {function} callback
   */
  getTriggeredAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(this.get('lastTriggeredOptions'), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getEnabledAvailableValues: function(facetValue, callback) {
    callback(this.rejectUsedValues(Object.values(this.get('enabledDisabledMap')), facetValue), {preserveOrder: true});
  },

  /**
   *
   * @param {string} facetValue
   * @param {Function} callback
   */
  getGroupsAvailableValues: function(facetValue, callback) {
    const alertGroups = App.AlertGroup.find();
    const map = {};
    alertGroups.forEach((group) => {
      map[group.get('displayName')] = group.get('id');
    });
    this.set('groupsNameIdMap', map);
    callback(this.rejectUsedValues(alertGroups.mapProperty('displayName'), facetValue));
  },

  /**
   *
   * @param {Array} values
   * @param {string} facetValue
   */
  rejectUsedValues: function(values, facetValue) {
    return values.reject(function (item) {
      return visualSearch.searchQuery.values(facetValue).indexOf(item) >= 0;
    })
  },

  /**
   *
   * @param {object} searchCollection
   * @returns {!object}
   */
  findInvalidFacet: function(searchCollection) {
    const map = this.get('keyFilterMap');
    return searchCollection.models.find((facet) => {
      return !map[facet.attributes.category];
    });
  },

  showErrMsg: function(category) {
    this.set('errMsg', category.attributes.value + " " + Em.I18n.t('hosts.combo.search.invalidCategory'));
  },

  clearErrMsg: function() {
    this.set('errMsg', '')
  },

  showHideClearButton: function () {
    if (visualSearch.searchQuery.length > 0) {
      $('.VS-cancel-search-box').removeClass('hide');
    } else {
      $('.VS-cancel-search-box').addClass('hide');
    }
  },

  restoreComboFilterQuery: function() {
    const query = App.db.getComboSearchQuery(this.get('parentView.parentView.controller.name'));
    if (query) {
      visualSearch.searchBox.setQuery(query);
    }
  },

  /**
   *
   * @param {object} searchCollection
   * @returns {Array}
   */
  createFilterConditions: function (searchCollection) {
    const filterConditions = [];
    const map = this.get('keyFilterMap');

    searchCollection.models.forEach((model) => {
      const filter = model.attributes;
      if (map[filter.category]) {
        filterConditions.push({
          skipFilter: false,
          iColumn: map[filter.category].column,
          value: this.mapLabelToValue(filter.category, filter.value),
          type: map[filter.category].type
        });
      }
    });
    return filterConditions;
  },

  /**
   *
   * @param {string} category
   * @param {string} label
   */
  mapLabelToValue: function(category, label) {
    const enabledDisabledMap = this.get('enabledDisabledMap');
    const groupsNameIdMap = this.get('groupsNameIdMap');

    switch (category) {
      case 'State':
        return Object.keys(enabledDisabledMap)[Object.values(enabledDisabledMap).indexOf(label)];
      case 'Group':
        return groupsNameIdMap[label];
      default:
        return label;
    }
  }
});
