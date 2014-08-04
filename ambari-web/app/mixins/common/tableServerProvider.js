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


App.TableServerProvider = Em.Mixin.create({
  tableName: '',
  updaterBinding: 'App.router.updateController',
  filteringComplete: true,
  filterConditions: [],
  filterWaitingTime: 500,
  timeOut: null,
  /**
   * total number of entities in table
   */
  totalCount: 0,
  /**
   * Request error data
   *
   */
  requestError: null,

  filteredContent: function () {
    return this.get('content');
  }.property('content'),

  pageContent: function () {
    return this.get('filteredContent');
  }.property('filteredContent'),

  /**
   * request latest data filtered by new parameters
   * called when trigger property(<code>refreshTriggers</code>) is changed
   */
  refresh: function () {
    var self = this;
    this.set('filteringComplete', false);
    var updaterMethodName = this.get('updater.tableUpdaterMap')[this.get('tableName')];
    this.get('updater')[updaterMethodName](function () {
      self.set('filteringComplete', true);
      self.propertyDidChange('pageContent');
    }, function() {
      self.set('requestError', arguments);
    });
    return true;
  },
  /**
   * reset filters value by column to which filter belongs
   * @param columns {Array}
   */
  resetFilterByColumns: function (columns) {
    var filterConditions = this.get('filterConditions');
    columns.forEach(function (iColumn) {
      var filterCondition = filterConditions.findProperty('iColumn', iColumn);

      if (filterCondition) {
        filterCondition.value = '';
        this.saveFilterConditions(filterCondition.iColumn, filterCondition.value, filterCondition.type, filterCondition.skipFilter);
      }
    }, this);
  },
  /**
   * Apply each filter to each row
   * @param iColumn {Number}
   * @param value {String}
   * @param type {String}
   */
  updateFilter: function (iColumn, value, type) {
    var self = this;
    this.saveFilterConditions(iColumn, value, type, false);
    // if initial load finished
    if (this.get('tableFilteringComplete')) {
      if (!this.get('filteringComplete')) {
        clearTimeout(this.get('timeOut'));
        this.set('timeOut', setTimeout(function () {
          self.updateFilter(iColumn, value, type);
        }, this.get('filterWaitingTime')));
      } else {
        clearTimeout(this.get('timeOut'));
        this.refresh();
      }
    }
  }
});
