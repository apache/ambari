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
   * The number of rows to show on every page
   * The value should be a number converted into string type in order to support select element API
   * Example: "10", "25"
   * @type {String}
   */
  displayLength: '10',

  /**
   * default value of display length
   * The value should be a number converted into string type in order to support select element API
   * Example: "10", "25"
   */
  defaultDisplayLength: "10",

  /**
   * number of hosts in table after applying filters
   */
  filteredCount: function () {
    return this.get('filteredContent.length');
  }.property('filteredContent.length'),

  /**
   * Do filtering, using saved in the local storage filter conditions
   */
  willInsertElement:function () {
    this.initFilters();
  },

  /**
   * initialize filters
   * restore values from local DB
   * or clear filters in case there is no filters to restore
   */
  initFilters: function () {
    this.clearFilters();
    this.set('tableFilteringComplete', true);
  },

  /**
   * Return pagination information displayed on the page
   * @type {String}
   */
  paginationInfo: function () {
    return this.t('tableView.filters.paginationInfo').format(this.get('startIndex'), this.get('endIndex'), this.get('filteredCount'));
  }.property('filteredCount', 'endIndex'),

  paginationLeft: Ember.View.extend({
    tagName: 'a',
    templateName: 'table/navigation/pagination_left',
    classNameBindings: ['class'],
    class: function () {
      if (this.get("parentView.startIndex") > 1) {
        return "paginate_previous";
      }
      return "paginate_disabled_previous";
    }.property("parentView.startIndex", 'parentView.filteredCount'),

    click: function () {
      if (this.get('class') === "paginate_previous") {
        this.get('parentView').previousPage();
      }
    }
  }),

  paginationRight: Ember.View.extend({
    tagName: 'a',
    templateName: 'table/navigation/pagination_right',
    classNameBindings: ['class'],
    class: function () {
      if ((this.get("parentView.endIndex")) < this.get("parentView.filteredCount")) {
        return "paginate_next";
      }
      return "paginate_disabled_next";
    }.property("parentView.endIndex", 'parentView.filteredCount'),

    click: function () {
      if (this.get('class') === "paginate_next") {
        this.get('parentView').nextPage();
      }
    }
  }),

  paginationFirst: Ember.View.extend({
    tagName: 'a',
    templateName: 'table/navigation/pagination_first',
    classNameBindings: ['class'],
    class: function () {
      if ((this.get("parentView.endIndex")) > parseInt(this.get("parentView.displayLength"))) {
        return "paginate_previous";
      }
      return "paginate_disabled_previous";
    }.property("parentView.endIndex", 'parentView.filteredCount'),

    click: function () {
      if (this.get('class') === "paginate_previous") {
        this.get('parentView').firstPage();
      }
    }
  }),

  paginationLast: Ember.View.extend({
    tagName: 'a',
    templateName: 'table/navigation/pagination_last',
    classNameBindings: ['class'],
    class: function () {
      if (this.get("parentView.endIndex") !== this.get("parentView.filteredCount")) {
        return "paginate_next";
      }
      return "paginate_disabled_next";
    }.property("parentView.endIndex", 'parentView.filteredCount'),

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

  /**
   * Start index for displayed content on the page
   */
  startIndex: 1,

  /**
   * Calculate end index for displayed content on the page
   */
  endIndex: function () {
    if (this.get('pagination') && this.get('displayLength')) {
      return Math.min(this.get('filteredCount'), this.get('startIndex') + parseInt(this.get('displayLength')) - 1);
    } else {
      return this.get('filteredCount') || 0;
    }
  }.property('startIndex', 'displayLength', 'filteredCount'),

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
    if (result - 1 < this.get('filteredCount')) {
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
    var pagesCount = this.get('filteredCount') / parseInt(this.get('displayLength'));
    var startIndex = (this.get('filteredCount') % parseInt(this.get('displayLength')) === 0) ?
      (pagesCount - 1) * parseInt(this.get('displayLength')) :
      Math.floor(pagesCount) * parseInt(this.get('displayLength'));
    this.set('startIndex', ++startIndex);
  },

  /**
   * Calculates default value for startIndex property after applying filter or changing displayLength
   */
  updatePaging: function (controller, property) {
    var displayLength = this.get('displayLength');
    var filteredContentLength = this.get('filteredCount');
    if (property == 'displayLength') {
      this.set('startIndex', Math.min(1, filteredContentLength));
    }
    else
      if (!filteredContentLength) {
        this.set('startIndex', 0);
      }
      else
        if (this.get('startIndex') > filteredContentLength) {
          this.set('startIndex', Math.floor((filteredContentLength - 1) / displayLength) * displayLength + 1);
        }
        else
          if (!this.get('startIndex')) {
            this.set('startIndex', 1);
          }
  }.observes('displayLength', 'filteredCount'),

  /**
   * Apply each filter to each row
   *
   * @param {Number} iColumn number of column by which filter
   * @param {Object} value
   * @param {String} type
   */
  updateFilter: function (iColumn, value, type) {
    var filterCondition = this.get('filterConditions').findProperty('iColumn', iColumn);
    if (filterCondition) {
      filterCondition.value = value;
    }
    else {
      filterCondition = {
        iColumn: iColumn,
        value: value,
        type: type
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
    return !!this.get('filteredCount');
  }.property('filteredCount'),

  /**
   * Contains content to show on the current page of data page view
   * @type {Array}
   */
  pageContent: function () {
    return this.get('filteredContent').slice(this.get('startIndex') - 1, this.get('endIndex'));
  }.property('filteredCount', 'startIndex', 'endIndex'),

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
          var filterFunc = App.Filters.getFilterByType(condition.type, false);
          if (match) {
            match = filterFunc(item.get(assoc[condition.iColumn]), condition.value);
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
      if (filterCondition.value.toString() !== '') {
        filtersUsed = true;
      }
    });
    this.set('filtersUsed', filtersUsed);
  },

  /**
   * Run <code>clearFilter</code> in the each child filterView
   */
  clearFilters: function() {
    this.set('filterConditions', []);
    this.get('_childViews').forEach(function(childView) {
      if (childView['clearFilter']) {
        childView.clearFilter();
      }
    });
  },

  actions: {
    actionClearFilters: function() {
      this.clearFilters();
    }
  }

});
