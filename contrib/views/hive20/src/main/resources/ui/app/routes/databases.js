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
import UILoggerMixin from '../mixins/ui-logger';
import ENV from 'ui/config/environment';

export default Ember.Route.extend(UILoggerMixin, {
  tableOperations: Ember.inject.service(),
  autoRefresh: Ember.inject.service(),

  activate() {
    if(ENV.APP.SHOULD_AUTO_REFRESH_DATABASES) {
      this.get('autoRefresh').startDatabasesAutoRefresh(() => {
        console.log("Databases AutoRefresh started");
      }, this._databasesRefreshed.bind(this));
    }

  },

  deactivate() {
    if(ENV.APP.SHOULD_AUTO_REFRESH_DATABASES) {
      this.get('autoRefresh').stopDatabasesAutoRefresh();
    }
  },

  _databasesRefreshed() {
    let model = this.store.peekAll('database');
    if(this.controller) {
      console.log(model.get('length'));
      this.setupController(this.controller, model);
    }
  },

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
    let alreadySelected = model.findBy('selected', true);
    if (Ember.isEmpty(alreadySelected)) {
      // Check if params present
      let paramsForDatabase = this.paramsFor('databases.database');
      let toSelect = null;
      if (!Ember.isEmpty(paramsForDatabase.databaseId)) {
        toSelect = model.findBy('name', paramsForDatabase.databaseId);
      } else {
        // check if default database is present
        toSelect = model.findBy('name', 'default');
      }

      if (Ember.isEmpty(toSelect)) {
        let sortedModel = model.sortBy('name');
        toSelect = sortedModel.get('firstObject');
      }
      toSelect.set('selected', true);
    }
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
              this.controller.set('deleteDatabaseMessage', "Successfully deleted database");
              this.get('logger').success(`Successfully deleted database '${databaseModel.get('name')}'`);
              Ember.run.later(() => {
                this.store.unloadRecord(databaseModel);
                this.controller.set('showDeleteDatabaseModal', false);
                this.controller.set('deleteDatabaseMessage');
                this.replaceWith('databases');
                this.refresh();
              }, 2 * 1000);
            }, (error) => {
              this.get('logger').danger(`Failed to delete database '${databaseModel.get('name')}'`, this.extractError(error));
              Ember.run.later(() => {
                this.controller.set('showDeleteDatabaseModal', false);
                this.controller.set('deleteDatabaseMessage');
                this.replaceWith('databases');
                this.refresh();
              }, 1 * 1000);
            });
        }, (error) => {
          this.get('logger').danger(`Failed to delete database '${databaseModel.get('name')}'`, this.extractError(error));
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
