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

App.TableView = Em.View.extend({

  didInsertElement: function () {
    this.set('filterConditions', []);
    this.filter();
  },

  /**
   * return pagination information displayed on the mirroring page
   */
  paginationInfo: function () {
    return this.t('apps.filters.paginationInfo').format(this.get('startIndex'), this.get('endIndex'), this.get('filteredContent.length'));
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
      this.get('parentView').previousPage();
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
      this.get('parentView').nextPage();
    }
  }),

  rowsPerPageSelectView: Em.Select.extend({
    content: ['10', '25', '50']
  }),

  // start index for displayed content on the mirroring page
  startIndex: 1,

  // calculate end index for displayed content on the mirroring page
  endIndex: function () {
    return Math.min(this.get('filteredContent.length'), this.get('startIndex') + parseInt(this.get('displayLength')) - 1);
  }.property('startIndex', 'displayLength', 'filteredContent.length'),

  /**
   * onclick handler for previous page button on the mirroring page
   */
  previousPage: function () {
    var result = this.get('startIndex') - parseInt(this.get('displayLength'));
    if (result < 2) {
      result = 1;
    }
    this.set('startIndex', result);
  },

  /**
   * onclick handler for next page button on the mirroring page
   */
  nextPage: function () {
    var result = this.get('startIndex') + parseInt(this.get('displayLength'));
    if (result - 1 < this.get('filteredContent.length')) {
      this.set('startIndex', result);
    }
  },

  // the number of mirroring to show on every page of the mirroring page view
  displayLength: null,

  // calculates default value for startIndex property after applying filter or changing displayLength
  updatePaging: function () {
    this.set('startIndex', Math.min(1, this.get('filteredContent.length')));
  }.observes('displayLength', 'filteredContent.length'),

  /**
   * Apply each filter to each row
   *
   * @param iColumn number of column by which filter
   * @param value
   */
  updateFilter: function (iColumn, value, type) {
    var filterCondition = this.get('filterConditions').findProperty('iColumn', iColumn);
    if (filterCondition) {
      filterCondition.value = value;
    } else {
      filterCondition = {
        iColumn: iColumn,
        value: value,
        type: type
      }
      this.get('filterConditions').push(filterCondition);
    }
    this.filter();
  },

  /**
   * contain filter conditions for each column
   */
  filterConditions: [],

  filteredContent: [],

  // contain content to show on the current page of mirroring page view
  pageContent: function () {
    return this.get('filteredContent').slice(this.get('startIndex') - 1, this.get('endIndex'));
  }.property('filteredContent.length', 'startIndex', 'endIndex'),

  /**
   * filter table by filterConditions
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
  }.observes('content')

});
