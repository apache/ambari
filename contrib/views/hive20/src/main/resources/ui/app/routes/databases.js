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

export default Ember.Route.extend({
  tableOperations: Ember.inject.service(),

  model() {
    return this.store.findAll('database', {reload: true});
  },

  afterModel(model) {
    if (model.get('length') > 0) {
      this.selectDatabase(model);
      if (this.controller) {
        this.setupController(this.controller, model);
      }
    }
  },

  setupController(controller, model) {
    let sortedModel = model.sortBy('name');
    let selectedModel = sortedModel.filterBy('selected', true).get('firstObject');
    sortedModel.removeObject(selectedModel);
    let finalList = [];
    finalList.pushObject(selectedModel);
    finalList.pushObjects(sortedModel);
    controller.set('model', finalList);
  },

  selectDatabase(model) {
    // check if default database is present
    let toSelect = model.findBy('name', 'default');
    if (Ember.isEmpty(toSelect)) {
      let sortedModel = model.sortBy('name');
      toSelect = sortedModel.get('firstObject');
    }
    toSelect.set('selected', true);
  },

  actions: {
    databaseSelected(database) {
      this.transitionTo('databases.database.tables', database.get('id'));
    },

    dropDatabase() {
      let databases = this.get('controller.model');
      let selectedModel = databases.filterBy('selected', true).get('firstObject');
      if (Ember.isEmpty(selectedModel)) {
        return;
      }

      this.get('controller').set('databaseToDelete', selectedModel);

      if (selectedModel.get('tables.length') > 0) {
        this.get('controller').set('databaseNotEmpty', true);
        console.log('database not empty');
        return;
      }
      this.get('controller').set('confirmDropDatabase', true);
    },

    createTable() {
      console.log("Table created");
    },

    notEmptyDialogClosed() {
      this.get('controller').set('databaseNotEmpty', false);
      this.get('controller').set('databaseToDelete', undefined);
    },

    databaseDropConfirmed() {
      console.log('drop confirmed');
      this.get('controller').set('confirmDropDatabase', false);

      this.controller.set('showDeleteDatabaseModal', true);
      this.controller.set('deleteDatabaseMessage', 'Submitting request to delete database');
      let databaseModel = this.controller.get('databaseToDelete');
      this.get('tableOperations').deleteDatabase(databaseModel)
        .then((job) => {
          this.controller.set('deleteDatabaseMessage', 'Waiting for the database to be deleted');
          this.get('tableOperations').waitForJobToComplete(job.get('id'), 5 * 1000)
            .then((status) => {
              this.controller.set('deleteDatabaseMessage', "Successfully Deleted table");
              Ember.run.later(() => {
                this.store.unloadRecord(databaseModel);
                this.controller.set('showDeleteDatabaseModal', false);
                this.controller.set('deleteDatabaseMessage');
                this.replaceWith('databases');
                this.refresh();
              }, 2 * 1000);
            }, (error) => {
              // TODO: handle error
              Ember.run.later(() => {
                this.controller.set('showDeleteDatabaseModal', false);
                this.controller.set('deleteDatabaseMessage');
                this.replaceWith('databases');
                this.refresh();
              }, 2 * 1000);
            });
        }, (error) => {
          console.log("Error encountered", error);
          this.controller.set('showDeleteDatabaseModal', false);
        });
    },

    databaseDropDeclined() {
      console.log('drop declined');
      this.get('controller').set('confirmDropDatabase', false);
      this.get('controller').set('databaseToDelete', undefined);
    }
  }
});
