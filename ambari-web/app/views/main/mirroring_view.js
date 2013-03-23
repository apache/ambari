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

App.MainMirroringView = Em.View.extend({
  templateName: require('templates/main/mirroring'),
  content: function () {
    return this.get('controller.content');
  }.property('controller.content'),

  didInsertElement: function () {
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

  datasetsPerPageSelectView: Em.Select.extend({
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

  sortView: sort.wrapperView,
  nameSort: sort.fieldView.extend({
    name: 'name',
    displayName: Em.I18n.t('common.name')
  }),
  dataSetSourceSort: sort.fieldView.extend({
    name: 'sourceDir',
    displayName: Em.I18n.t('mirroring.table.datasetSource')
  }),
  lastSuccessSort: sort.fieldView.extend({
    name: 'lastSucceededDate',
    displayName: Em.I18n.t('mirroring.table.lastSuccess'),
    type: 'number'
  }),
  lastFailSort: sort.fieldView.extend({
    name: 'lastFailedDate',
    displayName: Em.I18n.t('mirroring.table.lastFail'),
    type: 'number'
  }),
  lastDurationSort: sort.fieldView.extend({
    name: 'lastDuration',
    displayName: Em.I18n.t('mirroring.table.lastDuration'),
    type: 'number'
  }),
  avgDataSort: sort.fieldView.extend({
    name: 'avgData',
    displayName: Em.I18n.t('mirroring.table.avgData'),
    type: 'number'
  }),

  /**
   * Filter view for name column
   * Based on <code>filters</code> library
   */
  nameFilterView: filters.createTextView({
    fieldType: 'input-small',
    onChangeValue: function () {
      this.get('parentView').updateFilter(1, this.get('value'), 'string');
    }
  }),

  datasetSourceFilterView: filters.createTextView({
    fieldType: 'input-small',
    onChangeValue: function () {
      this.get('parentView').updateFilter(2, this.get('value'), 'string');
    }
  }),

  lastSuccessFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(3, this.get('value'), 'date');
    }
  }),

  lastFailFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(4, this.get('value'), 'date');
    }
  }),

  lastDurationFilterView: filters.createTextView({
    fieldType: 'input-small',
    onChangeValue: function () {
      this.get('parentView').updateFilter(5, this.get('value'), 'duration');
    }
  }),

  avgDataFilterView: filters.createTextView({
    fieldType: 'input-small',
    onChangeValue: function () {
      this.get('parentView').updateFilter(6, this.get('value'), 'ambari-bandwidth');
    }
  }),

  DatasetView: Em.View.extend({
    content: null,
    tagName: 'tr',

    lastDurationFormatted: function () {
      var milliseconds = this.get('content.lastDuration');
      var h = Math.floor(milliseconds / 3600000);
      var m = Math.floor((milliseconds % 3600000) / 60000);
      var s = Math.floor(((milliseconds % 360000) % 60000) / 1000);
      return (h == 0 ? '' : h + 'hr ') + (m == 0 ? '' : m + 'mins ') + (s == 0 ? '' : s + 'secs ');
    }.property('content.lastDuration'),

    lastSucceededDateFormatted: function () {
      if (this.get('content.lastSucceededDate')) {
        return $.timeago(this.get('content.lastSucceededDate'));
      }
    }.property('content.lastSucceededDate'),

    lastFailedDateFormatted: function () {
      if (this.get('content.lastFailedDate')) {
        return $.timeago(this.get('content.lastFailedDate'));
      }
    }.property('content.lastFailedDate')
  }),

  /**
   * Apply each filter to dataset
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
   * associations between dataset property and column index
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'name';
    associations[2] = 'sourceDir';
    associations[3] = 'lastSucceededDate';
    associations[4] = 'lastFailedDate';
    associations[5] = 'lastDuration';
    associations[6] = 'avgData';
    return associations;
  }.property(),

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
      result = content.filter(function (dataset) {
        var match = true;
        filterConditions.forEach(function (condition) {
          var filterFunc = filters.getFilterByType(condition.type, false);
          if (match) {
            match = filterFunc(dataset.get(assoc[condition.iColumn]), condition.value);
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
