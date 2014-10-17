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

var App = require('config/app');
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');

App.TableView = Em.View.extend({

  /**
   * Defines to show pagination or show all records
   * @type {Boolean}
   */
  pagination: true,

  /**
   * Shows if all data is loaded and filtered
   * @type {Boolean}
   */
  filteringComplete: false,

  /**
   * intermediary for filteringComplete
   * @type {Boolean}
   */
  tableFilteringComplete: false,

  /**
   * Loaded from local storage startIndex value
   * @type {Number}
   */
  startIndexOnLoad: null,

  /**
   * Loaded from server persist value
   * @type {Number}
   */
  displayLengthOnLoad: null,

  /**
   * The number of rows to show on every page
   * The value should be a number converted into string type in order to support select element API
   * Example: "10", "25"
   * @type {String}
   */
  displayLength: null,

  /**
   * default value of display length
   * The value should be a number converted into string type in order to support select element API
   * Example: "10", "25"
   */
  defaultDisplayLength: "10",

  /**
   * Persist-key of current table displayLength property
   * @param {String} loginName current user login name
   * @returns {String}
   */
  displayLengthKey: function (loginName) {
    if (App.get('testMode')) {
      return 'pagination_displayLength';
    }
    loginName = loginName ? loginName : App.router.get('loginName');
    return this.get('controller.name') + '-pagination-displayLength-' + loginName;
  },

  /**
   * Set received from server value to <code>displayLengthOnLoad</code>
   * @param {Number} response
   * @param {Object} request
   * @param {Object} data
   * @returns {*}
   */
  getUserPrefSuccessCallback: function (response, request, data) {
    console.log('Got DisplayLength value from server with key ' + data.key + '. Value is: ' + response);
    this.set('displayLengthOnLoad', response);
    return response;
  },

  /**
   * Set default value to <code>displayLengthOnLoad</code> (and send it on server) if value wasn't found on server
   * @returns {Number}
   */
  getUserPrefErrorCallback: function () {
    // this user is first time login
    console.log('Persist did NOT find the key');
    var displayLengthDefault = this.get('defaultDisplayLength');
    this.set('displayLengthOnLoad', displayLengthDefault);
    if (App.get('isAdmin')) {
      this.postUserPref(this.displayLengthKey(), displayLengthDefault);
    }
    return displayLengthDefault;
  },

  /**
   * Do pagination after filtering and sorting
   * Don't call this method! It's already used where it's need
   */
  showProperPage: function() {
    var self = this;
    Em.run.next(function() {
      Em.run.next(function() {
        if(self.get('startIndexOnLoad')) {
          self.set('startIndex', self.get('startIndexOnLoad'));
        }
      });
    });
  },

  /**
   * Return pagination information displayed on the page
   * @type {String}
   */
  paginationInfo: function () {
    return Em.I18n.t('tableView.filters.paginationInfo').format(this.get('startIndex'), this.get('endIndex'), this.get('filteredContent.length'));
  }.property('displayLength', 'filteredContent.length', 'startIndex', 'endIndex'),

  paginationLeft: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-left"></i>'),
    classNameBindings: ['class'],
    class: function () {
      if (this.get("parentView.startIndex") > 1) {
        return "paginate_previous";
      }
      return "paginate_disabled_previous";
    }.property("parentView.startIndex", 'filteredContent.length'),

    click: function () {
      if (this.get('class') === "paginate_previous") {
        this.get('parentView').previousPage();
      }
    }
  }),

  paginationRight: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-right"></i>'),
    classNameBindings: ['class'],
    class: function () {
      if ((this.get("parentView.endIndex")) < this.get("parentView.filteredContent.length")) {
        return "paginate_next";
      }
      return "paginate_disabled_next";
    }.property("parentView.endIndex", 'filteredContent.length'),

    click: function () {
      if (this.get('class') === "paginate_next") {
        this.get('parentView').nextPage();
      }
    }
  }),

  paginationFirst: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-step-backward"></i>'),
    classNameBindings: ['class'],
    class: function () {
      if ((this.get("parentView.endIndex")) > parseInt(this.get("parentView.displayLength"))) {
        return "paginate_previous";
      }
      return "paginate_disabled_previous";
    }.property("parentView.endIndex", 'filteredContent.length'),

    click: function () {
      if (this.get('class') === "paginate_previous") {
        this.get('parentView').firstPage();
      }
    }
  }),

  paginationLast: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-step-forward"></i>'),
    classNameBindings: ['class'],
    class: function () {
      if (this.get("parentView.endIndex") !== this.get("parentView.filteredContent.length")) {
        return "paginate_next";
      }
      return "paginate_disabled_next";
    }.property("parentView.endIndex", 'filteredContent.length'),

    click: function () {
      if (this.get('class') === "paginate_next") {
        this.get('parentView').lastPage();
      }
    }
  }),

  /**
   * Select View with list of "rows-per-page" options
   * @type {Ember.View}
   */
  rowsPerPageSelectView: Em.Select.extend({
    content: ['10', '25', '50', '100'],
    change: function () {
      this.get('parentView').saveDisplayLength();
    }
  }),

  saveDisplayLength: function() {
    var self = this;
    Em.run.next(function() {
      if (!App.testMode) {
        if (App.get('isAdmin')) {
          self.postUserPref(self.displayLengthKey(), self.get('displayLength'));
        }
      }
    });
  },
  /**
   * Start index for displayed content on the page
   */
  startIndex: 1,

  /**
   * Calculate end index for displayed content on the page
   */
  endIndex: function () {
    if (this.get('pagination')) {
      return Math.min(this.get('filteredContent.length'), this.get('startIndex') + parseInt(this.get('displayLength')) - 1);
    } else {
      return this.get('filteredContent.length')
    }
  }.property('startIndex', 'displayLength', 'filteredContent.length'),

  /**
   * Onclick handler for previous page button on the page
   */
  previousPage: function () {
    var result = this.get('startIndex') - parseInt(this.get('displayLength'));
    this.set('startIndex', (result < 2) ? 1 : result);
  },

  /**
   * Onclick handler for next page button on the page
   */
  nextPage: function () {
    var result = this.get('startIndex') + parseInt(this.get('displayLength'));
    if (result - 1 < this.get('filteredContent.length')) {
      this.set('startIndex', result);
    }
  },
  /**
   * Onclick handler for first page button on the page
   */
  firstPage: function () {
    this.set('startIndex', 1);
  },
  /**
   * Onclick handler for last page button on the page
   */
  lastPage: function () {
    var pagesCount = this.get('filteredContent.length') / parseInt(this.get('displayLength'));
    var startIndex = (this.get('filteredContent.length') % parseInt(this.get('displayLength')) === 0) ?
      (pagesCount - 1) * parseInt(this.get('displayLength')) :
      Math.floor(pagesCount) * parseInt(this.get('displayLength'));
    this.set('startIndex', ++startIndex);
  },

  /**
   * Calculates default value for startIndex property after applying filter or changing displayLength
   */
  updatePaging: function (controller, property) {
    var displayLength = this.get('displayLength');
    var filteredContentLength = this.get('filteredContent.length');
    if (property == 'displayLength') {
      this.set('startIndex', Math.min(1, filteredContentLength));
    } else if (!filteredContentLength) {
      this.set('startIndex', 0);
    } else if (this.get('startIndex') > filteredContentLength) {
      this.set('startIndex', Math.floor((filteredContentLength - 1) / displayLength) * displayLength + 1);
    } else if (!this.get('startIndex')) {
      this.set('startIndex', 1);
    }
  }.observes('displayLength', 'filteredContent.length'),

  /**
   * Apply each filter to each row
   *
   * @param {Number} iColumn number of column by which filter
   * @param {Object} value
   * @param {String} type
   * @param {String} defaultValue
   */
  updateFilter: function (iColumn, value, type, defaultValue) {
    var filterCondition = this.get('filterConditions').findProperty('iColumn', iColumn);
    defaultValue = defaultValue ? defaultValue : "";
    if (filterCondition) {
      filterCondition.value = value;
    }
    else {
      filterCondition = {
        iColumn: iColumn,
        value: value,
        type: type,
        defaultValue: defaultValue
      };
      this.get('filterConditions').push(filterCondition);
    }
    this.filtersUsedCalc();
    this.filter();
  },


  /**
   * Contain filter conditions for each column
   * @type {Array}
   */
  filterConditions: [],

  /**
   * Contains content after implementing filters
   * @type {Array}
   */
  filteredContent: [],

  /**
   * Determine if <code>filteredContent</code> is empty or not
   * @type {Boolean}
   */
  hasFilteredItems: function() {
    return !!this.get('filteredContent.length');
  }.property('filteredContent.length'),

  /**
   * Contains content to show on the current page of data page view
   * @type {Array}
   */
  pageContent: function () {
    return this.get('filteredContent').slice(this.get('startIndex') - 1, this.get('endIndex'));
  }.property('filteredContent.length', 'startIndex', 'endIndex'),

  /**
   * Filter table by filterConditions
   */
  filter: function () {
    var content = this.get('content');
    var filterConditions = this.get('filterConditions').filterProperty('value');
    var result;
    var assoc = this.get('colPropAssoc');
    if (filterConditions.length) {
      result = content.filter(function (item) {
        var match = true;
        filterConditions.forEach(function (condition) {
          var filterFunc = filters.getFilterByType(condition.type, false);
          if (match && condition.value != condition.defaultValue) {
            match = filterFunc(Em.get(item, assoc[condition.iColumn]), condition.value);
          }
        });
        return match;
      });
      this.set('filteredContent', result);
    } else {
      this.set('filteredContent', content.toArray());
    }
  }.observes('content.length'),

  /**
   * Does any filter is used on the page
   * @type {Boolean}
   */
  filtersUsed: false,

  /**
   * Determine if some filters are used on the page
   * Set <code>filtersUsed</code> value
   */
  filtersUsedCalc: function() {
    var filterConditions = this.get('filterConditions');
    if (!filterConditions.length) {
      this.set('filtersUsed', false);
      return;
    }
    var filtersUsed = false;
    filterConditions.forEach(function(filterCondition) {
      if (filterCondition.value.toString() !== '' && filterCondition.value.toString() !== filterCondition.defaultValue.toString()) {
        filtersUsed = true;
      }
    });
    this.set('filtersUsed', filtersUsed);
  },

  /**
   * Run <code>clearFilter</code> in the each child filterView
   */
  clearAllFilters: function() {
    this.set('filterConditions', []);
    this.get('_childViews').forEach(function(childView) {
      if (childView['clearFilter']) {
        childView.clearFilter();
      }
    });
  },

  actions: {
    clearFilters: function() {
      return this.clearAllFilters();
    }
  }

});
