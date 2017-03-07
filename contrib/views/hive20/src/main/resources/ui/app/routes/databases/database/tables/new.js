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
import tabs from '../../../../configs/create-table-tabs';
import UILoggerMixin from '../../../../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {
  tableOperations: Ember.inject.service(),

  setupController(controller, model) {
    this._super(controller, model);
    controller.set('tabs', Ember.copy(tabs));
  },

  // function is used in sub-classes
  /**
   * @param settings
   * @param shouldTransition : should transition to other route?
   * @returns {Promise.<TResult>|*}
   */
  createTable: function(settings, shouldTransition){
    this.controller.set('showCreateTableModal', true);
    this.controller.set('createTableMessage', 'Submitting request to create table');
    let databaseModel = this.controllerFor('databases.database').get('model');
    return this.get('tableOperations').submitCreateTable(databaseModel.get('name'), settings)
      .then((job) => {
        console.log('Created job: ', job.get('id'));
        this.controller.set('createTableMessage', 'Waiting for the table to be created');
        return this.get('tableOperations').waitForJobToComplete(job.get('id'), 5 * 1000)
          .then((status) => {
            this.controller.set('createTableMessage', "Successfully created table");
            this.get('logger').success(`Successfully created table '${settings.name}'`);
            Ember.run.later(() => {
            this.controller.set('showCreateTableModal', false);
            this.controller.set('createTableMessage');
            this._addTableToStoreLocally(databaseModel, settings.name);
            this._resetModelInTablesController(databaseModel.get('tables'));
              if(shouldTransition){
                this._transitionToCreatedTable(databaseModel.get('name'), settings.name);
              }
            }, 2 * 1000);
            return Ember.RSVP.Promise.resolve(job);
          }, (error) => {
            this.get('logger').danger(`Failed to create table '${settings.name}'`, this.extractError(error));
            Ember.run.later(() => {
              this.controller.set('showCreateTableModal', false);
              this.controller.set('createTableMessage');
              if(shouldTransition) {
                this.transitionTo('databases.database', databaseModel.get('name'));
              }
            }, 2 * 1000);

            return Ember.RSVP.Promise.reject(error);
          });
      }, (error) => {
        this.get('logger').danger(`Failed to create table '${settings.name}'`, this.extractError(error));
        this.controller.set('showCreateTableModal', true);
        throw error;
      });
  },
  actions: {
    cancel() {
      let databaseController = this.controllerFor('databases.database');
      this.transitionTo('databases.database', databaseController.get('model'));
    },
    toggleCSVFormat: function() {
      console.log("inside new route toggleCSVFormat");
      this.toggleProperty('showCSVFormatInput');
    },

    create(settings) {
      // keep this a function call call only as the createTable function is used in sub-classes
      this.createTable(settings, true);
    }
  },

  _transitionToCreatedTable(database, table) {
    this.transitionTo('databases.database.tables.table', database, table);
  },

  _addTableToStoreLocally(database, table) {
    // Add only if it has not been added by the auto refresh
    let existingRecord = this.store.peekRecord('table', `${database.get('name')}/${table}`);
    if(Ember.isEmpty(existingRecord)) {
      this.store.createRecord('table', {
        id: `${database.get('name')}/${table}`,
        name: `${table}`,
        type: 'TABLE',
        selected: true,
        database: database
      });
    }
  },

  _resetModelInTablesController(tables) {
    let tablesController = this.controllerFor('databases.database.tables');
    tablesController.get('model').setEach('selected', false);
    tablesController.set('model', tables);
  }
});
