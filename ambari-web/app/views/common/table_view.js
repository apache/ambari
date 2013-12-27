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

  /**
   * Shows if all data is loaded and filtered
   */
  filteringComplete: false,

  /**
   * Loaded from local storage startIndex value
   */
  startIndexOnLoad: null,
  /**
   * Loaded from server persist value
   */
  displayLengthOnLoad: null,

  /**
   * Do filtering, using saved in the local storage filter conditions
   */
  willInsertElement:function () {
    var self = this;
    var name = this.get('controller.name');

    this.set('startIndexOnLoad', App.db.getStartIndex(name));
    if (App.db.getDisplayLength(name)) {
      this.set('displayLength', App.db.getDisplayLength(name));
    } else {
      self.dataLoading().done(function (initValue) {
        self.set('displayLength', initValue);
      });
    }

    var filterConditions = App.db.getFilterConditions(name);
    if (filterConditions) {
      this.set('filterConditions', filterConditions);

      var childViews = this.get('childViews');

      filterConditions.forEach(function(condition) {
        var view = childViews.findProperty('column', condition.iColumn);
        if (view) {
          view.set('value', condition.value);
          Em.run.next(function() {
            view.showClearFilter();
          });
        }
      });
    } else {
      this.clearFilters();
    }

    Em.run.next(function() {
      Em.run.next(function() {
        self.set('filteringComplete', true);
      });
    });
  },

  /**
   * Load user preference value on hosts page from server
   */
  dataLoading: function () {
    var dfd = $.Deferred();
    var self = this;
    this.getUserPref(this.displayLengthKey()).done(function () {
      var curLength = self.get('displayLengthOnLoad');
      self.set('displayLengthOnLoad', null);
      dfd.resolve(curLength);
    });
    return dfd.promise();
  },
  displayLengthKey: function (loginName) {
    if (!loginName)
      loginName = App.router.get('loginName');
    return 'hosts-pagination-displayLength-' + loginName;
  },

  /**
   * get display length persist value from server with displayLengthKey
   */
  getUserPref: function(key){
    return App.ajax.send({
      name: 'settings.get.user_pref',
      sender: this,
      data: {
        key: key
      },
      success: 'getDisplayLengthSuccessCallback',
      error: 'getDisplayLengthErrorCallback'
    });
  },
  getDisplayLengthSuccessCallback: function (response, request, data) {
    if (response != null) {
      console.log('Got DisplayLength value from server with key ' + data.key + '. Value is: ' + response);
      this.set('displayLengthOnLoad', response);
      return response;
    }
  },
  getDisplayLengthErrorCallback: function (request, ajaxOptions, error) {
    // this user is first time login
    if (request.status == 404) {
      console.log('Persist did NOT find the key');
      var displayLengthDefault = 10;
      this.set('displayLengthOnLoad', displayLengthDefault);
      this.postUserPref(this.displayLengthKey(), displayLengthDefault);
      return displayLengthDefault;
    }
  },
  /**
   * post display length persist key/value to server, value is object
   */
  postUserPref: function (key, value) {
    var keyValuePair = {};
    keyValuePair[key] = JSON.stringify(value);
    App.ajax.send({
      'name': 'settings.post.user_pref',
      'sender': this,
      'beforeSend': 'postUserPrefBeforeSend',
      'data': {
        'keyValuePair': keyValuePair
      }
    });
  },
  postUserPrefBeforeSend: function(request, ajaxOptions, data){
    console.log('BeforeSend to persist: persistKeyValues', data.keyValuePair);
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
   * return filtered number of all content number information displayed on the page footer bar
   */
  filteredHostsInfo: function () {
    return this.t('apps.filters.filteredHostsInfo').format(this.get('filteredContent.length'), this.get('content').get('length'));
  }.property('content.length', 'filteredContent.length'),

  /**
   * return pagination information displayed on the page
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
    content: ['10', '25', '50'],
    change: function () {
      this.get('parentView').saveDisplayLength();
    }
  }),

  // start index for displayed content on the page
  startIndex: 1,

  // calculate end index for displayed content on the page
  endIndex: function () {
    return Math.min(this.get('filteredContent.length'), this.get('startIndex') + parseInt(this.get('displayLength')) - 1);
  }.property('startIndex', 'displayLength', 'filteredContent.length'),

  /**
   * onclick handler for previous page button on the page
   */
  previousPage: function () {
    var result = this.get('startIndex') - parseInt(this.get('displayLength'));
    if (result < 2) {
      result = 1;
    }
    this.set('startIndex', result);
  },

  /**
   * onclick handler for next page button on the page
   */
  nextPage: function () {
    var result = this.get('startIndex') + parseInt(this.get('displayLength'));
    if (result - 1 < this.get('filteredContent.length')) {
      this.set('startIndex', result);
    }
  },

  // the number of rows to show on every page
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
   * @param type
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
    this.saveFilterConditions();
    this.filtersUsedCalc();
    this.filter();
  },

  saveFilterConditions: function() {
    App.db.setFilterConditions(this.get('controller.name'), this.get('filterConditions'));
  },

  saveDisplayLength: function() {
    var self = this;
    Em.run.next(function() {
      App.db.setDisplayLength(self.get('controller.name'), self.get('displayLength'));
      if (!App.testMode) {
        self.postUserPref(self.displayLengthKey(), self.get('displayLength'));
      }
    });
  },

  saveStartIndex: function() {
    if (this.get('filteringComplete')) {
      App.db.setStartIndex(this.get('controller.name'), this.get('startIndex'));
    }
  }.observes('startIndex'),

  clearFilterCondition: function() {
    App.db.setFilterConditions(this.get('controller.name'), null);
  },

  clearStartIndex: function() {
    App.db.setStartIndex(this.get('controller.name'), null);
  },

  /**
   * contain filter conditions for each column
   */
  filterConditions: [],

  filteredContent: [],

  // contain content to show on the current page of data page view
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
  }.observes('content'),

  filtersUsed: false,

  filtersUsedCalc: function() {
    var filterConditions = this.get('filterConditions');
    if (!filterConditions.length) {
      this.set('filtersUsed', false);
    }
    var filtersUsed = false;
    filterConditions.forEach(function(filterCondition) {
      if (filterCondition.value.toString() !== '') {
        filtersUsed = true;
      }
    });
    this.set('filtersUsed', filtersUsed);
  },

  clearFilters: function() {
    this.set('filterConditions', []);
    this.get('_childViews').forEach(function(childView) {
      if (childView['clearFilter']) {
        childView.clearFilter();
      }
    });
  }

});
