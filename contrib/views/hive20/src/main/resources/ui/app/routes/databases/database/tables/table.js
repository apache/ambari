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
import tabs from '../../../../configs/table-level-tabs';
import UILoggerMixin from '../../../../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {
  tableOperations: Ember.inject.service(),
  model(params) {
    let database = this.modelFor('databases.database').get('name');
    let table = params.name;
    return this.store.queryRecord('tableInfo', {databaseId: database, tableName: table});
  },

  setupController: function (controller, model) {
    this._super(controller, model);
    let newTabs = Ember.copy(tabs);
    if (Ember.isEmpty(model.get('partitionInfo'))) {
      newTabs = newTabs.rejectBy('name', 'partitions');
    }

    console.log(model.get('detailedInfo.tableType').toLowerCase());
    if (model.get('detailedInfo.tableType').toLowerCase().indexOf('view') === -1) {
      newTabs = newTabs.rejectBy('name', 'viewInfo');
    } else {
      newTabs = newTabs.rejectBy('name', 'statistics');
    }
    controller.set('tabs', newTabs);
  },

  actions: {
    deleteTable() {
      this.deleteTable(this.currentModel);
    },

    deleteTableWarning(){
      this.deleteTableWarning();
    },

    cancelDeleteTableWarning(){
      this.cancelDeleteTableWarning();
    },

    editTable(table) {
      console.log("Edit table");
    },

    refreshTableInfo() {
      this.refresh();
    }
  },

  deleteTableWarning(){
    this.controller.set('showDeleteTableWarningModal', true);
  },

  cancelDeleteTableWarning(){
    this.controller.set('showDeleteTableWarningModal', false);
  },

  deleteTable(tableInfo) {
    this.controller.set('showDeleteTableWarningModal', false);
    this.controller.set('showDeleteTableModal', true);
    this.controller.set('deleteTableMessage', 'Submitting request to delete table');
    let databaseModel = this.controllerFor('databases.database').get('model');
    this.get('tableOperations').deleteTable(databaseModel.get('name'), tableInfo.get('table'))
      .then((job) => {
        this.controller.set('deleteTableMessage', 'Waiting for the table to be deleted');
        this.get('tableOperations').waitForJobToComplete(job.get('id'), 5 * 1000)
          .then((status) => {
            this.controller.set('deleteTableMessage', "Successfully Deleted table");
            this.get('logger').success(`Successfully deleted table '${tableInfo.get('table')}'`);
            Ember.run.later(() => {
              this.controller.set('showDeleteTableModal', false);
              this.controller.set('deleteTableMessage');
              this._removeTableLocally(databaseModel.get('name'), tableInfo.get('table'));
              this._resetModelInTablesController(databaseModel.get('name'), tableInfo.get('table'));
              this.transitionTo('databases.database', databaseModel.get('name'));
            }, 2 * 1000);
          }, (error) => {
            this.get('logger').danger(`Failed to delete table '${tableInfo.get('table')}'`, this.extractError(error));
            Ember.run.later(() => {
              this.controller.set('showDeleteTableModal', false);
              this.controller.set('deleteTableMessage');
              this.transitionTo('databases.database', databaseModel.get('name'));
            }, 2 * 1000);
          });
      }, (error) => {
        this.get('logger').danger(`Failed to delete table '${tableInfo.get('table')}'`, this.extractError(error));
        this.controller.set('showDeleteTableModal', true);
      });

  },

  _removeTableLocally(database, table) {
    let tableToBeRemoved = this.store.peekRecord('table', `${database}/${table}`);
    this.store.deleteRecord(tableToBeRemoved);
  },

  _resetModelInTablesController(database, tables) {
    let tablesController = this.controllerFor('databases.database.tables');
    let currentTables = this.store.peekRecord('database', database).get('tables');
    tablesController.set('model', currentTables);
  }
});
