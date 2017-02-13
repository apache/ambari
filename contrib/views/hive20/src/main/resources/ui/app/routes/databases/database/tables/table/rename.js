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

import TableMetaRouter from './table-meta-router';

export default TableMetaRouter.extend({

  tableOperations: Ember.inject.service(),

  activate() {
    let tableController = this.controllerFor('databases.database.tables.table');
    this.set('existingTabs', tableController.get('tabs'));
    tableController.set('tabs', []);
  },

  deactivate() {
    let tableController = this.controllerFor('databases.database.tables.table');
    tableController.set('tabs', this.get('existingTabs'));
  },

  actions: {
    cancel() {
      this.transitionTo('databases.database.tables');
    },

    rename(newTableName) {
      let tableName = this.controller.get('table.table');
      let databaseName = this.controller.get('table.database');
      this._renameTo(newTableName, tableName, databaseName);
    }
  },

  _renameTo(newTableName, oldTableName, databaseName) {
    this._modalStatus(true, 'Submitting request to rename table');
    this.get('tableOperations').renameTable(databaseName, newTableName, oldTableName).then((job) => {
      this._modalStatus(true, 'Waiting for the table to be renamed');
      return this.get('tableOperations').waitForJobToComplete(job.get('id'), 5 * 1000);
    }).then((status) => {
      this._modalStatus(true, 'Successfully renamed table');
      this._transitionToTables();
    }).catch((err) => {
      this._modalStatus(true, 'Failed to rename table');
      this._alertMessage('Failed to rename table', err);
      this._transitionToTables();
    });
  },

  _modalStatus(status, message) {
    this.controller.set('showModal', status);
    if(status) {
      this.controller.set('modalMessage', message);
    }
  },

  _transitionToTables() {
    Ember.run.later(() => {
      this._modalStatus(false);
      this.transitionTo('databases');
    }, 2000);
  },

  _alertMessage(message, err) {
    console.log(message, err);
    // TODO: user alert message here
  }


});
