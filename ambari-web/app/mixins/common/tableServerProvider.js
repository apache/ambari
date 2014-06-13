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
  /**
   * total number of entities in table
   */
  totalCount: 0,

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

    if (!this.get('filteringComplete')) return false;

    this.set('filteringComplete', false);
    this.get('updater')[this.get('updater.tableUpdaterMap')[this.get('tableName')]](function () {
      self.set('filteringComplete', true);
      self.propertyDidChange('pageContent');
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
    this.saveFilterConditions(iColumn, value, type, false);
    this.refresh();
  },

  /**
   * save filter conditions to local storage
   * @param iColumn {Number}
   * @param value {String|Array}
   * @param type {String}
   * @param skipFilter {Boolean}
   */
  saveFilterConditions: function (iColumn, value, type, skipFilter) {
    var filterCondition = this.get('filterConditions').findProperty('iColumn', iColumn);

    if (filterCondition) {
      filterCondition.value = value;
      filterCondition.skipFilter = skipFilter;
    } else {
      filterCondition = {
        skipFilter: skipFilter,
        iColumn: iColumn,
        value: value,
        type: type
      };
      this.get('filterConditions').push(filterCondition);
    }
    App.db.setFilterConditions(this.get('controller.name'), this.get('filterConditions'));
  }
});
