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
  /**
   * contains association between property of table and parameter in query
   */
  paramAssociations: {},
  /**
   * properties which trigger <code>refresh()</code> when they are changed
   */
  refreshTriggers: [],
  refreshCompleted: true,

  /**
   * add observers to trigger properties
   */
  initTriggers: function () {
    this.get('refreshTriggers').forEach(function (trigger) {
      this.addObserver(trigger, this, 'refresh');
    }, this);
  },

  /**
   * set filter properties of table to query parameters
   * @param newParams
   */
  setParams: function (newParams) {
    this.get('updater.queryParams').set(this.get('tableName'), newParams);
  },

  /**
   * request latest data filtered by new parameters
   * called when trigger property(<code>refreshTriggers</code>) is changed
   */
  refresh: function () {
    var params = [];
    var paramAssociations = this.get('paramAssociations');
    var self = this;

    for (var property in paramAssociations) {
      if (!Em.isNone(this.get(property))) {
        params.push({
          key: paramAssociations[property],
          value: this.get(property)
        });
      }
    }
    this.setParams(params);
    this.set('refreshCompleted', false);
    this.get('updater')[this.get('updater.tableUpdaterMap')[this.get('tableName')]](function () {
      self.set('refreshCompleted', true);
      self.propertyDidChange('pageContent');
    });
  }
});
