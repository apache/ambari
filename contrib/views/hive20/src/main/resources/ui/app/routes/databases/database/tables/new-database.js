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
import UILoggerMixin from '../../../../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {

  tableOperations: Ember.inject.service(),

  actions: {
    cancel() {
      this.transitionTo('databases');
    },

    create(newDatabaseName) {
      this._createDatabase(newDatabaseName);
    }
  },

  _createDatabase(newDatabaseName) {
    this._modalStatus(true, 'Submitting request to create database');
    this.get('tableOperations').createDatabase(newDatabaseName).then((job) => {
      this._modalStatus(true, 'Waiting for the database to be created');
      return this.get('tableOperations').waitForJobToComplete(job.get('id'), 5 * 1000);
    }).then((status) => {
      this._modalStatus(true, 'Successfully created database');
      this._transitionToDatabases(newDatabaseName);
      this.get('logger').success(`Successfully created database '${newDatabaseName}'`);
    }).catch((err) => {
      this._modalStatus(false);
      this.get('logger').danger(`Failed to create database '${newDatabaseName}'`, this.extractError(err));
    });
  },

  _modalStatus(status, message) {
    this.controller.set('showModal', status);
    if(status) {
      this.controller.set('modalMessage', message);
    }
  },

  _transitionToDatabases(databaseName) {
    Ember.run.later(() => {
      this._modalStatus(false);
      this.transitionTo('databases');
    }, 2000);
  }

});
