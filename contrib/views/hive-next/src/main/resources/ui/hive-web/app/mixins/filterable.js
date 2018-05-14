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

import Ember from 'ember';
import utils from 'hive/utils/functions';

export default Ember.Mixin.create({
  init: function () {
    this._super();
    this.clearFilters();
  },

  filter: function (items) {
    var self = this;

    if (items && this.get('filters.length')) {
      items = items.filter(function (item) {
        return self.get('filters').every(function (filter) {
          var propValue = item.get(filter.property);

          if (!!filter.value) {
            if (filter.min !== undefined && filter.max !== undefined) {
              if (utils.isInteger(propValue)) {
                return +propValue >= +filter.min && +propValue <= +filter.max;
              } else if (utils.isDate(propValue)) {
                return propValue >= filter.min && propValue <= filter.max;
              } else {
                return false;
              }
            } else if (filter.exactMatch) {
              return propValue == filter.value;
            } else {
              return propValue && propValue.toLowerCase().indexOf(filter.value.toLowerCase()) > -1;
            }
          }

          return false;
        });
      });
    }

    return items;
  },

  updateFilters: function (property, filterValue, exactMatch) {
    var addFilter = function () {
      if (!filterValue) {
        return;
      }

      this.get('filters').pushObject(Ember.Object.create({
        property: property,
        exactMatch: exactMatch,
        min: filterValue.min,
        max: filterValue.max,
        value: filterValue
      }));
    };

    var existentFilter = this.get('filters').find(function (filter) {
      return filter.property === property;
    });

    if (existentFilter) {
      if (filterValue) {
        //remove and add again for triggering collection change thus avoiding to add observers on individual properties of a filter
        this.get('filters').removeObject(existentFilter);
        addFilter.apply(this);
      } else {
        //ensures removal of the filterValue when it's an empty string
        this.set('filters', this.get('filters').without(existentFilter));
      }
    } else {
       addFilter.apply(this);
    }
  },

  clearFilters: function () {
    var filters = this.get('filters');

    if (!filters || filters.get('length')) {
      this.set('filters', Ember.A());
    }
  },

  actions: {
    filter: function (property, filterValue) {
      this.updateFilters(property, filterValue);
    }
  }
});
