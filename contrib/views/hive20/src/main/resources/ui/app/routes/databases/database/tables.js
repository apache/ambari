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
import ENV from 'ui/config/environment';
import UILoggerMixin from '../../../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {
  autoRefresh: Ember.inject.service(),

  activate() {
    if(ENV.APP.SHOULD_AUTO_REFRESH_TABLES) {
      let selectedDatabase = this.modelFor('databases.database');
      this.get('autoRefresh').startTablesAutoRefresh(selectedDatabase.get('name'),
        this.tableRefreshStarting.bind(this), this.tableRefreshed.bind(this));
    }
  },

  deactivate() {
    if(ENV.APP.SHOULD_AUTO_REFRESH_TABLES) {
      this.get('autoRefresh').stopTablesAutoRefresh(this.controller.get('database.name'));
    }
  },

  tableRefreshStarting(databaseName) {
    this.controller.set('tableRefreshing', true);
  },

  tableRefreshed(databaseName, deletedTablesCount) {
    this.controller.set('tableRefreshing', false);
    let currentTablesForDatabase = this.store.peekAll('table').filterBy('database.name', databaseName);
    let paramsForTable = this.paramsFor('databases.database.tables.table');
    let currentTableNamesForDatabase = currentTablesForDatabase.mapBy('name');
    if (currentTableNamesForDatabase.length <= 0  || !currentTableNamesForDatabase.contains(paramsForTable.name)) {
      if(deletedTablesCount !== 0) {
        this.get('logger').info(`Current selected table '${paramsForTable.name}' has been deleted from Hive Server. Transitioning out.`);
        this.transitionTo('databases.database', databaseName);
        return;
      }
    }
    if(currentTablesForDatabase.get('length') > 0) {
      this.selectTable(currentTablesForDatabase);
      this.controller.set('model', currentTablesForDatabase);
    }
  },

  model() {
    let selectedDatabase = this.modelFor('databases.database');
    return this.store.query('table', {databaseId: selectedDatabase.get('name')});
  },

  afterModel(model) {
    if (model.get('length') > 0) {
      this.selectTable(model);
    }
  },
  selectTable(model) {
    let sortedModel = model.sortBy('name');
    let alreadySelected = sortedModel.findBy('selected', true);
    if (Ember.isEmpty(alreadySelected)) {
      let paramsForTable = this.paramsFor('databases.database.tables.table');
      let toSelect = null;
      if (!Ember.isEmpty(paramsForTable.name)) {
        toSelect = sortedModel.findBy('name', paramsForTable.name);
      } else {
        toSelect = sortedModel.get('firstObject');
      }

      toSelect.set('selected', true);
    }
  },

  setupController(controller, model) {
    this._super(...arguments);
    let selectedDatabase = this.modelFor('databases.database');
    controller.set('database', selectedDatabase);
  },

  actions: {
    tableSelected(table) {
      let tables = this.controllerFor('databases.database.tables').get('model');
      tables.forEach((table) => {
        table.set('selected', false);
      });
      table.set('selected', true);
      this.transitionTo('databases.database.tables.table', table.get('name'));
    },

    refreshTable() {
      let databaseName = this.controller.get('database.name');
      this.get('autoRefresh').refreshTables(databaseName, this.tableRefreshStarting.bind(this), this.tableRefreshed.bind(this), true);
    }
  }
});
